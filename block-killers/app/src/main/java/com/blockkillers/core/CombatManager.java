package com.blockkillers.core;

/**
 * Projectiles (incl. homing, boomerang, ricochet, flame, rail, mines), enemy bullets, damage (crits, armor,
 * elements), kills, explosions, legendary traits, obstacle collisions and the Nova ability.
 */
public final class CombatManager {
    private final Game g;
    public final EntityList<Projectile> projectiles = new EntityList<>(900, Projectile::new);
    public final EntityList<EnemyBullet> bullets = new EntityList<>(400, EnemyBullet::new);
    private final int[] hits = new int[256], areaHits = new int[256], shockHits = new int[64], seekHits = new int[64];
    // deferred explosions (from Volatile kills) so nested grid queries never clash
    private final float[] pendX = new float[64], pendY = new float[64], pendR = new float[64], pendD = new float[64];
    private final int[] pendC = new int[64];
    private int pendN;

    public CombatManager(Game g) { this.g = g; }

    public void clear() { projectiles.clear(); bullets.clear(); pendN = 0; }

    /** Simple projectile (used by tests and legacy callers). */
    public void fireProjectile(float x, float y, float angle, float speed, float damage, float crit, int pierce,
                               float explode, float range, int color, int weaponId, float radius) {
        fireProjectile(x, y, angle, speed, damage, crit, pierce, explode, range, color, weaponId, radius, WeaponType.SP_NONE, Loot.E_NONE, 0, false, null);
    }

    public Projectile fireProjectile(float x, float y, float angle, float speed, float damage, float crit, int pierce, float explode,
                                     float range, int color, int weaponId, float radius, int special, int element, int bounces,
                                     boolean canSplit, Weapon src) {
        Projectile p = projectiles.obtain();
        if (p == null) return null;
        p.x = x; p.y = y; p.vx = MathUtil.cos(angle) * speed; p.vy = MathUtil.sin(angle) * speed; p.speed = speed;
        p.life = special == WeaponType.SP_MINE ? Config.MINE_LIFE : (special == WeaponType.SP_BOOMERANG ? 10f : range / Math.max(1f, speed));
        if (special == WeaponType.SP_BOOMERANG) p.explodeRadius = 0;
        p.damage = damage; p.critChance = crit; p.pierceLeft = pierce; p.explodeRadius = explode; p.color = color;
        p.weaponId = weaponId; p.radius = radius; p.alive = true; p.hitCount = 0; p.special = special; p.element = element;
        p.bounces = bounces; p.canSplit = canSplit; p.src = src; p.age = 0; p.returning = false;
        if (special == WeaponType.SP_BOOMERANG) p.critChance = crit;
        // boomerang turn time is stored in explodeRadius-free field: use range/speed
        if (special == WeaponType.SP_BOOMERANG) p.bounces = (int) (range / Math.max(1f, speed) * 1000); // turn time in ms
        return p;
    }

    public void fireEnemyBullet(float x, float y, float dx, float dy, float speed, float damage, int color) {
        EnemyBullet b = bullets.obtain();
        if (b == null) return;
        b.x = x; b.y = y; b.vx = dx * speed; b.vy = dy * speed; b.life = 6f; b.radius = 8f; b.damage = damage; b.color = color;
    }

