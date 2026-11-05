package com.blockkillers.core;

/** Host-side recorder of one-shot effects (particles, sounds, announcements) to replay on clients. */
public final class NetEvents {
    public static final int MAX_FX = 160, MAX_SFX = 32;
    public boolean recording;
    // fx: kind (0 burst, 1 ring, 2 text, 3 bolt), floats a..f, color, text
    public final int[] fxKind = new int[MAX_FX], fxColor = new int[MAX_FX];
    public final float[][] fxF = new float[MAX_FX][5];
    public final String[] fxText = new String[MAX_FX];
    public int fxN;
    public final int[] sfx = new int[MAX_SFX];
    public int sfxN;
    public String annTitle, annSub;

    public void fx(int kind, float a, float b, float c, float d, float e, int color, String text) {
        if (!recording || fxN >= MAX_FX) return;
        fxKind[fxN] = kind; fxColor[fxN] = color; fxText[fxN] = text;
        float[] f = fxF[fxN]; f[0] = a; f[1] = b; f[2] = c; f[3] = d; f[4] = e;
        fxN++;
    }
    public void sound(int id) { if (recording && sfxN < MAX_SFX) sfx[sfxN++] = id; }
    public void announce(String t, String s) { if (recording) { annTitle = t; annSub = s; } }
    public void clear() { fxN = 0; sfxN = 0; annTitle = annSub = null; }
}
