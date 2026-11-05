package com.blockkillers.core;

/** Low level audio backend. Sound ids are defined in {@link Sfx}. */
public interface Audio {
    void play(int sfx, float volume);
    void startMusic();
    void stopMusic();
    void setMusicVolume(float v);
    void release();
}
