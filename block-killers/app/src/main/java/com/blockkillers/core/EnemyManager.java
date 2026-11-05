package com.blockkillers.core;

/** Owns the enemy pool: spawning, movement integration, separation, contact damage and cleanup. */
public final class EnemyManager {
    public static final int CAPACITY = 600;
    private final Game g;
    public final EntityList<Enemy> list = new EntityList<>(CAPACITY, Enemy::new);
    private final int[] near = new int[64];

    public EnemyManager(Game g) { this.g = g; }

    public void clear() { list.clear(); }

    public int aliveCount() { return list.size; }

    public Enemy spawn(EnemyType t, float x, float y) {
        Enemy e = list.obtain();
        if (e == null) return null;
        float time = g.time;
        e.init(t, clampW(x), clampW(y), Config.hpMult(time), Config.dmgMult(time), Config.speedMult(time));
        e.xp = Math.max(1, Math.round(t.xp * Config.xpMult(time)));
        return e;
    }

    private static float clampW(float v) { return MathUtil.clamp(v, -Config.WORLD_HALF + 40, Config.WORLD_HALF - 40); }

    /** A point just outside the visible screen around the player. */
    public void offscreenPoint(float[] out) {
        float hw = g.viewW / 2f, hh = g.viewH / 2f;
        float r = MathUtil.len(hw, hh) + 60f;
        for (int tries = 0; tries < 6; tries++) {
            float a = MathUtil.rand(0, MathUtil.TAU);
            float x = g.player.x + MathUtil.cos(a) * r, y = g.player.y + MathUtil.sin(a) * r;
            out[0] = clampW(x); out[1] = clampW(y);
            // accept if still off-screen after clamping to the world border
            if (Math.abs(out[0] - g.player.x) > hw + 20 || Math.abs(out[1] - g.player.y) > hh + 20) return;
        }
    }

    public Enemy nearest(float x, float y, float maxRange) {
        Enemy best = null;
        float bd = maxRange * maxRange;
        for (int i = 0; i < list.size; i++) {
            Enemy e = list.get(i);
            if (!e.alive) continue;
            float d = MathUtil.dist2(x, y, e.x, e.y) - e.radius * e.radius;
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    public void update(float dt) {
        Player p = g.player;
        SpatialGrid grid = g.grid;
        for (int i = 0; i < list.size; i++) {
            Enemy e = list.get(i);
            if (!e.alive) continue;
            e.t += dt;
            e.spawnAnim = Math.min(1f, e.spawnAnim + dt * 4f);
            if (e.flash > 0) e.flash -= dt;
            if (e.attackTimer > 0) e.attackTimer -= dt;
            if (e.bladeHitTimer > 0) e.bladeHitTimer -= dt;
            e.rot += e.rotSpeed * dt;

            if (e.boss) g.bosses.updateBoss(e, dt); else g.ai.update(e, dt);

            // separation (cheap, neighbours only)
            if (!e.boss) {
                int n = grid.query(list, e.x, e.y, e.radius, near);
                for (int k = 0; k < n; k++) {
                    Enemy o = list.get(near[k]);
                    if (o == e || o.boss) continue;
                    float dx = e.x - o.x, dy = e.y - o.y;
                    float d2 = dx * dx + dy * dy, rr = (e.radius + o.radius) * 0.9f;
                    if (d2 < rr * rr && d2 > 0.01f) {
                        float d = (float) Math.sqrt(d2);
                        float push = (rr - d) * 0.5f;
                        e.x += dx / d * push * 0.5f; e.y += dy / d * push * 0.5f;
                    }
                }
            }

            e.x += (e.vx + e.kx) * dt;
            e.y += (e.vy + e.ky) * dt;
            float decay = Math.max(0f, 1f - 8f * dt);
            e.kx *= decay; e.ky *= decay;
            e.x = MathUtil.clamp(e.x, -Config.WORLD_HALF + e.radius, Config.WORLD_HALF - e.radius);
            e.y = MathUtil.clamp(e.y, -Config.WORLD_HALF + e.radius, Config.WORLD_HALF - e.radius);

            // contact damage
            float rr = e.radius + p.radius - 4f;
            if (e.attackTimer <= 0 && MathUtil.dist2(e.x, e.y, p.x, p.y) < rr * rr) {
                g.combat.damagePlayer(e.damage);
                e.attackTimer = e.boss ? 0.8f : e.type.attackCooldown;
            }

            // despawn stragglers that fell far behind (keeps density near the player)
            if (!e.boss && MathUtil.dist2(e.x, e.y, p.x, p.y) > 2200f * 2200f) e.alive = false;
        }
    }

    /** Removes dead enemies. Called once per frame after all systems ran (indices stay stable during the frame). */
    public void cleanup() {
        for (int i = list.size - 1; i >= 0; i--) if (!list.get(i).alive) list.removeAt(i);
    }
}
