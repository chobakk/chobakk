package com.blockkillers.core;

/** Projectiles, enemy bullets, damage calculation (crits, armor), kills, explosions and the Nova ability. */
public final class CombatManager {
    private final Game g;
    public final EntityList<Projectile> projectiles = new EntityList<>(500, Projectile::new);
    public final EntityList<EnemyBullet> bullets = new EntityList<>(400, EnemyBullet::new);
    private final int[] hits = new int[256];

    public CombatManager(Game g) { this.g = g; }

    public void clear() { projectiles.clear(); bullets.clear(); }

    public void fireProjectile(float x, float y, float angle, float speed, float damage, float crit, int pierce,
                               float explode, float range, int color, int weaponId, float radius) {
        Projectile p = projectiles.obtain();
        if (p == null) return;
        p.x = x; p.y = y; p.vx = MathUtil.cos(angle) * speed; p.vy = MathUtil.sin(angle) * speed;
        p.life = range / speed; p.damage = damage; p.critChance = crit; p.pierceLeft = pierce;
        p.explodeRadius = explode; p.color = color; p.weaponId = weaponId; p.radius = radius; p.alive = true; p.hitCount = 0;
    }

    public void fireEnemyBullet(float x, float y, float dx, float dy, float speed, float damage, int color) {
        EnemyBullet b = bullets.obtain();
        if (b == null) return;
        b.x = x; b.y = y; b.vx = dx * speed; b.vy = dy * speed; b.life = 6f; b.radius = 8f; b.damage = damage; b.color = color;
    }

    public void update(float dt) {
        EntityList<Enemy> enemies = g.enemies.list;
        for (int i = projectiles.size - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.life -= dt;
            p.x += p.vx * dt; p.y += p.vy * dt;
            if (p.life > 0 && p.alive) {
                int n = g.grid.query(enemies, p.x, p.y, p.radius, hits);
                for (int k = 0; k < n && p.alive; k++) {
                    Enemy e = enemies.get(hits[k]);
                    if (!e.alive || alreadyHit(p, e)) continue;
                    if (p.explodeRadius > 0) {
                        explode(p.x, p.y, p.explodeRadius, p.damage, p.critChance, p.color);
                        p.alive = false;
                        break;
                    }
                    float len = MathUtil.len(p.vx, p.vy) + 0.001f;
                    damageEnemy(e, p.damage, p.critChance, p.vx / len * 120f, p.vy / len * 120f);
                    if (p.hitCount < p.hits.length) p.hits[p.hitCount++] = e;
                    if (p.pierceLeft > 0) p.pierceLeft--; else p.alive = false;
                }
            }
            if (p.life <= 0 && p.alive && p.explodeRadius > 0) { explode(p.x, p.y, p.explodeRadius, p.damage, p.critChance, p.color); p.alive = false; }
            if (p.life <= 0 || !p.alive) projectiles.removeAt(i);
        }

        Player pl = g.player;
        for (int i = bullets.size - 1; i >= 0; i--) {
            EnemyBullet b = bullets.get(i);
            b.life -= dt;
            b.x += b.vx * dt; b.y += b.vy * dt;
            float rr = b.radius + pl.radius - 4f;
            if (MathUtil.dist2(b.x, b.y, pl.x, pl.y) < rr * rr) {
                damagePlayer(b.damage);
                g.fx.burst(b.x, b.y, b.color, 6, 120f, 4f);
                b.life = 0;
            }
            if (b.life <= 0) bullets.removeAt(i);
        }
    }

    private static boolean alreadyHit(Projectile p, Enemy e) {
        for (int i = 0; i < p.hitCount; i++) if (p.hits[i] == e) return true;
        return false;
    }

    /** Applies damage with crit roll. Returns true if the enemy died. */
    public boolean damageEnemy(Enemy e, float dmg, float extraCrit, float kbx, float kby) {
        if (!e.alive) return false;
        Player p = g.player;
        boolean crit = MathUtil.rand() < p.critChance + extraCrit;
        if (crit) dmg *= p.critMult;
        e.hp -= dmg;
        e.flash = 0.09f;
        if (!e.boss) {
            float res = e.type.id == 9 ? 0.3f : 1f; // tanks resist knockback
            e.kx += kbx * res; e.ky += kby * res;
        }
        if (g.settings.showDamageNumbers && (crit || g.settings.quality > 0 || e.boss)) {
            g.fx.text(e.x, e.y - e.radius, Integer.toString(Math.max(1, Math.round(dmg))), crit ? 0xFFFFD740 : 0xFFFFFFFF, crit ? 30f : 21f);
        }
        g.fx.spawn(e.x, e.y, kbx * 0.6f + MathUtil.rand(-60, 60), kby * 0.6f + MathUtil.rand(-60, 60), 0.25f, 4f,
                e.boss ? g.bosses.color(e) : e.type.color, ParticleSystem.DOT);
        g.audio.play(Sfx.HIT);
        if (e.hp <= 0) { kill(e); return true; }
        return false;
    }

