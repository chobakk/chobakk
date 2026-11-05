package com.blockkillers.core.net;

import java.net.InetSocketAddress;
import java.net.Socket;

/** Client side: connects to a host over Wi-Fi or joins an online room through the relay. */
public final class NetClient extends NetSession {
    private volatile StreamLink link;
    private volatile boolean viaRelay, accepted;

    @Override public boolean isClient() { return true; }

    public void connectLan(final String ip, final int port) { connect(ip, port, null); }
    public void connectRelay(final String host, final int port, final String code) { connect(host, port, code); }

    private void connect(final String host, final int port, final String code) {
        status = "connecting";
        viaRelay = code != null;
        Thread t = new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, port), 6000);
                link = new StreamLink(0, s, new HostListener(this));
                if (viaRelay) {
                    byte[] j = ("J" + code.toUpperCase()).getBytes("US-ASCII");
                    link.send(j);
                } else { ready = true; status = "joined"; }
            } catch (Exception e) { failed = true; status = "error: " + e.getMessage(); }
        }, "bk-client-connect");
        t.setDaemon(true);
        t.start();
    }

    /** Frames from the host (or the relay handshake first). */
    private static final class HostListener implements StreamLink.Listener {
        private final NetClient c;
        HostListener(NetClient c) { this.c = c; }
        @Override public void onFrame(StreamLink l, byte[] f) {
            if (c.viaRelay && !c.accepted) {
                if (f.length > 0 && f[0] == 'A') { c.accepted = true; c.ready = true; c.status = "joined"; }
                else { c.failed = true; c.status = "room not found"; l.close(); }
                return;
            }
            c.push(EV_FRAME, 0, f);
        }
        @Override public void onClosed(StreamLink l) { c.push(EV_LEAVE, 0, null); }
    }

    public void send(byte[] msg) { StreamLink l = link; if (l != null && (!viaRelay || accepted)) l.send(msg); }
    public boolean connected() { StreamLink l = link; return l != null && l.open() && ready; }

    @Override public void close() { StreamLink l = link; if (l != null) l.close(); }
}
