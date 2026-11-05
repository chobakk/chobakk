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
        final File dir = new File(ctx.getCacheDir(), "sfx_v1");
        Thread t = new Thread(() -> {
            try {
                if (!dir.exists()) dir.mkdirs();
                for (int i = 0; i < Sfx.COUNT; i++) {
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
