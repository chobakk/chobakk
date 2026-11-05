package com.blockkillers.core.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Length-prefixed binary frames over a TCP stream. */
public final class Frames {
    private Frames() {}
    public static final int MAX = 1 << 20;

    public static byte[] read(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > MAX) throw new IOException("bad frame length " + n);
        byte[] b = new byte[n];
        in.readFully(b);
        return b;
    }

    public static void write(DataOutputStream out, byte[] b) throws IOException {
        out.writeInt(b.length);
        out.write(b);
        out.flush();
    }
}
