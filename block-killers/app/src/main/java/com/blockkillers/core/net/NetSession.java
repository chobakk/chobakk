package com.blockkillers.core.net;

import java.util.concurrent.ConcurrentLinkedQueue;

/** Common base for host and client: an inbox of messages filled by network threads, drained by the game thread. */
public abstract class NetSession {
    public static final int EV_FRAME = 0, EV_JOIN = 1, EV_LEAVE = 2;

    public static final class Msg {
        public final int kind, link;
        public final byte[] data;
        Msg(int kind, int link, byte[] data) { this.kind = kind; this.link = link; this.data = data; }
    }

    protected final ConcurrentLinkedQueue<Msg> inbox = new ConcurrentLinkedQueue<>();
    /** Human readable status for the lobby ("Connecting…", "Room ABCD", errors). */
    public volatile String status = "";
    public volatile boolean failed, ready;
    protected volatile long seed;

    public abstract boolean isClient();
    public long mapSeed() { return seed; }
    public void setSeed(long s) { seed = s; }
    public Msg poll() { return inbox.poll(); }
    public abstract void close();

    protected void push(int kind, int link, byte[] data) { inbox.add(new Msg(kind, link, data)); }
}
