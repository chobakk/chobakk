package com.blockkillers.core;

import java.util.Random;

public final class MathUtil {
    private MathUtil() {}
    public static final Random RNG = new Random();
    public static final float PI = (float) Math.PI, TAU = (float) (Math.PI * 2);

    public static float rand() { return RNG.nextFloat(); }
    public static float rand(float a, float b) { return a + RNG.nextFloat() * (b - a); }
    public static int randInt(int n) { return RNG.nextInt(n); }
    public static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
    public static int clamp(int v, int a, int b) { return v < a ? a : (v > b ? b : v); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float dist2(float x1, float y1, float x2, float y2) { float dx = x2 - x1, dy = y2 - y1; return dx * dx + dy * dy; }
    public static float dist(float x1, float y1, float x2, float y2) { return (float) Math.sqrt(dist2(x1, y1, x2, y2)); }
    public static float len(float x, float y) { return (float) Math.sqrt(x * x + y * y); }
    public static float cos(float a) { return (float) Math.cos(a); }
    public static float sin(float a) { return (float) Math.sin(a); }
    public static float atan2(float y, float x) { return (float) Math.atan2(y, x); }

    /** Replace alpha channel (0..1). */
    public static int alpha(int color, float a) {
        int al = (int) (clamp(a, 0f, 1f) * ((color >>> 24) & 0xFF));
        return (al << 24) | (color & 0x00FFFFFF);
    }
    public static int mix(int c1, int c2, float t) {
        t = clamp(t, 0f, 1f);
        int a = (int) lerp((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t);
        int r = (int) lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = (int) lerp((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t);
        int b = (int) lerp(c1 & 0xFF, c2 & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    public static String time(float seconds) {
        int s = (int) seconds;
        int m = s / 60; s %= 60;
        return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }
}
