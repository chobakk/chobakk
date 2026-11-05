package com.blockkillers.core;

import java.util.Random;

/**
 * The arena: four themed regions, obstacle pillars and ruin walls (collide with everything except bosses/railgun),
 * loot chests, and fog-of-war exploration for the full-screen map.
 */
public final class WorldMap {
    public static final int NEON = 0, FROST = 1, TOXIC = 2, CRIMSON = 3;
    /** Map modes selectable before a run. */
    public static final int OPEN = 0, ARENA = 1, CORRIDOR = 2;
    public static final int MODES = 3;
    public static final String[] MODE_KEYS = {"mp_open", "mp_arena", "mp_corridor"};
    public static final String[] MODE_DESC = {"mpd_open", "mpd_arena", "mpd_corridor"};
    public int mode = OPEN;
    /** Playable half extents (x: [-hw, hw], y: [-hh, hh]). */
    public float hw = Config.WORLD_HALF, hh = Config.WORLD_HALF;
    public boolean corridor() { return mode == CORRIDOR; }

    /** Region rectangle q as {l, t, r, b}: quadrants on square maps, four segments along the corridor. */
    public void regionRect(int q, float[] out) {
        if (mode == CORRIDOR) {
            float seg = 2 * hw / 4f;
            int order = q == NEON ? 0 : (q == TOXIC ? 1 : (q == FROST ? 2 : 3));
            out[0] = -hw + order * seg; out[1] = -hh; out[2] = out[0] + seg; out[3] = hh;
            return;
        }
        out[0] = (q == NEON || q == TOXIC) ? -hw : 0; out[2] = out[0] + hw;
        out[1] = (q == TOXIC || q == FROST) ? -hh : 0; out[3] = out[1] + hh;
    }
    public int regionAt(float x, float y) {
        if (mode == CORRIDOR) {
            int seg = MathUtil.clamp((int) ((x + hw) / (2 * hw / 4f)), 0, 3);
            return seg == 0 ? NEON : (seg == 1 ? TOXIC : (seg == 2 ? FROST : CRIMSON));
        }
        return region(x, y);
    }
    public float clampX(float x, float r) { return MathUtil.clamp(x, -hw + r, hw - r); }
    public float clampY(float y, float r) { return hh - r <= 0 ? 0 : MathUtil.clamp(y, -hh + r, hh - r); }
    public boolean outside(float x, float y, float margin) { return x < -hw - margin || x > hw + margin || y < -hh - margin || y > hh + margin; }

    public void generate(long seed, int mode) {
        this.mode = mode;
        if (mode == ARENA) { hw = hh = 1000f; }
        else if (mode == CORRIDOR) { hw = Config.WORLD_HALF; hh = 75f; }
        else { hw = hh = Config.WORLD_HALF; }
        if (mode == OPEN) { generate(seed); return; }
        Random r = new Random(seed);
        count = 0; chestCount = 0;
        if (mode == ARENA) {
            // a ring of pillars for cover around an open centre
            for (int i = 0; i < 10; i++) {
                float a = i * MathUtil.TAU / 10 + 0.3f;
                add(MathUtil.cos(a) * 560, MathUtil.sin(a) * 560, 34 + r.nextFloat() * 14, i % 2 == 0 ? 6 : 4, a);
            }
            for (int i = 0; i < 4; i++) {
                float a = i * MathUtil.TAU / 4 + 0.78f;
                add(MathUtil.cos(a) * 260, MathUtil.sin(a) * 260, 26, 8, a);
            }
            for (int i = 0; i < 4; i++) {
                float a = i * MathUtil.TAU / 4;
                cx[chestCount] = MathUtil.cos(a) * 820; cy[chestCount] = MathUtil.sin(a) * 820; opened[chestCount] = false; golden[chestCount] = i == 0; chestCount++;
            }
        } else {
            // corridor: no obstacles, chests along the lane
            for (float x = -hw + 600; x < hw - 300 && chestCount < maxChests; x += 900) {
                if (Math.abs(x) < 500) continue;
                cx[chestCount] = x; cy[chestCount] = 0; opened[chestCount] = false; golden[chestCount] = r.nextFloat() < 0.15f; chestCount++;
            }
        }
        buildGrid();
        for (int i = 0; i < explored.length; i++) explored[i] = false;
    }

