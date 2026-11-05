package com.blockkillers.core;

/** Applies volume settings and rate-limits sound effects on top of the platform {@link Audio} backend. */
public final class AudioManager {
    private final Audio backend;
    private final SettingsManager settings;
    private final float[] lastPlayed = new float[Sfx.COUNT];
    private float clock;
    private boolean musicOn;
    /** Records sounds for network clients (set by Game). */
    public NetEvents recorder;

    public AudioManager(Audio backend, SettingsManager settings) {
        this.backend = backend; this.settings = settings;
        for (int i = 0; i < Sfx.COUNT; i++) lastPlayed[i] = -10f;
    }

    public void tick(float dt) { clock += dt; }

    public void play(int sfx) {
        if (recorder != null) recorder.sound(sfx);
        if (settings.sfxVolume <= 0.001f) return;
        if (clock - lastPlayed[sfx] < Sfx.MIN_GAP[sfx]) return;
        lastPlayed[sfx] = clock;
        backend.play(sfx, Sfx.VOLUME[sfx] * settings.sfxVolume);
    }

    public void musicOn() {
        backend.setMusicVolume(settings.musicVolume * 0.7f);
        if (!musicOn) { musicOn = true; backend.startMusic(); }
    }
    public void musicOff() { if (musicOn) { musicOn = false; backend.stopMusic(); } }
    /** Re-applies volume (after settings change). */
    public void refreshVolume() { backend.setMusicVolume(settings.musicVolume * 0.7f); }
    public boolean isMusicOn() { return musicOn; }
    /** Forget music state (used after the host paused audio). */
    public void markMusicStopped() { musicOn = false; }
}
