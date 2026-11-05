package com.blockkillers.core;

/** Owns the enemy pool: spawning, movement integration, separation, contact damage and cleanup. */
public final class EnemyManager {
    public static final int CAPACITY = 600;
    private final Game g;
    public final EntityList<Enemy> list = new EntityList<>(CAPACITY, Enemy::new);
    private final int[] near = new int[64];
    private final float[] vel = new float[2], pos = new float[2];
    private static final int[] FUSE_SHAPES = {Shapes.SQUARE, Shapes.TRIANGLE, Shapes.PENTAGON, Shapes.HEXAGON, Shapes.DIAMOND};

    public EnemyManager(Game g) { this.g = g; }

    public void clear() { list.clear(); }

    public int aliveCount() { return list.size; }

    public Enemy spawn(EnemyType t, float x, float y) {
        Enemy e = list.obtain();
        if (e == null) return null;
        float time = g.time;
        e.init(t, g.map.clampX(x, 40), g.map.clampY(y, 40), Config.hpMult(time), Config.dmgMult(time), Config.speedMult(time));
        e.xp = Math.max(1, Math.round(t.xp * Config.xpMult(time)));
        e.radius *= Config.enemyGrowth(time);
        if (g.map != null && g.map.count > 0) { pos[0] = e.x; pos[1] = e.y; g.map.resolve(pos, e.radius); e.x = pos[0]; e.y = pos[1]; }
        return e;
    }



    /** Turns a fresh enemy into a fused (tier 1) or mega-fused (tier 2) mash-up of shapes. */
    public void fuse(Enemy e, int tier) {
        int a = FUSE_SHAPES[MathUtil.randInt(FUSE_SHAPES.length)];
        if (a == e.type.shape) a = FUSE_SHAPES[(MathUtil.randInt(FUSE_SHAPES.length - 1) + 1) % FUSE_SHAPES.length];
        int b = FUSE_SHAPES[MathUtil.randInt(FUSE_SHAPES.length)];
        EnemyType other = Config.ENEMIES[MathUtil.randInt(9)];
        int col = MathUtil.mix(e.type.color, other.color, tier == 2 ? 0.5f : 0.35f);
        e.fuse(tier, a, b, col);
    }

    /** A point just outside the visible screen around the player. */
    public void offscreenPoint(float[] out) {
        float z = g.camera.zoom;
        float hw = g.viewW / 2f / z, hh = g.viewH / 2f / z;
        WorldMap m = g.map;
        if (m.corridor()) {
            // enemies arrive from the left and right ends of the lane
            float side = MathUtil.rand() < 0.5f ? -1 : 1;
            float x = g.player.x + side * (hw + 60);
            if (x < -m.hw + 40 || x > m.hw - 40) x = g.player.x - side * (hw + 60);
            out[0] = m.clampX(x, 40); out[1] = MathUtil.rand(-m.hh * 0.5f, m.hh * 0.5f);
            return;
        }
        float r = MathUtil.len(hw, hh) + 60f;
        for (int tries = 0; tries < 6; tries++) {
            float a = MathUtil.rand(0, MathUtil.TAU);
            float x = g.player.x + MathUtil.cos(a) * r, y = g.player.y + MathUtil.sin(a) * r;
            out[0] = m.clampX(x, 40); out[1] = m.clampY(y, 40);
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

            // elemental status effects
            if (e.burnT > 0) {
                e.burnT -= dt; e.hp -= e.burnDps * dt;
                if (MathUtil.rand() < 0.25f) g.fx.spawn(e.x + MathUtil.rand(-e.radius, e.radius) * 0.6f, e.y, 0, -60, 0.4f, 5f, 0xFFFF6D00, ParticleSystem.DOT);
                if (e.burnT <= 0) e.burnDps = 0;
            }
            if (e.poisonT > 0) {
                e.poisonT -= dt; e.hp -= e.poisonDps * dt * (e.armored ? 1.75f : 0.9f);
                if (MathUtil.rand() < 0.15f) g.fx.spawn(e.x, e.y, MathUtil.rand(-30, 30), MathUtil.rand(-30, 30), 0.5f, 4f, 0xFF76FF03, ParticleSystem.DOT);
                if (e.poisonT <= 0) e.poisonDps = 0;
            }
            if (e.hp <= 0) { g.combat.kill(e); continue; }
            if (e.slagT > 0) e.slagT -= dt;
            if (e.shieldFlash > 0) e.shieldFlash -= dt;
            float slow = 1f;
            if (e.slowT > 0) { e.slowT -= dt; slow = 1f - e.slowAmt; }

            // corridor: everything moves strictly left/right
            if (g.map.corridor()) { e.vy = 0; e.ky = 0; }
            // steer around obstacles (bosses float over them)
            if (!e.boss && !g.map.corridor()) {
                vel[0] = e.vx; vel[1] = e.vy;
                g.map.steer(e.x, e.y, e.radius, vel);
                e.vx = vel[0]; e.vy = vel[1];
            }

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

            e.x += (e.vx * slow + e.kx) * dt;
            e.y += (e.vy * slow + e.ky) * dt;
            if (!e.boss) { pos[0] = e.x; pos[1] = e.y; if (g.map.resolve(pos, e.radius)) { e.x = pos[0]; e.y = pos[1]; } }
            float decay = Math.max(0f, 1f - 8f * dt);
            e.kx *= decay; e.ky *= decay;
            e.x = g.map.clampX(e.x, e.radius);
            e.y = g.map.clampY(e.y, e.radius);

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
