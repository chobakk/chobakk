package com.blockkillers.core.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A TCP socket with a reader thread (frames -> listener) and a writer thread (queue -> socket),
 * so the game thread never blocks on the network (required on Android's main thread).
 */
public final class StreamLink implements Link {
    public interface Listener {
        void onFrame(StreamLink link, byte[] frame);
        void onClosed(StreamLink link);
    }

    private static final byte[] POISON = new byte[0];
    private final int id;
    private final Socket socket;
    private final LinkedBlockingQueue<byte[]> outbox = new LinkedBlockingQueue<>();
    private volatile boolean open = true;
    private final Listener listener;

    public StreamLink(int id, Socket socket, Listener listener) {
        this.id = id; this.socket = socket; this.listener = listener;
        try { socket.setTcpNoDelay(true); } catch (Exception ignored) {}
        Thread r = new Thread(this::readLoop, "bk-net-read-" + id);
        Thread w = new Thread(this::writeLoop, "bk-net-write-" + id);
        r.setDaemon(true); w.setDaemon(true);
        r.start(); w.start();
    }

    private void readLoop() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            while (open) listener.onFrame(this, Frames.read(in));
        } catch (Exception e) {
            // connection dropped
        } finally { close(); }
    }

    private void writeLoop() {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            while (open) {
                byte[] b = outbox.take();
                if (b == POISON) break;
                Frames.write(out, b);
            }
        } catch (Exception e) {
            // connection dropped
        } finally { close(); }
    }

    @Override public int id() { return id; }
    @Override public boolean open() { return open; }

    @Override public void send(byte[] msg) {
        if (!open) return;
        // drop stale snapshots if the peer can't keep up (keeps latency low)
        if (outbox.size() > 60) outbox.clear();
        outbox.offer(msg);
    }

    @Override public void close() {
        if (!open) return;
        open = false;
        outbox.offer(POISON);
        try { socket.close(); } catch (Exception ignored) {}
        listener.onClosed(this);
    }
}