    public void update(float dt) {
        EntityList<Enemy> enemies = g.enemies.list;
        WorldMap map = g.map;
        Player pl = g.player;
        for (int i = projectiles.size - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.age += dt;
            p.life -= dt;
            switch (p.special) {
                case WeaponType.SP_HOMING: homing(p, dt); break;
                case WeaponType.SP_BOOMERANG: {
                    if (!p.returning && p.age * 1000 >= p.bounces) { p.returning = true; p.hitCount = 0; }
                    if (p.returning) {
                        float dx = pl.x - p.x, dy = pl.y - p.y, d = MathUtil.len(dx, dy) + 0.01f;
                        p.vx = dx / d * p.speed * 1.1f; p.vy = dy / d * p.speed * 1.1f;
                        if (d < pl.radius + 10) p.alive = false;
                    }
                    break;
                }
                case WeaponType.SP_FLAME: {
                    float d = Math.max(0f, 1f - 1.6f * dt);
                    p.vx *= d; p.vy *= d; p.radius += 22f * dt;
                    break;
                }
                case WeaponType.SP_MINE: {
                    if (p.age >= Config.MINE_ARM && g.grid.query(enemies, p.x, p.y, Config.MINE_TRIGGER, hits) > 0) {
                        explode(p.x, p.y, p.explodeRadius, p.damage, p.critChance, p.color, p.src);
                        p.alive = false;
                    }
                    if (p.life <= 0 && p.alive) { explode(p.x, p.y, p.explodeRadius * 0.7f, p.damage * 0.5f, 0, p.color, p.src); p.alive = false; }
                    if (!p.alive) projectiles.removeAt(i);
                    continue;
                }
            }
            p.x += p.vx * dt; p.y += p.vy * dt;
            if (p.alive && map.outside(p.x, p.y, 30)) { g.fx.burst(p.x, p.y, p.color, 3, 120f, 3f); p.alive = false; }
            if (p.alive && p.special != WeaponType.SP_RAIL && map.blocked(p.x, p.y, p.radius * 0.5f)) {
                if (p.explodeRadius > 0) explode(p.x, p.y, p.explodeRadius, p.damage, p.critChance, p.color, p.src);
                else g.fx.burst(p.x, p.y, p.color, 4, 140f, 3f);
                p.alive = false;
            }
            if (p.life > 0 && p.alive) {
                int n = g.grid.query(enemies, p.x, p.y, p.radius, hits);
                for (int k = 0; k < n && p.alive; k++) {
                    Enemy e = enemies.get(hits[k]);
                    if (!e.alive || alreadyHit(p, e)) continue;
                    if (p.explodeRadius > 0) {
                        explode(p.x, p.y, p.explodeRadius, p.damage, p.critChance, p.color, p.src);
                        p.alive = false;
                        break;
                    }
                    float len = MathUtil.len(p.vx, p.vy) + 0.001f;
                    damageEnemy(e, p.damage, p.critChance, p.vx / len * 120f, p.vy / len * 120f, p.src);
                    if (p.hitCount < p.hits.length) p.hits[p.hitCount++] = e;
                    if (p.canSplit) split(p);
                    if (p.special == WeaponType.SP_RICOCHET) {
                        if (p.bounces > 0 && retarget(p, e)) { p.bounces--; break; }
                        p.alive = false;
                    } else if (p.pierceLeft > 0) p.pierceLeft--;
                    else p.alive = false;
                }
            }
            if (p.life <= 0 && p.alive && p.explodeRadius > 0) { explode(p.x, p.y, p.explodeRadius, p.damage, p.critChance, p.color, p.src); p.alive = false; }
            if (p.life <= 0 || !p.alive) projectiles.removeAt(i);
        }

        interceptBullets();
        for (int i = bullets.size - 1; i >= 0; i--) {
            EnemyBullet b = bullets.get(i);
            b.life -= dt;
            b.x += b.vx * dt; b.y += b.vy * dt;
            if (map.blocked(b.x, b.y, b.radius * 0.5f) || map.outside(b.x, b.y, 30)) { g.fx.burst(b.x, b.y, b.color, 3, 100f, 3f); b.life = 0; }
            float rr = b.radius + pl.radius - 4f;
            if (b.life > 0 && MathUtil.dist2(b.x, b.y, pl.x, pl.y) < rr * rr) {
                damagePlayer(b.damage);
                g.fx.burst(b.x, b.y, b.color, 6, 120f, 4f);
                b.life = 0;
            }
            if (b.life <= 0) bullets.removeAt(i);
        }
        // deferred explosions
        for (int i = 0; i < pendN; i++) explode(pendX[i], pendY[i], pendR[i], pendD[i], 0, pendC[i], null);
        pendN = 0;
    }