    public static final String[] REGION_KEYS = {"rg_neon", "rg_frost", "rg_toxic", "rg_crimson"};
    public static final int[] FLOOR = {0xFF0A0A1F, 0xFF07121F, 0xFF0A1710, 0xFF1A0A0E};
    public static final int[] GRID = {0x1EB388FF, 0x2240C4FF, 0x1E76FF03, 0x22FF5252};
    public static final int[] OBST = {0xFF5E35B1, 0xFF0277BD, 0xFF2E7D32, 0xFFC62828};
    public static final int[] MAP_COLOR = {0xFF2A1F55, 0xFF0D3050, 0xFF143A1C, 0xFF4A1418};

    private static final float H = Config.WORLD_HALF;
    private static final float OCELL = 400f;
    private static final int OCOLS = (int) (2 * H / OCELL);
    public static final float FOG_CELL = 160f;
    public static final int FOG_COLS = (int) (2 * H / FOG_CELL);

    public final int maxObstacles = 420;
    public final float[] ox = new float[maxObstacles], oy = new float[maxObstacles], or = new float[maxObstacles], orot = new float[maxObstacles];
    public final int[] oshape = new int[maxObstacles];
    public int count;
    private final int[] cellStart = new int[OCOLS * OCOLS + 1];
    private int[] cellItems = new int[0];

    public final int maxChests = 48;
    public final float[] cx = new float[maxChests], cy = new float[maxChests];
    public final boolean[] opened = new boolean[maxChests], golden = new boolean[maxChests];
    public int chestCount;

    public final boolean[] explored = new boolean[FOG_COLS * FOG_COLS];
    public long seed;

    public static int region(float x, float y) {
        if (x < 0) return y < 0 ? TOXIC : NEON;
        return y < 0 ? FROST : CRIMSON;
    }

    private void generate(long seed) {
        this.seed = seed;
        Random r = new Random(seed);
        count = 0;
        int[] shapes = {4, 5, 6, 8};
        // ruin walls: short arcs of touching pillars with gaps (cover)
        for (int k = 0; k < 34 && count < maxObstacles - 10; k++) {
            float bx = (r.nextFloat() * 2 - 1) * (H - 300), by = (r.nextFloat() * 2 - 1) * (H - 300);
            if (bx * bx + by * by < 750 * 750) continue;
            float a = r.nextFloat() * MathUtil.TAU, curve = (r.nextFloat() - 0.5f) * 0.5f;
            int n = 4 + r.nextInt(5);
            int shape = shapes[r.nextInt(shapes.length)];
            for (int i = 0; i < n; i++) {
                if (r.nextFloat() < 0.15f) { a += curve; continue; }   // gap
                float x = bx + MathUtil.cos(a) * i * 52, y = by + MathUtil.sin(a) * i * 52;
                if (!freeFor(x, y, 26, 4)) continue;
                add(x, y, 24 + r.nextFloat() * 6, shape, a);
                a += curve;
            }
        }
        // free-standing pillars
        for (int tries = 0; tries < 4000 && count < maxObstacles; tries++) {
            float x = (r.nextFloat() * 2 - 1) * (H - 150), y = (r.nextFloat() * 2 - 1) * (H - 150);
            if (x * x + y * y < 520 * 520) continue;
            float rad = 30 + r.nextFloat() * r.nextFloat() * 75;
            if (!freeFor(x, y, rad, 190)) continue;
            add(x, y, rad, shapes[r.nextInt(shapes.length)], r.nextFloat() * MathUtil.TAU);
        }
        buildGrid();
        // chests
        chestCount = 0;
        for (int tries = 0; tries < 3000 && chestCount < maxChests; tries++) {
            float x = (r.nextFloat() * 2 - 1) * (H - 250), y = (r.nextFloat() * 2 - 1) * (H - 250);
            if (x * x + y * y < 650 * 650 || blocked(x, y, 90)) continue;
            boolean near = false;
            for (int i = 0; i < chestCount && !near; i++) near = MathUtil.dist2(x, y, cx[i], cy[i]) < 800 * 800;
            if (near) continue;
            cx[chestCount] = x; cy[chestCount] = y; opened[chestCount] = false; golden[chestCount] = r.nextFloat() < 0.14f;
            chestCount++;
        }
        for (int i = 0; i < explored.length; i++) explored[i] = false;
    }

    private void add(float x, float y, float rad, int shape, float rot) {
        ox[count] = x; oy[count] = y; or[count] = rad; oshape[count] = shape; orot[count] = rot; count++;
    }

    private boolean freeFor(float x, float y, float rad, float gap) {
        for (int i = 0; i < count; i++) {
            float rr = rad + or[i] + gap;
            if (MathUtil.dist2(x, y, ox[i], oy[i]) < rr * rr) return false;
        }
        return true;
    }

