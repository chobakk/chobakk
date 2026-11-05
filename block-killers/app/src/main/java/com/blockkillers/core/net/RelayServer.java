package com.blockkillers.core.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Tiny relay for online play (phones on different networks can't reach each other directly).
 * Run anywhere with Java 11+:  java -cp relay com.blockkillers.core.net.RelayServer 47800
 *
 * Protocol (frames = int length + bytes):
 *   host  -> "H"                 server -> "R" + 4-letter room code
 *   client-> "J" + code          server -> "A" (accepted) or "E" (no such room / full)
 *   then  client frame  P        -> host gets   'D' id P
 *         host frame    id P     -> client id gets P   (id 255 = every client)
 *         client joins/leaves    -> host gets   'C' id / 'X' id
 */
public final class RelayServer {
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final Map<String, Room> rooms = new HashMap<>();
    private final Random rnd = new Random();
    private ServerSocket server;
    private volatile boolean running = true;

    private static final class Peer {
        final Socket s; final DataOutputStream out; final DataInputStream in;
        Peer(Socket s) throws Exception {
            this.s = s; s.setTcpNoDelay(true);
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        }
        synchronized void send(byte[] b) { try { Frames.write(out, b); } catch (Exception e) { close(); } }
        void close() { try { s.close(); } catch (Exception ignored) {} }
    }

    private static final class Room {
        final String code; final Peer host; final Peer[] clients = new Peer[8];
        Room(String code, Peer host) { this.code = code; this.host = host; }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 47800;
        RelayServer r = new RelayServer();
        r.start(port);
        System.out.println("Zombie Killer relay listening on port " + r.port());
        Thread.currentThread().join();
    }

    public void start(int port) throws Exception {
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(port));
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    Socket s = server.accept();
                    Thread h = new Thread(() -> handle(s), "relay-peer");
                    h.setDaemon(true); h.start();
                } catch (Exception e) { if (!running) return; }
            }
        }, "relay-accept");
        t.setDaemon(true);
        t.start();
    }

    public int port() { return server.getLocalPort(); }
    public void stop() { running = false; try { server.close(); } catch (Exception ignored) {} }

    private void handle(Socket s) {
        Peer p;
        try { p = new Peer(s); } catch (Exception e) { return; }
        try {
            byte[] first = Frames.read(p.in);
            if (first.length >= 1 && first[0] == 'H') runHost(p);
            else if (first.length >= 5 && first[0] == 'J') runClient(p, new String(first, 1, 4, "US-ASCII"));
        } catch (Exception ignored) {
        } finally { p.close(); }
    }

    private void runHost(Peer p) throws Exception {
        Room room;
        synchronized (rooms) {
            String code;
            do {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 4; i++) sb.append(LETTERS.charAt(rnd.nextInt(LETTERS.length())));
                code = sb.toString();
            } while (rooms.containsKey(code));
            room = new Room(code, p);
            rooms.put(code, room);
        }
        byte[] reply = ("R" + room.code).getBytes("US-ASCII");
        p.send(reply);
        try {
            while (true) {
                byte[] f = Frames.read(p.in);
                if (f.length < 1) continue;
                int target = f[0] & 0xFF;
                byte[] payload = new byte[f.length - 1];
                System.arraycopy(f, 1, payload, 0, payload.length);
                synchronized (room) {
                    for (int i = 0; i < room.clients.length; i++) {
                        Peer c = room.clients[i];
                        if (c != null && (target == 255 || target == i)) c.send(payload);
                    }
                }
            }
        } finally {
            synchronized (rooms) { rooms.remove(room.code); }
            synchronized (room) { for (Peer c : room.clients) if (c != null) c.close(); }
        }
    }

    private void runClient(Peer p, String code) throws Exception {
        Room room;
        synchronized (rooms) { room = rooms.get(code.toUpperCase()); }
        int id = -1;
        if (room != null) synchronized (room) {
            for (int i = 0; i < room.clients.length; i++) if (room.clients[i] == null) { room.clients[i] = p; id = i; break; }
        }
        if (id < 0) { p.send(new byte[]{'E'}); return; }
        p.send(new byte[]{'A'});
        room.host.send(new byte[]{'C', (byte) id});
        try {
            while (true) {
                byte[] f = Frames.read(p.in);
                byte[] wrapped = new byte[f.length + 2];
                wrapped[0] = 'D'; wrapped[1] = (byte) id;
                System.arraycopy(f, 0, wrapped, 2, f.length);
                room.host.send(wrapped);
            }
        } finally {
            synchronized (room) { room.clients[id] = null; }
            room.host.send(new byte[]{'X', (byte) id});
        }
    }
}
