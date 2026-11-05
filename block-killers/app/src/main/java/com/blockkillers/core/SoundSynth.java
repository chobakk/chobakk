package com.blockkillers.core;

import java.util.Random;

/** Procedural audio: generates every sound effect and the background music loop as 16-bit PCM. */
public final class SoundSynth {
    private SoundSynth() {}
    public static final int RATE = 22050;
    private static final Random R = new Random(7);

    private static float[] buf(float seconds) { return new float[(int) (seconds * RATE)]; }
    private static float sq(float ph) { return (ph % 1f) < 0.5f ? 1f : -1f; }
    private static float saw(float ph) { return 2f * (ph % 1f) - 1f; }
    private static float tri(float ph) { float f = ph % 1f; return f < 0.5f ? 4f * f - 1f : 3f - 4f * f; }
    private static float sine(float ph) { return (float) Math.sin(ph * Math.PI * 2); }
    private static float noise() { return R.nextFloat() * 2f - 1f; }

    /** Tone with exponential frequency sweep and exponential decay envelope. wave: 0 sine 1 square 2 saw 3 tri. */
    private static void tone(float[] b, int start, float dur, float f0, float f1, float vol, int wave, float attack) {
        int n = (int) (dur * RATE);
        float ph = 0;
        for (int i = 0; i < n && start + i < b.length; i++) {
            float t = (float) i / n;
            float f = f0 * (float) Math.pow(f1 / f0, t);
            ph += f / RATE;
            float s;
            switch (wave) { case 1: s = sq(ph); break; case 2: s = saw(ph); break; case 3: s = tri(ph); break; default: s = sine(ph); }
            float env = (t < attack ? t / attack : 1f) * (float) Math.exp(-4.0 * t);
            b[start + i] += s * vol * env;
        }
    }
    private static void noiseBurst(float[] b, int start, float dur, float vol, float lowpass) {
        int n = (int) (dur * RATE);
        float lp = 0;
        for (int i = 0; i < n && start + i < b.length; i++) {
            float t = (float) i / n;
            lp += (noise() - lp) * lowpass;
            b[start + i] += lp * vol * (float) Math.exp(-5.0 * t);
        }
    }

    public static short[] generate(int id) {
        float[] b;
        switch (id) {
            case Sfx.SHOOT: b = buf(0.07f); tone(b, 0, 0.07f, 1100, 520, 0.5f, 1, 0.02f); break;
            case Sfx.HIT: b = buf(0.05f); noiseBurst(b, 0, 0.05f, 0.8f, 0.6f); tone(b, 0, 0.05f, 300, 180, 0.4f, 1, 0.01f); break;
            case Sfx.DEATH: b = buf(0.2f); noiseBurst(b, 0, 0.2f, 0.7f, 0.35f); tone(b, 0, 0.15f, 520, 90, 0.5f, 1, 0.01f); break;
            case Sfx.XP: b = buf(0.07f); tone(b, 0, 0.07f, 1300, 2100, 0.45f, 0, 0.05f); break;
            case Sfx.LEVEL_UP: {
                b = buf(0.6f); float[] notes = {523.25f, 659.25f, 783.99f, 1046.5f};
                for (int i = 0; i < 4; i++) tone(b, (int) (i * 0.09f * RATE), 0.3f, notes[i], notes[i], 0.35f, 3, 0.02f);
                break;
            }
            case Sfx.SELECT: b = buf(0.18f); tone(b, 0, 0.08f, 880, 880, 0.4f, 1, 0.02f); tone(b, (int) (0.07f * RATE), 0.1f, 1320, 1320, 0.4f, 1, 0.02f); break;
            case Sfx.BOSS: {
                b = buf(1.4f);
                for (int k = 0; k < 3; k++) tone(b, (int) (k * 0.42f * RATE), 0.4f, 110, 220, 0.5f, 2, 0.05f);
                noiseBurst(b, 0, 1.4f, 0.25f, 0.05f);
                break;
            }
            case Sfx.HURT: b = buf(0.16f); tone(b, 0, 0.16f, 220, 80, 0.7f, 1, 0.01f); noiseBurst(b, 0, 0.1f, 0.3f, 0.4f); break;
            case Sfx.GAME_OVER: {
                b = buf(1.3f); float[] notes = {392f, 311.13f, 261.63f, 196f};
                for (int i = 0; i < 4; i++) tone(b, (int) (i * 0.22f * RATE), 0.5f, notes[i], notes[i] * 0.98f, 0.45f, 3, 0.02f);
                break;
            }
            case Sfx.EXPLODE: b = buf(0.4f); noiseBurst(b, 0, 0.4f, 1f, 0.12f); tone(b, 0, 0.3f, 140, 40, 0.6f, 0, 0.01f); break;
            case Sfx.ZAP: {
                b = buf(0.14f);
                for (int i = 0; i < b.length; i++) { float t = (float) i / b.length; b[i] = (R.nextFloat() < 0.5f ? 1 : -1) * 0.35f * (1 - t) * (i % 9 < 4 ? 1 : -0.4f); }
                tone(b, 0, 0.14f, 1800, 600, 0.3f, 2, 0.01f);
                break;
            }
            case Sfx.ACHIEVEMENT: {
                b = buf(0.7f); float[] notes = {783.99f, 987.77f, 1174.66f, 1567.98f};
                for (int i = 0; i < 4; i++) tone(b, (int) (i * 0.07f * RATE), 0.45f, notes[i], notes[i], 0.28f, 0, 0.02f);
                break;
            }
            case Sfx.NOVA: b = buf(0.45f); noiseBurst(b, 0, 0.45f, 0.6f, 0.08f); tone(b, 0, 0.45f, 180, 900, 0.4f, 2, 0.05f); break;
            case Sfx.CLICK: b = buf(0.04f); tone(b, 0, 0.04f, 1500, 1000, 0.4f, 1, 0.05f); break;
            case Sfx.PULSE: b = buf(0.3f); tone(b, 0, 0.3f, 300, 120, 0.6f, 0, 0.02f); noiseBurst(b, 0, 0.2f, 0.25f, 0.1f); break;
            case Sfx.BOSS_DEATH: {
                b = buf(1.5f); noiseBurst(b, 0, 1.5f, 1f, 0.06f);
                float[] notes = {261.63f, 329.63f, 392f, 523.25f, 659.25f};
                for (int i = 0; i < 5; i++) tone(b, (int) ((0.3f + i * 0.1f) * RATE), 0.5f, notes[i], notes[i], 0.3f, 3, 0.02f);
                break;
            }
            default: b = buf(0.01f);
        }
        return toPcm(b, 0.9f);
    }