    private static int ocell(float v) { return MathUtil.clamp((int) ((v + H) / OCELL), 0, OCOLS - 1); }

    private void buildGrid() {
        int cells = OCOLS * OCOLS;
        int[] counts = new int[cells];
        // an obstacle is registered in every cell its bounding box touches
        int total = 0;
        for (int i = 0; i < count; i++) {
            for (int gy = ocell(oy[i] - or[i]); gy <= ocell(oy[i] + or[i]); gy++)
                for (int gx = ocell(ox[i] - or[i]); gx <= ocell(ox[i] + or[i]); gx++) { counts[gy * OCOLS + gx]++; total++; }
        }
        cellItems = new int[total];
        cellStart[0] = 0;
        for (int c = 0; c < cells; c++) cellStart[c + 1] = cellStart[c] + counts[c];
        int[] fill = new int[cells];
        for (int i = 0; i < count; i++) {
            for (int gy = ocell(oy[i] - or[i]); gy <= ocell(oy[i] + or[i]); gy++)
                for (int gx = ocell(ox[i] - or[i]); gx <= ocell(ox[i] + or[i]); gx++) {
                    int c = gy * OCOLS + gx;
                    cellItems[cellStart[c] + fill[c]++] = i;
                }
        }
    }

    /** True if a circle overlaps any obstacle. */
    public boolean blocked(float x, float y, float r) {
        int c = ocell(y) * OCOLS + ocell(x);
        for (int k = cellStart[c]; k < cellStart[c + 1]; k++) {
            int i = cellItems[k];
            float rr = r + or[i];
            if (MathUtil.dist2(x, y, ox[i], oy[i]) < rr * rr) return true;
        }
        // circles near cell borders: check neighbours too
        if (r > 0) {
            for (int gy = ocell(y - r); gy <= ocell(y + r); gy++) for (int gx = ocell(x - r); gx <= ocell(x + r); gx++) {
                int cc = gy * OCOLS + gx;
                if (cc == c) continue;
                for (int k = cellStart[cc]; k < cellStart[cc + 1]; k++) {
                    int i = cellItems[k];
                    float rr = r + or[i];
                    if (MathUtil.dist2(x, y, ox[i], oy[i]) < rr * rr) return true;
                }
            }
        }
        return false;
    }

    /** Pushes a circle out of obstacles. pos = {x, y}. Returns true if it collided. */
    public boolean resolve(float[] pos, float r) {
        boolean hit = false;
        for (int gy = ocell(pos[1] - r); gy <= ocell(pos[1] + r); gy++) for (int gx = ocell(pos[0] - r); gx <= ocell(pos[0] + r); gx++) {
            int c = gy * OCOLS + gx;
            for (int k = cellStart[c]; k < cellStart[c + 1]; k++) {
                int i = cellItems[k];
                float dx = pos[0] - ox[i], dy = pos[1] - oy[i];
                float rr = r + or[i];
                float d2 = dx * dx + dy * dy;
                if (d2 < rr * rr) {
                    float d = (float) Math.sqrt(d2);
                    if (d < 0.001f) { dx = 1; dy = 0; d = 1; }
                    pos[0] = ox[i] + dx / d * rr; pos[1] = oy[i] + dy / d * rr;
                    hit = true;
                }
            }
        }
        return hit;
    }

    /** Steers a velocity around the nearest obstacle ahead. vel = {vx, vy} (modified in place). */
    public void steer(float x, float y, float r, float[] vel) {
        float vx = vel[0], vy = vel[1];
        float sp = MathUtil.len(vx, vy);
        if (sp < 1f) return;
        float ax = x + vx / sp * (r + 40), ay = y + vy / sp * (r + 40);
        int c = ocell(ay) * OCOLS + ocell(ax);
        int best = -1; float bestGap = 60f;
        for (int k = cellStart[c]; k < cellStart[c + 1]; k++) {
            int i = cellItems[k];
            float dx = ox[i] - x, dy = oy[i] - y;
            if (dx * vx + dy * vy <= 0) continue; // behind
            float gap = MathUtil.len(dx, dy) - or[i] - r;
            if (gap < bestGap) { bestGap = gap; best = i; }
        }
        if (best < 0) return;
        float dx = ox[best] - x, dy = oy[best] - y, d = MathUtil.len(dx, dy) + 0.001f;
        float tx = -dy / d, ty = dx / d;
        if (tx * vx + ty * vy < 0) { tx = -tx; ty = -ty; }
        float w = MathUtil.clamp(1f - bestGap / 60f, 0f, 1f);
        vel[0] = vx * (1 - w) + tx * sp * w;
        vel[1] = vy * (1 - w) + ty * sp * w;
    }