    /** Player shots destroy enemy bullets they touch (non-piercing shots are used up). */
    private void interceptBullets() {
        if (bullets.size == 0 || projectiles.size == 0) return;
        int checks = 0;
        for (int i = bullets.size - 1; i >= 0; i--) {
            EnemyBullet b = bullets.get(i);
            for (int k = 0; k < projectiles.size; k++) {
                if (++checks > 80000) return;   // safety cap for extreme screens
                Projectile p = projectiles.get(k);
                if (!p.alive || p.special == WeaponType.SP_MINE) continue;
                float rr = b.radius + p.radius + 4;
                if (MathUtil.dist2(b.x, b.y, p.x, p.y) < rr * rr) {
                    popBullet(b);
                    b.life = 0;
                    if (p.special != WeaponType.SP_RAIL && p.special != WeaponType.SP_BOOMERANG && p.special != WeaponType.SP_FLAME) {
                        if (p.pierceLeft > 0) p.pierceLeft--; else p.alive = false;
                    }
                    break;
                }
            }
        }
    }

    private void popBullet(EnemyBullet b) {
        g.fx.spawn(b.x, b.y, 0, 0, 0.2f, b.radius * 1.6f, 0xFFFFFFFF, ParticleSystem.DOT);
        g.fx.burst(b.x, b.y, b.color, 4, 150f, 3f);
        g.bulletsDestroyed++;
    }

    /** Destroys enemy bullets inside a circle (explosions, pulse, nova, blades). */
    public int clearBullets(float x, float y, float r) {
        int n = 0;
        for (int i = bullets.size - 1; i >= 0; i--) {
            EnemyBullet b = bullets.get(i);
            float rr = r + b.radius;
            if (MathUtil.dist2(b.x, b.y, x, y) < rr * rr) { popBullet(b); bullets.removeAt(i); n++; }
        }
        return n;
    }

