package com.blockkillers.core;

/** Boss schedule, three boss designs with multi-phase attack patterns, and boss rewards. */
public final class BossManager {
    public static final int MONOLITH = 0, HIVE = 1, PRISM = 2;
    public static final String[] NAME_KEYS = {"boss_monolith", "boss_hive", "boss_prism"};
    private static final int[] SHAPES = {Shapes.SQUARE, Shapes.HEXAGON, Shapes.TRIANGLE};
    private static final int[] COLORS = {0xFFFF1744, 0xFF00E676, 0xFFD500F9};
    private static final float[] RADIUS = {78f, 84f, 80f};

    private final Game g;
    public float nextBossTime, warning;
    public Enemy current;
    public int spawned;
    private final float[] pt = new float[2];

    public BossManager(Game g) { this.g = g; }

    public void reset() { nextBossTime = Config.BOSS_FIRST_TIME; warning = 0; current = null; spawned = 0; }

    public boolean bossActive() { return current != null && current.alive; }
    public int shape(Enemy e) { return SHAPES[e.bossKind]; }
    public int color(Enemy e) { return COLORS[e.bossKind]; }
    public String name(Enemy e) { return g.strings.get(NAME_KEYS[e.bossKind]); }

    public void update(float dt) {
        if (bossActive()) return;
        if (warning > 0) {
            warning -= dt;
            if (warning <= 0) spawnBoss();
            return;
        }
        if (g.time >= nextBossTime - Config.BOSS_WARNING) {
            warning = Config.BOSS_WARNING;
            g.audio.play(Sfx.BOSS);
            g.platformVibrate(200);
            g.ui.announce(g.strings.get("boss_incoming"), g.strings.get(NAME_KEYS[spawned % 3]));
        }
    }

    /** Spawns the next boss in the cycle immediately (also used by tests). */
    public Enemy spawnBoss() {
        int kind = spawned % 3;
        int tier = spawned;
        g.enemies.offscreenPoint(pt);
        Enemy e = g.enemies.spawn(Config.ENEMIES[0], pt[0], pt[1]);
        if (e == null) {
            // pool full: recycle the farthest regular enemy
            Enemy far = null; float fd = -1;
            for (int i = 0; i < g.enemies.list.size; i++) {
                Enemy o = g.enemies.list.get(i);
                if (o.boss) continue;
                float d = MathUtil.dist2(o.x, o.y, g.player.x, g.player.y);
                if (d > fd) { fd = d; far = o; }
            }
            if (far == null) return null;
            far.init(Config.ENEMIES[0], pt[0], pt[1], 1, 1, 1);
            e = far;
        }
        e.boss = true; e.bossKind = kind;
        e.radius = RADIUS[kind];
        e.maxHp = e.hp = Config.BOSS_BASE_HP * (float) Math.pow(Config.hpMult(g.time), 0.85) * (1f + 0.45f * tier);
        e.damage = Config.BOSS_DAMAGE * Config.dmgMult(g.time);
        e.speed = kind == HIVE ? 150f : (kind == PRISM ? 125f : 75f);
        e.xp = (int) (Config.BOSS_XP * (1 + tier * 0.5f));
        e.score = Config.BOSS_SCORE * (tier + 1);
        e.rotSpeed = 0.6f; e.state = 0; e.stateTimer = 2.5f;
        current = e;
        spawned++;
        nextBossTime = g.time + Config.BOSS_INTERVAL;
        warning = 0;
        g.fx.ring(e.x, e.y, 200, color(e), 0.8f);
        return e;
    }

    private float bulletDmg() { return 12f * Config.dmgMult(g.time); }

    private void radial(Enemy e, int n, float offset, float speed) {
        for (int i = 0; i < n; i++) {
            float a = offset + i * MathUtil.TAU / n;
            g.combat.fireEnemyBullet(e.x, e.y, MathUtil.cos(a), MathUtil.sin(a), speed, bulletDmg(), color(e));
        }
    }