    public void kill(Enemy e) {
        if (!e.alive) return;
        e.alive = false;
        if (e.boss) { g.bosses.onBossKilled(e); return; }
        g.kills++;
        g.killsByType[e.type.id]++;
        g.killScore += e.score;
        g.xp.addXp(e.xp);
        g.fx.burst(e.x, e.y, e.type.color, 14, 260f, e.radius * 0.45f);
        if (g.settings.quality == SettingsManager.Q_HIGH) g.fx.ring(e.x, e.y, e.radius * 2.2f, e.type.color, 0.35f);
        g.fx.spawn(e.x, e.y, 0, 0, 0.18f, e.radius * 1.4f, 0xFFFFFFFF, ParticleSystem.DOT);
        g.audio.play(Sfx.DEATH);
        if (e.type.behavior == EnemyType.B_SPLIT) {
            EnemyType mini = Config.ENEMIES[Config.MINI_ID];
            for (int i = 0; i < Config.SPLIT_COUNT; i++) {
                float a = i * MathUtil.TAU / Config.SPLIT_COUNT + MathUtil.rand(0, 1);
                Enemy m = g.enemies.spawn(mini, e.x + MathUtil.cos(a) * 14, e.y + MathUtil.sin(a) * 14);
                if (m != null) { m.kx = MathUtil.cos(a) * 260; m.ky = MathUtil.sin(a) * 260; m.spawnAnim = 0.6f; }
            }
        }
    }

    public void explode(float x, float y, float r, float dmg, float crit, int color) {
        EntityList<Enemy> enemies = g.enemies.list;
        int n = g.grid.query(enemies, x, y, r, hits);
        for (int k = 0; k < n; k++) {
            Enemy e = enemies.get(hits[k]);
            float dx = e.x - x, dy = e.y - y, d = MathUtil.len(dx, dy) + 0.01f;
            damageEnemy(e, dmg, crit, dx / d * 220f, dy / d * 220f);
        }
        g.fx.ring(x, y, r, color, 0.35f);
        g.fx.burst(x, y, color, 12, 300f, 6f);
        g.fx.burst(x, y, 0xFFFFFFFF, 6, 200f, 4f);
        g.camera.addShake(0.12f);
        g.audio.play(Sfx.EXPLODE);
    }

    /** Area damage used by Pulse and Nova. */
    public int areaDamage(float x, float y, float r, float dmg, float knock) {
        EntityList<Enemy> enemies = g.enemies.list;
        int n = g.grid.query(enemies, x, y, r, hits);
        for (int k = 0; k < n; k++) {
            Enemy e = enemies.get(hits[k]);
            float dx = e.x - x, dy = e.y - y, d = MathUtil.len(dx, dy) + 0.01f;
            damageEnemy(e, dmg, 0, dx / d * knock, dy / d * knock);
        }
        return n;
    }

    public void damagePlayer(float dmg) {
        Player p = g.player;
        if (!p.alive || p.invuln > 0 || g.godMode) return;
        float reduced = dmg * (1f - p.armor / (p.armor + Config.ARMOR_K));
        p.hp -= reduced;
        p.invuln = Config.PLAYER_INVULN;
        p.hurtFlash = 0.25f;
        g.camera.addShake(0.35f);
        g.platformVibrate(35);
        g.audio.play(Sfx.HURT);
        g.fx.burst(p.x, p.y, 0xFFFF5252, 10, 220f, 5f);
        g.fx.text(p.x, p.y - 30, "-" + Math.max(1, Math.round(reduced)), 0xFFFF5252, 24f);
        if (p.hp <= 0) { p.hp = 0; g.onPlayerDeath(); }
    }

    /** Nova Burst ability: damages and repels everything nearby and erases hostile bullets. */
    public void nova() {
        Player p = g.player;
        float r = Config.NOVA_RADIUS * (0.7f + 0.3f * p.rangeMult);
        float dmg = Config.NOVA_DAMAGE * p.damageMult * (1f + 0.06f * (p.level - 1));
        areaDamage(p.x, p.y, r, dmg, Config.NOVA_KNOCKBACK);
        for (int i = bullets.size - 1; i >= 0; i--) {
            EnemyBullet b = bullets.get(i);
            if (MathUtil.dist2(b.x, b.y, p.x, p.y) < r * r) { g.fx.spawn(b.x, b.y, 0, 0, 0.3f, 6, b.color, ParticleSystem.DOT); bullets.removeAt(i); }
        }
        g.fx.ring(p.x, p.y, r, 0xFF40C4FF, 0.5f);
        g.fx.ring(p.x, p.y, r * 0.7f, 0xFFFFFFFF, 0.35f);
        g.fx.burst(p.x, p.y, 0xFF40C4FF, 30, 500f, 6f);
        g.camera.addShake(0.4f);
        g.platformVibrate(30);
        g.audio.play(Sfx.NOVA);
    }
}