    /** Index of the obstacle hit by the last {@link #raycast} (-1 = outer wall). */
    public int rayHit;
    /** Cosine of the angle between the ray and the surface normal at the last hit (for shading). */
    public float rayShade;

    /**
     * Casts a ray (unit direction dx,dy) against obstacle circles and the outer walls.
     * Walks the obstacle grid cell by cell (DDA) so long rays stay cheap. Returns the hit distance.
     */
    public float raycast(float x, float y, float dx, float dy, float maxD) {
        // outer walls
        float best = maxD;
        rayHit = -1;
        if (dx > 1e-5f) best = Math.min(best, (hw - x) / dx); else if (dx < -1e-5f) best = Math.min(best, (-hw - x) / dx);
        if (dy > 1e-5f) best = Math.min(best, (hh - y) / dy); else if (dy < -1e-5f) best = Math.min(best, (-hh - y) / dy);
        float wallBest = best;
        rayShade = Math.abs((hw - Math.abs(x + dx * best)) < 1f ? dx : dy);
        // DDA over the obstacle grid
        int cx = ocell(x), cy = ocell(y);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1;
        float nextX = (-H + (cx + (dx > 0 ? 1 : 0)) * OCELL), nextY = (-H + (cy + (dy > 0 ? 1 : 0)) * OCELL);
        float tMaxX = Math.abs(dx) < 1e-6f ? Float.MAX_VALUE : (nextX - x) / dx;
        float tMaxY = Math.abs(dy) < 1e-6f ? Float.MAX_VALUE : (nextY - y) / dy;
        float tDX = Math.abs(dx) < 1e-6f ? Float.MAX_VALUE : OCELL / Math.abs(dx), tDY = Math.abs(dy) < 1e-6f ? Float.MAX_VALUE : OCELL / Math.abs(dy);
        float tCell = 0;
        while (tCell < best && cx >= 0 && cy >= 0 && cx < OCOLS && cy < OCOLS) {
            int c = cy * OCOLS + cx;
            for (int k = cellStart[c]; k < cellStart[c + 1]; k++) {
                int i = cellItems[k];
                float ox2 = ox[i] - x, oy2 = oy[i] - y;
                float tc = ox2 * dx + oy2 * dy;
                if (tc < 0) continue;
                float d2 = ox2 * ox2 + oy2 * oy2 - tc * tc, r2 = or[i] * or[i];
                if (d2 > r2) continue;
                float th = tc - (float) Math.sqrt(r2 - d2);
                if (th > 0 && th < best) {
                    best = th; rayHit = i;
                    float hx = x + dx * th - ox[i], hy = y + dy * th - oy[i];
                    rayShade = Math.abs((hx * dx + hy * dy) / or[i]);
                }
            }
            if (tMaxX < tMaxY) { tCell = tMaxX; tMaxX += tDX; cx += stepX; } else { tCell = tMaxY; tMaxY += tDY; cy += stepY; }
        }
        if (rayHit < 0 && best == wallBest) rayShade = 0.6f;
        return best;
    }

    public void reveal(float x, float y, float radius) {
        int c0 = MathUtil.clamp((int) ((x - radius + H) / FOG_CELL), 0, FOG_COLS - 1), c1 = MathUtil.clamp((int) ((x + radius + H) / FOG_CELL), 0, FOG_COLS - 1);
        int r0 = MathUtil.clamp((int) ((y - radius + H) / FOG_CELL), 0, FOG_COLS - 1), r1 = MathUtil.clamp((int) ((y + radius + H) / FOG_CELL), 0, FOG_COLS - 1);
        for (int ry = r0; ry <= r1; ry++) for (int rx = c0; rx <= c1; rx++) {
            float cxx = -H + (rx + 0.5f) * FOG_CELL, cyy = -H + (ry + 0.5f) * FOG_CELL;
            if (MathUtil.dist2(x, y, cxx, cyy) < radius * radius) explored[ry * FOG_COLS + rx] = true;
        }
    }

    public boolean isExplored(float x, float y) {
        int rx = MathUtil.clamp((int) ((x + H) / FOG_CELL), 0, FOG_COLS - 1), ry = MathUtil.clamp((int) ((y + H) / FOG_CELL), 0, FOG_COLS - 1);
        return explored[ry * FOG_COLS + rx];
    }

    public float exploredFraction() {
        int n = 0;
        for (boolean b : explored) if (b) n++;
        return n / (float) explored.length;
    }
}
