package com.blockkillers.core;

/** Sound effect ids. All sounds are synthesized at startup by {@link SoundSynth} (no asset files needed). */
public final class Sfx {
    private Sfx() {}
    public static final int SHOOT = 0, HIT = 1, DEATH = 2, XP = 3, LEVEL_UP = 4, SELECT = 5, BOSS = 6, HURT = 7,
            GAME_OVER = 8, EXPLODE = 9, ZAP = 10, ACHIEVEMENT = 11, NOVA = 12, CLICK = 13, PULSE = 14, BOSS_DEATH = 15;
    public static final int COUNT = 16;
    /** Minimum seconds between two plays of the same sound (prevents audio spam with hundreds of enemies). */
    public static final float[] MIN_GAP = {0.07f, 0.05f, 0.045f, 0.06f, 0.3f, 0.1f, 1f, 0.15f, 1f, 0.08f, 0.08f, 0.5f, 0.3f, 0.05f, 0.15f, 1f};
    public static final float[] VOLUME = {0.30f, 0.35f, 0.5f, 0.35f, 0.8f, 0.7f, 0.9f, 0.8f, 0.9f, 0.6f, 0.45f, 0.8f, 0.8f, 0.6f, 0.5f, 1f};
}