    /** 8-bar synthwave loop in A minor (Am - F - C - G), 112 BPM. */
    public static short[] music() {
        float bpm = 112f, beat = 60f / bpm;
        int bars = 8;
        float[] b = buf(bars * 4 * beat);
        float[][] chords = {{220f, 261.63f, 329.63f}, {174.61f, 220f, 261.63f}, {130.81f, 164.81f, 196f}, {196f, 246.94f, 293.66f}};
        float[] bassRoot = {110f, 87.31f, 65.41f, 98f};
        for (int bar = 0; bar < bars; bar++) {
            int c = bar % 4;
            float barStart = bar * 4 * beat;
            // bass: 8th notes
            for (int k = 0; k < 8; k++) {
                int st = (int) ((barStart + k * beat / 2) * RATE);
                float f = bassRoot[c] * (k % 4 == 3 ? 2f : 1f);
                tone(b, st, beat / 2 * 0.95f, f, f, 0.22f, 2, 0.02f);
            }
            // arpeggio: 16th notes
            for (int k = 0; k < 16; k++) {
                int st = (int) ((barStart + k * beat / 4) * RATE);
                float f = chords[c][k % 3] * (k % 6 < 3 ? 2f : 4f);
                tone(b, st, beat / 4 * 0.9f, f, f, 0.06f, 1, 0.05f);
            }
            // drums: kick on beats, hat on offbeats, snare on 2 & 4
            for (int k = 0; k < 4; k++) {
                int st = (int) ((barStart + k * beat) * RATE);
                tone(b, st, 0.18f, 150, 45, 0.5f, 0, 0.005f);
                noiseBurst(b, (int) ((barStart + k * beat + beat / 2) * RATE), 0.04f, 0.10f, 0.9f);
                if (k % 2 == 1) noiseBurst(b, st, 0.15f, 0.22f, 0.5f);
            }
            // pad
            for (int k = 0; k < 3; k++) {
                float f = chords[c][k];
                int st = (int) (barStart * RATE), n = (int) (4 * beat * RATE);
                float ph = 0;
                for (int i = 0; i < n && st + i < b.length; i++) {
                    ph += f / RATE;
                    float t = (float) i / n;
                    float env = Math.min(1f, t * 8f) * Math.min(1f, (1 - t) * 8f);
                    b[st + i] += tri(ph) * 0.035f * env;
                }
            }
        }
        return toPcm(b, 0.75f);
    }

    private static short[] toPcm(float[] b, float target) {
        float peak = 0.0001f;
        for (float v : b) peak = Math.max(peak, Math.abs(v));
        float g = target / peak;
        short[] out = new short[b.length];
        for (int i = 0; i < b.length; i++) out[i] = (short) (MathUtil.clamp(b[i] * g, -1f, 1f) * 32767);
        return out;
    }

    /** Wrap PCM in a WAV container (mono, 16 bit). */
    public static byte[] wav(short[] pcm) {
        int dataLen = pcm.length * 2;
        byte[] out = new byte[44 + dataLen];
        writeStr(out, 0, "RIFF"); writeInt(out, 4, 36 + dataLen); writeStr(out, 8, "WAVE");
        writeStr(out, 12, "fmt "); writeInt(out, 16, 16); writeShort(out, 20, 1); writeShort(out, 22, 1);
        writeInt(out, 24, RATE); writeInt(out, 28, RATE * 2); writeShort(out, 32, 2); writeShort(out, 34, 16);
        writeStr(out, 36, "data"); writeInt(out, 40, dataLen);
        for (int i = 0; i < pcm.length; i++) { out[44 + i * 2] = (byte) (pcm[i] & 0xFF); out[45 + i * 2] = (byte) ((pcm[i] >> 8) & 0xFF); }
        return out;
    }
    private static void writeStr(byte[] b, int o, String s) { for (int i = 0; i < s.length(); i++) b[o + i] = (byte) s.charAt(i); }
    private static void writeInt(byte[] b, int o, int v) { for (int i = 0; i < 4; i++) b[o + i] = (byte) ((v >> (8 * i)) & 0xFF); }
    private static void writeShort(byte[] b, int o, int v) { b[o] = (byte) (v & 0xFF); b[o + 1] = (byte) ((v >> 8) & 0xFF); }
}