    /** Destroys enemy bullets along a beam segment. */
    public void clearBulletsOnLine(float x0, float y0, float dx, float dy, float len, float width) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            EnemyBullet b = bullets.get(i);
            float ex = b.x - x0, ey = b.y - y0;
            float along = ex * dx + ey * dy;
            if (along < 0 || along > len) continue;
            if (Math.abs(ex * dy - ey * dx) < width + b.radius) { popBullet(b); bullets.removeAt(i); }
        }
    }

    private void homing(Projectile p, float dt) {
        int n = g.grid.query(g.enemies.list, p.x, p.y, 280f, seekHits);
        Enemy best = null; float bd = Float.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            Enemy e = g.enemies.list.get(seekHits[k]);
            float d = MathUtil.dist2(p.x, p.y, e.x, e.y);
            if (d < bd) { bd = d; best = e; }
        }
        if (best == null || p.age < 0.12f) return;
        float want = MathUtil.atan2(best.y - p.y, best.x - p.x), cur = MathUtil.atan2(p.vy, p.vx);
        float diff = want - cur;
        while (diff > MathUtil.PI) diff -= MathUtil.TAU;
        while (diff < -MathUtil.PI) diff += MathUtil.TAU;
        cur += MathUtil.clamp(diff, -7f * dt, 7f * dt);
        p.vx = MathUtil.cos(cur) * p.speed; p.vy = MathUtil.sin(cur) * p.speed;
        if (MathUtil.rand() < 0.5f) g.fx.spawn(p.x, p.y, -p.vx * 0.1f, -p.vy * 0.1f, 0.25f, 4f, 0xFFFF9100, ParticleSystem.DOT);
    }

    private boolean retarget(Projectile p, Enemy from) {
        int n = g.grid.query(g.enemies.list, p.x, p.y, 320f, seekHits);
        Enemy best = null; float bd = Float.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            Enemy e = g.enemies.list.get(seekHits[k]);
            if (e == from || !e.alive || alreadyHit(p, e)) continue;
            float d = MathUtil.dist2(p.x, p.y, e.x, e.y);
            if (d < bd) { bd = d; best = e; }
        }
        if (best == null) return false;
        float a = MathUtil.atan2(best.y - p.y, best.x - p.x);
        p.vx = MathUtil.cos(a) * p.speed; p.vy = MathUtil.sin(a) * p.speed;
        p.life = Math.max(p.life, 0.8f);
        g.fx.ring(p.x, p.y, 18, p.color, 0.15f);
        return true;
    }

    private void split(Projectile p) {
        float a = MathUtil.atan2(p.vy, p.vx);
        for (int s = -1; s <= 1; s += 2) {
            Projectile c = fireProjectile(p.x, p.y, a + s * 0.6f, p.speed * 0.9f, p.damage * 0.5f, p.critChance, 0, 0, 220,
                    p.color, p.weaponId, p.radius * 0.7f, WeaponType.SP_NONE, p.element, 0, false, p.src);
            if (c != null) { c.hits[0] = p.hits[p.hitCount - 1]; c.hitCount = 1; }
        }
    }

    private static boolean alreadyHit(Projectile p, Enemy e) {
        for (int i = 0; i < p.hitCount; i++) if (p.hits[i] == e) return true;
        return false;
    }

    /** Damage without a source weapon (Nova, tests). */
    public boolean damageEnemy(Enemy e, float dmg, float extraCrit, float kbx, float kby) {
        return damageEnemy(e, dmg, extraCrit, kbx, kby, null);
    }

    /**
     * Applies damage: crit roll, slag amplification, then element matchups — shields absorb first
     * (Shock x2), then armor (Acid x1.75) or unprotected health (Burn x1.5). Returns true if the enemy died.
     */
    public boolean damageEnemy(Enemy e, float dmg, float extraCrit, float kbx, float kby, Weapon src) {
        if (!e.alive) return false;
        Player p = g.player;
        int element = src == null ? Loot.E_NONE : src.element;
        boolean crit = MathUtil.rand() < p.critChance + extraCrit;
        if (crit) dmg *= p.critMult;
        if (e.slagT > 0 && element != Loot.E_SLAG) dmg *= 1.5f;
        float dealt;
        boolean shieldHit = false;
        if (e.shield > 0) {
            float sd = dmg * Loot.vsShield(element);
            shieldHit = true;
            if (sd >= e.shield) {
                float overflow = (sd - e.shield) / Loot.vsShield(element);
                e.shield = 0;
                breakShield(e);
                dealt = sd;
                e.hp -= overflow * (e.armored ? Loot.vsArmor(element) : Loot.vsFlesh(element));
            } else { e.shield -= sd; dealt = sd; }
            e.shieldFlash = 0.15f;
        } else {
            dealt = dmg * (e.armored ? Loot.vsArmor(element) : Loot.vsFlesh(element));
            e.hp -= dealt;
        }
        e.flash = 0.09f;
        if (src != null) e.lastHitBy = src;
        if (!e.boss) {
            float res = e.type.id == 9 ? 0.3f : (e.tier == 2 ? 0.5f : 1f);
            e.kx += kbx * res; e.ky += kby * res;
        }
        switch (element) {
            case Loot.E_FIRE: e.burnT = 2.5f; e.burnDps = Math.max(e.burnDps, dmg * 0.3f); break;
            case Loot.E_ACID: e.poisonT = 4f; e.poisonDps = Math.max(e.poisonDps, dmg * 0.2f); break;
            case Loot.E_FROST: e.slowT = 1.6f; e.slowAmt = e.boss ? 0.15f : 0.45f; break;
            case Loot.E_SLAG: e.slagT = 4f; break;
            case Loot.E_SHOCK:
                if (MathUtil.rand() < 0.3f) {
                    int n = g.grid.query(g.enemies.list, e.x, e.y, 140f, shockHits);
                    for (int k = 0; k < n; k++) {
                        Enemy o = g.enemies.list.get(shockHits[k]);
                        if (o == e || !o.alive) continue;
                        g.fx.bolt(e.x, e.y, o.x, o.y, Loot.ELEMENT_COLORS[Loot.E_SHOCK]);
                        damageEnemy(o, dmg * 0.5f, 0, 0, 0, null);
                        break;
                    }
                }
                break;
        }
        if (g.settings.showDamageNumbers && (crit || g.settings.quality > 0 || e.boss)) {
            int col = crit ? 0xFFFFD740 : (shieldHit ? 0xFF448AFF : (element != Loot.E_NONE ? Loot.ELEMENT_COLORS[element] : 0xFFFFFFFF));
            g.fx.text(e.x, e.y - e.radius, Integer.toString(Math.max(1, Math.round(dealt))), col, crit ? 30f : 21f);
        }
        g.fx.spawn(e.x, e.y, kbx * 0.6f + MathUtil.rand(-60, 60), kby * 0.6f + MathUtil.rand(-60, 60), 0.25f, 4f,
                shieldHit ? 0xFF448AFF : (e.boss ? g.bosses.color(e) : e.fuseColor), ParticleSystem.DOT);
        g.audio.play(Sfx.HIT);
        if (e.hp <= 0) { kill(e); return true; }
        return false;
    }

    private void breakShield(Enemy e) {
        g.fx.ring(e.x, e.y, e.radius * 1.8f, 0xFF448AFF, 0.35f);
        g.fx.burst(e.x, e.y, 0xFF82B1FF, 12, 260f, 5f);
        g.audio.play(Sfx.ZAP);
    }

    public void kill(Enemy e) {
        if (!e.alive) return;
        e.alive = false;
        Weapon src = e.lastHitBy;
        if (src != null) {
            src.kills++;
            if (src.trait == Loot.T_VAMPIRIC) {
                Player p = g.player;
                p.hp = Math.min(p.maxHp, p.hp + 1.5f);
            } else if (src.trait == Loot.T_VOLATILE && pendN < pendX.length) {
                pendX[pendN] = e.x; pendY[pendN] = e.y; pendR[pendN] = 70 + e.radius; pendD[pendN] = src.damage(g.player) * 0.5f; pendC[pendN] = Loot.COLORS[Loot.LEGENDARY]; pendN++;
            }
        }
        if (e.boss) { g.bosses.onBossKilled(e); return; }
        g.kills++;
        g.killsByType[e.type.id]++;
        g.addCoins(0.3f * (1 + e.tier * 3));
        g.meme.onKill(e);
        if (g.kills % Config.PASSIVE_KILLS == 0) {
            // passive: every 20 kills restores 2 HP
            Player pp = g.player;
            pp.hp = Math.min(pp.maxHp, pp.hp + Config.PASSIVE_HEAL);
            g.fx.text(pp.x, pp.y - pp.radius - 20, "+" + (int) Config.PASSIVE_HEAL + " HP", 0xFF00E676, 24f);
        }
        g.killScore += e.score;
        g.xp.addXp(e.xp);
        g.loot.onEnemyKilled(e);
        g.fx.burst(e.x, e.y, e.fuseColor, 14 + e.tier * 8, 260f, e.radius * 0.45f);
        if (e.tier > 0) g.fx.burst(e.x, e.y, e.type.color, 10, 220f, e.radius * 0.35f);
        if (g.settings.quality == SettingsManager.Q_HIGH) g.fx.ring(e.x, e.y, e.radius * 2.2f, e.fuseColor, 0.35f);
        g.fx.spawn(e.x, e.y, 0, 0, 0.18f, e.radius * 1.4f, 0xFFFFFFFF, ParticleSystem.DOT);
        g.audio.play(Sfx.DEATH);
        if (e.type.behavior == EnemyType.B_SPLIT) {
            EnemyType mini = Config.ENEMIES[Config.MINI_ID];
            for (int i = 0; i < Config.SPLIT_COUNT + e.tier; i++) {
                float a = i * MathUtil.TAU / Config.SPLIT_COUNT + MathUtil.rand(0, 1);
                Enemy m = g.enemies.spawn(mini, e.x + MathUtil.cos(a) * 14, e.y + MathUtil.sin(a) * 14);
                if (m != null) { m.kx = MathUtil.cos(a) * 260; m.ky = MathUtil.sin(a) * 260; m.spawnAnim = 0.6f; }
            }
        }
    }

    public void explode(float x, float y, float r, float dmg, float crit, int color) { explode(x, y, r, dmg, crit, color, null); }

    public void explode(float x, float y, float r, float dmg, float crit, int color, Weapon src) {
        EntityList<Enemy> enemies = g.enemies.list;
        int n = g.grid.query(enemies, x, y, r, areaHits);
        for (int k = 0; k < n; k++) {
            Enemy e = enemies.get(areaHits[k]);
            float dx = e.x - x, dy = e.y - y, d = MathUtil.len(dx, dy) + 0.01f;
            damageEnemy(e, dmg, crit, dx / d * 220f, dy / d * 220f, src);
        }
        clearBullets(x, y, r);
        g.fx.ring(x, y, r, color, 0.35f);
        g.fx.burst(x, y, color, 12, 300f, 6f);
        g.fx.burst(x, y, 0xFFFFFFFF, 6, 200f, 4f);
        g.camera.addShake(0.12f);
        g.audio.play(Sfx.EXPLODE);
    }

    /** Area damage used by Pulse and Nova. */
    public int areaDamage(float x, float y, float r, float dmg, float knock) { return areaDamage(x, y, r, dmg, knock, null); }

    public int areaDamage(float x, float y, float r, float dmg, float knock, Weapon src) {
        clearBullets(x, y, r);
        EntityList<Enemy> enemies = g.enemies.list;
        int n = g.grid.query(enemies, x, y, r, areaHits);
        for (int k = 0; k < n; k++) {
            Enemy e = enemies.get(areaHits[k]);
            float dx = e.x - x, dy = e.y - y, d = MathUtil.len(dx, dy) + 0.01f;
            damageEnemy(e, dmg, 0, dx / d * knock, dy / d * knock, src);
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
        if (!g.meme.onHurt()) g.audio.play(Sfx.HURT);
        g.fx.burst(p.x, p.y, 0xFFFF5252, 10, 220f, 5f);
        g.fx.text(p.x, p.y - 30, "-" + Math.max(1, Math.round(reduced)), 0xFFFF5252, 24f);
        if (p.hp <= 0) { p.hp = 0; g.onPlayerDeath(); }
    }

    /** Nova Burst ability: damages and repels everything nearby and erases hostile bullets. */
    public void nova() {
        Player p = g.player;
        float r = Config.NOVA_RADIUS * (0.7f + 0.3f * p.rangeMult) * p.radius / Config.PLAYER_RADIUS;
        float dmg = Config.NOVA_DAMAGE * p.damageMult * (1f + 0.06f * (p.level - 1));
        areaDamage(p.x, p.y, r, dmg, Config.NOVA_KNOCKBACK);
        g.fx.ring(p.x, p.y, r, 0xFF40C4FF, 0.5f);
        g.fx.ring(p.x, p.y, r * 0.7f, 0xFFFFFFFF, 0.35f);
        g.fx.burst(p.x, p.y, 0xFF40C4FF, 30, 500f, 6f);
        g.camera.addShake(0.4f);
        g.platformVibrate(30);
        g.audio.play(Sfx.NOVA);
    }
}
