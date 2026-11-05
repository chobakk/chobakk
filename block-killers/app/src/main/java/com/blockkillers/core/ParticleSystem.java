package com.blockkillers.core;

/** Pooled particles, floating text and lightning bolts with quality-based budgets. */
public final class ParticleSystem {
    public static final int DOT = 0, SHARD = 1, RING = 2, SPARK = 3;
    private final Game g;
    public final EntityList<Particle> particles = new EntityList<>(1200, Particle::new);
    public final EntityList<FloatText> texts = new EntityList<>(90, FloatText::new);
    // Lightning bolts: x1,y1,x2,y2,life
    public final float[][] bolts = new float[80][5];
    public final int[] boltColors = new int[80];
    public int boltCount;
    /** Adaptive scaling (0.3..1) reduced automatically on slow devices. */
    public float adaptive = 1f;

    public ParticleSystem(Game g) { this.g = g; }

    public void clear() { particles.clear(); texts.clear(); boltCount = 0; }

    private int budget() { return (int) (g.settings.particleBudget() * adaptive); }

    public Particle spawn(float x, float y, float vx, float vy, float life, float size, int color, int kind) {
        if (particles.size >= budget()) return null;
        Particle p = particles.obtain();
        if (p == null) return null;
        p.x = x; p.y = y; p.vx = vx; p.vy = vy; p.life = p.maxLife = life; p.size = size; p.color = color; p.kind = kind;
        p.rot = MathUtil.rand(0, MathUtil.TAU); p.rotSpeed = MathUtil.rand(-8, 8); p.drag = 3f;
        return p;
    }

    public void burst(float x, float y, int color, int count, float speed, float size) {
        int q = g.settings.quality;
        int n = q == 0 ? count / 3 : (q == 1 ? count * 2 / 3 : count);
        for (int i = 0; i < n; i++) {
            float a = MathUtil.rand(0, MathUtil.TAU), v = speed * MathUtil.rand(0.3f, 1f);
            spawn(x, y, MathUtil.cos(a) * v, MathUtil.sin(a) * v, MathUtil.rand(0.3f, 0.7f), size * MathUtil.rand(0.6f, 1.2f), color, i % 3 == 0 ? SHARD : DOT);
        }
    }

    public void ring(float x, float y, float radius, int color, float life) {
        Particle p = spawn(x, y, 0, 0, life, radius, color, RING);
        if (p != null) p.drag = 0;
    }

    public void text(float x, float y, String s, int color, float size) {
        if (texts.full()) return;
        FloatText t = texts.obtain();
        t.x = x + MathUtil.rand(-8, 8); t.y = y; t.vy = -70; t.life = t.maxLife = 0.7f; t.text = s; t.color = color; t.size = size;
    }

    public void bolt(float x1, float y1, float x2, float y2, int color) {
        if (boltCount >= bolts.length) return;
        float[] b = bolts[boltCount++];
        b[0] = x1; b[1] = y1; b[2] = x2; b[3] = y2; b[4] = 0.18f; boltColors[boltCount - 1] = color;
    }

    public void update(float dt) {
        for (int i = particles.size - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) { particles.removeAt(i); continue; }
            p.x += p.vx * dt; p.y += p.vy * dt;
            float d = Math.max(0f, 1f - p.drag * dt);
            p.vx *= d; p.vy *= d;
            p.rot += p.rotSpeed * dt;
        }
        for (int i = texts.size - 1; i >= 0; i--) {
            FloatText t = texts.get(i);
            t.life -= dt;
            if (t.life <= 0) { texts.removeAt(i); continue; }
            t.y += t.vy * dt; t.vy *= Math.max(0f, 1f - 3f * dt);
        }
        for (int i = boltCount - 1; i >= 0; i--) {
            bolts[i][4] -= dt;
            if (bolts[i][4] <= 0) {
                float[] tmp = bolts[i]; bolts[i] = bolts[boltCount - 1]; bolts[boltCount - 1] = tmp;
                int tc = boltColors[i]; boltColors[i] = boltColors[boltCount - 1]; boltColors[boltCount - 1] = tc; boltCount--;
            }
        }
    }

    private final float[] pts = new float[8];

    public void render(Gfx gfx, float l, float t, float r, float b) {
        for (int i = 0; i < particles.size; i++) {
            Particle p = particles.get(i);
            if (p.kind != RING && (p.x < l || p.x > r || p.y < t || p.y > b)) continue;
            float k = p.life / p.maxLife;
            switch (p.kind) {
                case RING: {
                    float rad = p.size * (1f - k * k * 0.85f);
                    gfx.ring(p.x, p.y, rad, 3f + 6f * k, MathUtil.alpha(p.color, k));
                    break;
                }
                case SHARD: {
                    float s = p.size * (0.4f + 0.6f * k);
                    float c = MathUtil.cos(p.rot), sn = MathUtil.sin(p.rot);
                    pts[0] = p.x + c * s; pts[1] = p.y + sn * s;
                    pts[2] = p.x - c * s * 0.5f - sn * s * 0.6f; pts[3] = p.y - sn * s * 0.5f + c * s * 0.6f;
                    pts[4] = p.x - c * s * 0.5f + sn * s * 0.6f; pts[5] = p.y - sn * s * 0.5f - c * s * 0.6f;
                    gfx.poly(pts, 3, MathUtil.alpha(p.color, Math.min(1f, k * 1.5f)));
                    break;
                }
                case SPARK:
                    gfx.line(p.x, p.y, p.x - p.vx * 0.04f, p.y - p.vy * 0.04f, p.size, MathUtil.alpha(p.color, k));
                    break;
                default:
                    gfx.circle(p.x, p.y, p.size * (0.3f + 0.7f * k), MathUtil.alpha(p.color, Math.min(1f, k * 1.5f)));
            }
        }
        for (int i = 0; i < boltCount; i++) {
            float[] bb = bolts[i];
            float k = bb[4] / 0.18f;
            int col = MathUtil.alpha(boltColors[i], k);
            // jagged line in 4 segments
            float px = bb[0], py = bb[1];
            float dx = bb[2] - bb[0], dy = bb[3] - bb[1];
            float len = MathUtil.len(dx, dy) + 0.001f;
            float nx = -dy / len, ny = dx / len;
            for (int s = 1; s <= 4; s++) {
                float f = s / 4f;
                float off = s == 4 ? 0 : MathUtil.rand(-14, 14);
                float qx = bb[0] + dx * f + nx * off, qy = bb[1] + dy * f + ny * off;
                gfx.line(px, py, qx, qy, 4f * k + 1f, col);
                px = qx; py = qy;
            }
        }
        for (int i = 0; i < texts.size; i++) {
            FloatText ft = texts.get(i);
            float k = ft.life / ft.maxLife;
            float sc = k > 0.8f ? 1f + (k - 0.8f) * 2f : 1f;
            gfx.text(ft.text, ft.x + 1.5f, ft.y + 1.5f, ft.size * sc, MathUtil.alpha(0xFF000000, k), Gfx.ALIGN_CENTER, true);
            gfx.text(ft.text, ft.x, ft.y, ft.size * sc, MathUtil.alpha(ft.color, k), Gfx.ALIGN_CENTER, true);
        }
    }
}
