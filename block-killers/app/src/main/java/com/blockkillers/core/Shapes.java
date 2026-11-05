package com.blockkillers.core;

/** Geometry helpers that write polygon points into reusable buffers (no allocation per frame). */
public final class Shapes {
    private Shapes() {}
    public static final int SQUARE = 4, TRIANGLE = 3, PENTAGON = 5, HEXAGON = 6, DIAMOND = 2;
    private static final float[] BUF = new float[32];

    /** Draws a shape. sides: 3..8 regular polygon, DIAMOND = stretched rhombus. */
    public static void draw(Gfx g, int sides, float x, float y, float r, float rot, int color) {
        int n = build(sides, x, y, r, rot);
        g.poly(BUF, n, color);
    }
    public static void stroke(Gfx g, int sides, float x, float y, float r, float rot, float w, int color) {
        int n = build(sides, x, y, r, rot);
        g.strokePoly(BUF, n, w, color);
    }
    private static int build(int sides, float x, float y, float r, float rot) {
        if (sides == DIAMOND) {
            float c = MathUtil.cos(rot), s = MathUtil.sin(rot);
            float[] lx = {0, r * 0.62f, 0, -r * 0.62f};
            float[] ly = {-r * 1.15f, 0, r * 1.15f, 0};
            for (int i = 0; i < 4; i++) {
                BUF[i * 2] = x + lx[i] * c - ly[i] * s;
                BUF[i * 2 + 1] = y + lx[i] * s + ly[i] * c;
            }
            return 4;
        }
        float start = rot - MathUtil.PI / 2f + (sides == 4 ? MathUtil.PI / 4f : 0f);
        float rr = sides == 4 ? r * 1.15f : r * 1.08f;
        for (int i = 0; i < sides; i++) {
            float a = start + i * MathUtil.TAU / sides;
            BUF[i * 2] = x + MathUtil.cos(a) * rr;
            BUF[i * 2 + 1] = y + MathUtil.sin(a) * rr;
        }
        return sides;
    }
}
