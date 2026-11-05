package com.blockkillers.core.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

/** Listens for host beacons on the local network (UDP broadcast). */
public final class Discovery {
    public static final class Found {
        public String name, ip, mode; public int port, players; public long seen;
    }

    private final ArrayList<Found> found = new ArrayList<>();
    private volatile boolean running;
    private DatagramSocket socket;

    public synchronized ArrayList<Found> list() {
        long now = System.currentTimeMillis();
        found.removeIf(f -> now - f.seen > 4000);
        return new ArrayList<>(found);
    }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(() -> {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.bind(new InetSocketAddress(NetHost.DISCOVERY_PORT));
                byte[] buf = new byte[512];
                while (running) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    String s = new String(p.getData(), 0, p.getLength(), "UTF-8");
                    String[] parts = s.split("\\|");
                    if (parts.length < 5 || !parts[0].equals("BK1")) continue;
                    String ip = p.getAddress().getHostAddress();
                    synchronized (this) {
                        Found f = null;
                        for (Found x : found) if (x.ip.equals(ip) && x.port == Integer.parseInt(parts[2])) f = x;
                        if (f == null) { f = new Found(); found.add(f); }
                        f.name = parts[1]; f.ip = ip; f.port = Integer.parseInt(parts[2]); f.mode = parts[3];
                        f.players = Integer.parseInt(parts[4]); f.seen = System.currentTimeMillis();
                    }
                }
            } catch (Exception ignored) {
            } finally { running = false; }
        }, "bk-discovery");
        t.setDaemon(true);
        t.start();
    }

    public void stop() { running = false; if (socket != null) socket.close(); }
}
