package com.blockkillers.core;

/** Services the host platform provides to the game. */
public interface Platform {
    Storage storage();
    Audio audio();
    void vibrate(int ms);
    boolean canExit();
    void exitApp();
    /** Ask the user for a line of text (name, IP, server, room code); callback runs on the game thread. */
    void promptText(String title, String initial, TextCallback cb);
    /** Hold Wi-Fi multicast/broadcast reception while multiplayer is active. */
    void setNetworkActive(boolean active);

    interface TextCallback { void done(String text); }
}
