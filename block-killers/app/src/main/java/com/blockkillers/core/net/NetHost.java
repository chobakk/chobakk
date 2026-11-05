package com.blockkillers.core.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/** Host side: accepts players over Wi-Fi (TCP + UDP discovery beacon) or through the online relay. */
public final class NetHost extends NetSession {
    public static final int LAN_PORT = 47777, DISCOVERY_PORT = 47778, MAX_CLIENTS = 3;
    private final ConcurrentHashMap<Integer, Link> links = new ConcurrentHashMap<>();
    private ServerSocket server;
    private volatile StreamLink relay;
    private volatile boolean running = true;
    public volatile String roomCode;
    public volatile int port;
    /** Beacon text fields (updated by the lobby). */
    public volatile String beaconName = "Host", beaconMode = "coop";
    public volatile int beaconPlayers = 1;
    private int nextId = 1;

    @Override public boolean isClient() { return false; }

    // ------------------------------------------------------------------ Wi-Fi
    public void startLan(int wantPort) {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(wantPort));
            port = server.getLocalPort();
            status = "lan";
            ready = true;
        } catch (Exception e) { failed = true; status = "error: " + e.getMessage(); return; }
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    Socket s = server.accept();
                    if (links.size() >= MAX_CLIENTS) { s.close(); continue; }
                    final int id = nextId++;
                    StreamLink l = new StreamLink(id, s, new ClientListener(this));
                    links.put(id, l);
                    push(EV_JOIN, id, null);
                } catch (Exception e) { if (!running) return; }
            }
        }, "bk-host-accept");
        t.setDaemon(true);
        t.start();
        Thread b = new Thread(this::beaconLoop, "bk-host-beacon");
        b.setDaemon(true);
        b.start();
    }

    private void beaconLoop() {
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setBroadcast(true);
            while (running) {
                byte[] msg = ("BK1|" + beaconName.replace('|', ' ') + "|" + port + "|" + beaconMode + "|" + beaconPlayers).getBytes("UTF-8");
                for (InetAddress a : broadcastAddresses()) {
                    try { ds.send(new DatagramPacket(msg, msg.length, a, DISCOVERY_PORT)); } catch (Exception ignored) {}
                }
                Thread.sleep(1000);
            }
        } catch (Exception ignored) {}
    }

    private static ArrayList<InetAddress> broadcastAddresses() {
        ArrayList<InetAddress> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp()) continue;
                ni.getInterfaceAddresses().forEach(ia -> { if (ia.getBroadcast() != null) out.add(ia.getBroadcast()); });
            }
            out.add(InetAddress.getByName("255.255.255.255"));
            out.add(InetAddress.getLoopbackAddress());
        } catch (Exception ignored) {}
        return out;
    }

    /** This device's Wi-Fi/LAN IPv4 addresses (shown in the lobby for manual joining). */
    public static String localAddresses() {
        StringBuilder sb = new StringBuilder();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) { if (sb.length() > 0) sb.append("  "); sb.append(a.getHostAddress()); }
                }
            }
        } catch (Exception ignored) {}
        return sb.length() == 0 ? "127.0.0.1" : sb.toString();
    }

    // ------------------------------------------------------------------ online relay
    public void startRelay(final String host, final int relayPort) {
        status = "connecting";
        Thread t = new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, relayPort), 6000);
                relay = new StreamLink(0, s, new RelayListener(this));
                relay.send(new byte[]{'H'});
            } catch (Exception e) { failed = true; status = "error: " + e.getMessage(); }
        }, "bk-host-relay");
        t.setDaemon(true);
        t.start();
    }

    private void onRelayFrame(byte[] f) {
        if (f.length == 0) return;
        switch (f[0]) {
            case 'R': roomCode = new String(f, 1, f.length - 1); status = "room"; ready = true; break;
            case 'C': {
                int rid = f[1] & 0xFF, id = 100 + rid;
                if (links.size() >= MAX_CLIENTS) return;
                links.put(id, new RelayLink(this, rid, id));
                push(EV_JOIN, id, null);
                break;
            }
            case 'X': { int id = 100 + (f[1] & 0xFF); if (links.remove(id) != null) push(EV_LEAVE, id, null); break; }
            case 'D': {
                int id = 100 + (f[1] & 0xFF);
                byte[] p = new byte[f.length - 2];
                System.arraycopy(f, 2, p, 0, p.length);
                push(EV_FRAME, id, p);
                break;
            }
        }
    }

    /** Frames from directly connected (Wi-Fi) players. */
    private static final class ClientListener implements StreamLink.Listener {
        private final NetHost h;
        ClientListener(NetHost h) { this.h = h; }
        @Override public void onFrame(StreamLink link, byte[] f) { h.push(EV_FRAME, link.id(), f); }
        @Override public void onClosed(StreamLink link) { if (h.links.remove(link.id()) != null) h.push(EV_LEAVE, link.id(), null); }
    }

    /** Frames from the relay server (multiplexed players). */
    private static final class RelayListener implements StreamLink.Listener {
        private final NetHost h;
        RelayListener(NetHost h) { this.h = h; }
        @Override public void onFrame(StreamLink link, byte[] f) { h.onRelayFrame(f); }
        @Override public void onClosed(StreamLink link) { if (h.running) { h.failed = true; h.status = "relay closed"; } }
    }

    /** A player connected through the relay: messages are prefixed with the relay's client id. */
    private static final class RelayLink implements Link {
        private final NetHost host;
        private final int rid, id;
        private volatile boolean open = true;
        RelayLink(NetHost host, int rid, int id) { this.host = host; this.rid = rid; this.id = id; }
        @Override public int id() { return id; }
        @Override public void send(byte[] msg) {
            StreamLink r = host.relay;
            if (!open || r == null) return;
            byte[] w = new byte[msg.length + 1];
            w[0] = (byte) rid;
            System.arraycopy(msg, 0, w, 1, msg.length);
            r.send(w);
        }
        @Override public boolean open() { return open && host.relay != null && host.relay.open(); }
        @Override public void close() { open = false; }
    }

    // ------------------------------------------------------------------ sending
    public void send(int linkId, byte[] msg) { Link l = links.get(linkId); if (l != null) l.send(msg); }
    public void broadcast(byte[] msg) { for (Link l : links.values()) l.send(msg); }
    public int clientCount() { return links.size(); }
    public void kick(int linkId) { Link l = links.remove(linkId); if (l != null) l.close(); }

    @Override public void close() {
        running = false;
        for (Link l : links.values()) l.close();
        links.clear();
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        if (relay != null) relay.close();
    }
}
