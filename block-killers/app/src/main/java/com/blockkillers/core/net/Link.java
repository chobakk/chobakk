package com.blockkillers.core.net;

/** A connection to one peer (direct socket, or a virtual channel through the relay). */
public interface Link {
    int id();
    void send(byte[] msg);
    boolean open();
    void close();
}
