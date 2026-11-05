package com.blockkillers.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.util.Log;

import com.blockkillers.core.Audio;
import com.blockkillers.core.Sfx;
import com.blockkillers.core.SoundSynth;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Sound effects through SoundPool (synthesized WAVs cached on first launch) and gapless looping music
 * through a static AudioTrack. Synthesis runs on a background thread so startup is instant.
 */
final class AndroidAudio implements Audio {
    private static final String TAG = "BlockKillers";
    private final SoundPool pool;
    private final int[] ids = new int[Sfx.COUNT];
    private volatile boolean sfxReady;
    private AudioTrack music;
    private volatile boolean wantMusic;
    private volatile float musicVolume = 0.5f;
    private volatile boolean released;

    AndroidAudio(final Context ctx) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build();
        final File dir = new File(ctx.getCacheDir(), "sfx_v2");
        Thread t = new Thread(() -> {
            try {
                if (!dir.exists()) dir.mkdirs();
                File custom = customDir(ctx);
                for (int i = 0; i < Sfx.COUNT; i++) {
                    File user = custom == null ? null : findCustom(custom, i);
                    if (user != null) { ids[i] = pool.load(user.getAbsolutePath(), 1); continue; }
                    File f = new File(dir, "s" + i + ".wav");
                    if (!f.exists() || f.length() < 64) {
                        try (FileOutputStream out = new FileOutputStream(f)) { out.write(SoundSynth.wav(SoundSynth.generate(i))); }
                    }
                    ids[i] = pool.load(f.getAbsolutePath(), 1);
                }
                sfxReady = true;
                buildMusic();
            } catch (Throwable e) {
                Log.w(TAG, "audio init failed", e);
            }
        }, "audio-init");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Player-supplied sounds: Android/data/com.blockkillers/files/sounds/NAME.(mp3|ogg|wav|m4a)
     * override the built-in synthesized effects (no permission needed, files stay on the device).
     */
    private static final String[][] CUSTOM = {
            {"death", String.valueOf(Sfx.DEATH_STING)}, {"death", String.valueOf(Sfx.GAME_OVER)},
            {"kill", String.valueOf(Sfx.DEATH)}, {"hurt", String.valueOf(Sfx.OOF)}, {"hurt", String.valueOf(Sfx.HURT)},
            {"multikill", String.valueOf(Sfx.AIRHORN)}, {"bosskill", String.valueOf(Sfx.BOSS_DEATH)}, {"boom", String.valueOf(Sfx.BOOM)},
            {"bonk", String.valueOf(Sfx.BONK)}, {"levelup", String.valueOf(Sfx.LEVEL_UP)}, {"gameover", String.valueOf(Sfx.SAD_TROMBONE)},
            {"shoot", String.valueOf(Sfx.SHOOT)}, {"boss", String.valueOf(Sfx.BOSS)}, {"loot", String.valueOf(Sfx.LOOT)}};
    private static final String[] EXT = {".mp3", ".ogg", ".wav", ".m4a"};

    private static File customDir(Context ctx) {
        try {
            File d = ctx.getExternalFilesDir("sounds");
            if (d == null) return null;
            if (!d.exists()) d.mkdirs();
            File readme = new File(d, "README.txt");
            if (!readme.exists()) {
                try (FileOutputStream out = new FileOutputStream(readme)) {
                    out.write(("Put your own sound clips here to replace the built-in ones:\n"
                            + "death, kill, hurt, multikill, bosskill, boom, bonk, levelup, gameover, shoot, boss, loot\n"
                            + "Formats: .mp3 .ogg .wav .m4a (short clips, under ~5 seconds). Restart the game to load them.\n").getBytes("UTF-8"));
                }
            }
            return d;
        } catch (Exception e) { return null; }
    }

    private static File findCustom(File dir, int sfx) {
        for (String[] c : CUSTOM) {
            if (Integer.parseInt(c[1]) != sfx) continue;
            for (String ext : EXT) {
                File f = new File(dir, c[0] + ext);
                if (f.isFile() && f.length() > 0) return f;
            }
        }
        return null;
    }

    private synchronized void buildMusic() {
        if (released) return;
        short[] pcm = SoundSynth.music();
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SoundSynth.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.length * 2)
                .build();
        track.write(pcm, 0, pcm.length);
        track.setLoopPoints(0, pcm.length, -1);
        track.setVolume(musicVolume);
        music = track;
        if (wantMusic) track.play();
    }

    @Override public void play(int sfx, float volume) {
        if (!sfxReady || sfx < 0 || sfx >= Sfx.COUNT) return;
        pool.play(ids[sfx], volume, volume, 1, 0, 1f);
    }

    @Override public synchronized void startMusic() {
        wantMusic = true;
        if (music != null && music.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
            try { music.play(); } catch (IllegalStateException ignored) {}
        }
    }

    @Override public synchronized void stopMusic() {
        wantMusic = false;
        if (music != null && music.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            try { music.pause(); } catch (IllegalStateException ignored) {}
        }
    }

    @Override public synchronized void setMusicVolume(float v) {
        musicVolume = v;
        if (music != null) music.setVolume(v);
    }

    @Override public synchronized void release() {
        released = true;
        pool.release();
        if (music != null) { try { music.stop(); } catch (IllegalStateException ignored) {} music.release(); music = null; }
    }
}