    public void updateBoss(Enemy e, float dt) {
        Player p = g.player;
        float dx = p.x - e.x, dy = p.y - e.y, d = MathUtil.len(dx, dy) + 0.001f;
        float nx = dx / d, ny = dy / d;
        e.stateTimer -= dt;
        switch (e.bossKind) {
            case MONOLITH:
                switch (e.state) {
                    case 0: // stalk
                        e.vx = nx * e.speed; e.vy = ny * e.speed; e.rotSpeed = 0.6f;
                        if (e.stateTimer <= 0) {
                            if (MathUtil.rand() < 0.5f) { e.state = 1; e.stateTimer = 0.9f; }
                            else { e.state = 3; e.stateTimer = 0; e.wobble = 0; }
                        }
                        break;
                    case 1: // telegraph charge
                        e.vx = e.vy = 0; e.dirX = nx; e.dirY = ny; e.rotSpeed = 6f;
                        if (e.stateTimer <= 0) { e.state = 2; e.stateTimer = 0.85f; }
                        break;
                    case 2: // charge
                        e.vx = e.dirX * 620f; e.vy = e.dirY * 620f;
                        if (MathUtil.rand() < 0.7f) g.fx.spawn(e.x, e.y, 0, 0, 0.4f, 14f, color(e), ParticleSystem.DOT);
                        if (e.stateTimer <= 0) { e.state = 0; e.stateTimer = 2.2f; g.camera.addShake(0.3f); radial(e, 10, MathUtil.rand(0, 1), 200f); }
                        break;
                    default: // radial bursts x3
                        e.vx = e.vy = 0; e.rotSpeed = 2f;
                        if (e.stateTimer <= 0) {
                            radial(e, 18, e.wobble * 0.17f, 230f);
                            e.wobble += 1; e.stateTimer = 0.4f;
                            if (e.wobble >= 3) { e.state = 0; e.stateTimer = 2.5f; }
                        }
                }
                break;
            case HIVE: {
                // orbit around the player at ~320 units
                float want = 320f;
                float radial = (d - want) / want;
                float tx = -ny, ty = nx;
                e.vx = (tx * 0.9f + nx * radial * 1.5f) * e.speed;
                e.vy = (ty * 0.9f + ny * radial * 1.5f) * e.speed;
                e.rotSpeed = 1f;
                if (e.state == 0) {
                    if (e.stateTimer <= 0) {
                        if (MathUtil.rand() < 0.5f) {
                            g.waves.spawnNear(Config.ENEMIES[2], e.x, e.y, 6);
                            g.fx.ring(e.x, e.y, 140, color(e), 0.5f);
                            e.stateTimer = 3.5f;
                        } else { e.state = 1; e.stateTimer = 3f; e.wobble = 0; }
                    }
                } else {
                    e.wobble += dt;
                    e.dirX += dt;
                    if (e.dirX >= 0.09f) {
                        e.dirX = 0;
                        float a = e.wobble * 2.4f;
                        g.combat.fireEnemyBullet(e.x, e.y, MathUtil.cos(a), MathUtil.sin(a), 210f, bulletDmg(), color(e));
                        g.combat.fireEnemyBullet(e.x, e.y, MathUtil.cos(a + MathUtil.PI), MathUtil.sin(a + MathUtil.PI), 210f, bulletDmg(), color(e));
                    }
                    if (e.stateTimer <= 0) { e.state = 0; e.stateTimer = 3f; }
                }
                break;
            }
            default: // PRISM
                switch (e.state) {
                    case 0: // approach
                        e.vx = nx * e.speed; e.vy = ny * e.speed; e.rotSpeed = 1.5f;
                        if (e.stateTimer <= 0) {
                            e.state = 1; e.stateTimer = 0.8f;
                            float a = MathUtil.rand(0, MathUtil.TAU);
                            e.dirX = MathUtil.clamp(p.x + MathUtil.cos(a) * 240f, -Config.WORLD_HALF + 100, Config.WORLD_HALF - 100);
                            e.dirY = MathUtil.clamp(p.y + MathUtil.sin(a) * 240f, -Config.WORLD_HALF + 100, Config.WORLD_HALF - 100);
                        }
                        break;
                    case 1: // blink telegraph
                        e.vx = e.vy = 0; e.rotSpeed = 10f;
                        if (e.stateTimer <= 0) {
                            g.fx.burst(e.x, e.y, color(e), 20, 300, 8);
                            e.x = e.dirX; e.y = e.dirY;
                            g.fx.ring(e.x, e.y, 160, color(e), 0.4f);
                            e.state = 2; e.stateTimer = 0.35f; e.wobble = 0;
                        }
                        break;
                    default: // shotgun volleys
                        e.vx = e.vy = 0; e.rotSpeed = 3f;
                        if (e.stateTimer <= 0) {
                            float base = MathUtil.atan2(ny, nx);
                            for (int i = -2; i <= 2; i++) {
                                float a = base + i * 0.22f;
                                g.combat.fireEnemyBullet(e.x, e.y, MathUtil.cos(a), MathUtil.sin(a), 290f, bulletDmg(), color(e));
                            }
                            e.wobble += 1; e.stateTimer = 0.45f;
                            if (e.wobble >= 3) { e.state = 0; e.stateTimer = 2.2f; }
                        }
                }
        }
    }

    /** Telegraph indicator (drawn by the world renderer) - returns true if the boss is winding up. */
    public boolean telegraphing(Enemy e) {
        return (e.bossKind == MONOLITH && e.state == 1) || (e.bossKind == PRISM && e.state == 1);
    }

    public void onBossKilled(Enemy e) {
        g.bossesKilled++;
        g.bossScore += e.score;
        g.xp.addXp(e.xp);
        Player p = g.player;
        p.hp = Math.min(p.maxHp, p.hp + p.maxHp * 0.3f);
        int c = color(e);
        for (int i = 0; i < 4; i++) g.fx.ring(e.x, e.y, 120 + i * 90, i % 2 == 0 ? c : 0xFFFFFFFF, 0.5f + i * 0.2f);
        g.fx.burst(e.x, e.y, c, 60, 600, 12);
        g.fx.burst(e.x, e.y, 0xFFFFD740, 40, 450, 8);
        g.camera.addShake(1f);
        g.platformVibrate(300);
        g.audio.play(Sfx.BOSS_DEATH);
        // clear hostile bullets as a reward
        g.combat.bullets.clear();
        g.ui.announce(g.strings.get("boss_defeated"), "+" + e.score);
        g.pendingBossReward = true;
        current = null;
    }
}
