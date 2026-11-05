package com.blockkillers.core;

import java.util.Arrays;

/** Uniform grid over the world for fast enemy neighbourhood queries (collisions, separation, area damage). */
public final class SpatialGrid {
    public final float cell;
    public final int cols;
    public final int[] head;
    public final int[] next;
    private final float half;

    public SpatialGrid(float worldHalf, float cell, int maxEntities) {
        this.cell = cell; this.half = worldHalf;
        cols = (int) Math.ceil(worldHalf * 2 / cell) + 1;
        head = new int[cols * cols];
        next = new int[maxEntities];
    }

    public int cx(float x) { return MathUtil.clamp((int) ((x + half) / cell), 0, cols - 1); }

    public void build(EntityList<Enemy> list) {
        Arrays.fill(head, -1);
        for (int i = 0; i < list.size; i++) {
            Enemy e = list.get(i);
            if (!e.alive) { next[i] = -1; continue; }
            int c = cx(e.y) * cols + cx(e.x);
            next[i] = head[c];
            head[c] = i;
        }
    }

    /** Writes indices of alive enemies whose circle may overlap (x,y,r) into out. Returns count. */
    public int query(EntityList<Enemy> list, float x, float y, float r, int[] out) {
        int n = 0;
        int x0 = cx(x - r - 50), x1 = cx(x + r + 50), y0 = cx(y - r - 50), y1 = cx(y + r + 50);
        for (int gy = y0; gy <= y1; gy++) {
            for (int gx = x0; gx <= x1; gx++) {
                for (int i = head[gy * cols + gx]; i != -1; i = next[i]) {
                    Enemy e = list.get(i);
                    if (!e.alive) continue;
                    float rr = r + e.radius;
                    if (MathUtil.dist2(x, y, e.x, e.y) <= rr * rr) {
                        if (n < out.length) out[n++] = i;
                    }
                }
            }
        }
        return n;
    }
}
