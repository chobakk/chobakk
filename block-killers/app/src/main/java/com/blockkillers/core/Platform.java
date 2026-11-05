package com.blockkillers.core;

/** Services the host platform provides to the game. */
public interface Platform {
    Storage storage();
    Audio audio();
    void vibrate(int ms);
    boolean canExit();
    void exitApp();
}
