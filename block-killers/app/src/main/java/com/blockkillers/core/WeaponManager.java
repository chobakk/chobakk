package com.blockkillers.core;

import java.util.ArrayList;

/** Owned weapons and their firing logic (projectiles, orbiting blades, chain lightning, area pulse). */
public final class WeaponManager {
    private final Game g;
    public final ArrayList<Weapon> owned = new ArrayList<>();
    private final int[] hits = new int[256];
    private int chainId;
    public static final float BLADE_HIT_RADIUS = 26f;

    public WeaponManager(Game g) { this.g = g; }

    public void reset() {
        owned.clear();
        owned.add(new Weapon(Config.WEAPONS[Config.W_BLASTER]));
    }

    public Weapon get(int id) {
        for (int i = 0; i < owned.size(); i++) if (owned.get(i).type.id == id) return owned.get(i);
        return null;
    }
    public boolean canAddMore() { return owned.size() < Config.MAX_WEAPONS; }
    public void add(int id) { if (get(id) == null && canAddMore()) owned.add(new Weapon(Config.WEAPONS[id])); }
    public void levelUp(int id, int levels) {
        Weapon w = get(id);
        if (w != null) w.level = Math.min(Config.WEAPON_MAX_LEVEL, w.level + levels);
    }

    public void update(float dt) {
        Player p = g.player;
        for (int i = 0; i < owned.size(); i++) {
            Weapon w = owned.get(i);
            switch (w.type.kind) {
                case WeaponType.K_ORBIT: updateBlades(w, p, dt); break;
                case WeaponType.K_CHAIN: updateChain(w, p, dt); break;
                case WeaponType.K_AREA: updatePulse(w, p, dt); break;
                default: updateGun(w, p, dt);
            }
        }
    }

    private void updateGun(Weapon w, Player p, float dt) {
        w.timer -= dt;
        if (w.timer > 0) return;
        float range = w.range(p);
        Enemy target = pickTarget(p, range);
        if (target == null) { w.timer = 0.08f; return; }
        w.timer = w.cooldown(p);
        float base = MathUtil.atan2(target.y - p.y, target.x - p.x);
        p.facing = base;
        float dmg = w.damage(p), spd = w.projSpeed(p);
        int count = w.count(p);
        float rad = w.type.id == Config.W_ROCKET ? 9f : (w.type.id == Config.W_PIERCER ? 7f : 6f);
        float explode = w.explodeRadius(p);
        float startX = p.x + MathUtil.cos(base) * p.radius, startY = p.y + MathUtil.sin(base) * p.radius;
        switch (w.type.kind) {
            case WeaponType.K_PARALLEL: {
                float nx = -MathUtil.sin(base), ny = MathUtil.cos(base);
                for (int k = 0; k < count; k++) {
                    float off = (k - (count - 1) / 2f) * 14f;
                    g.combat.fireProjectile(startX + nx * off, startY + ny * off, base, spd, dmg, w.type.critBonus, w.pierce(), explode, range, w.type.color, w.type.id, rad);
                }
                break;
            }
            case WeaponType.K_SPREAD: {
                float step = (float) Math.toRadians(w.type.spreadDeg);
                for (int k = 0; k < count; k++) {
                    float a = base + (k - (count - 1) / 2f) * step;
                    g.combat.fireProjectile(startX, startY, a, spd, dmg, w.type.critBonus, w.pierce(), explode, range, w.type.color, w.type.id, rad);
                }
                break;
            }
            default: {
                float step = (float) Math.toRadians(7);
                for (int k = 0; k < count; k++) {
                    float a = base + (k - (count - 1) / 2f) * step;
                    g.combat.fireProjectile(startX, startY, a, spd, dmg, w.type.critBonus, w.pierce(), explode, range, w.type.color, w.type.id, rad);
                }
            }
        }
        g.fx.spawn(startX, startY, MathUtil.cos(base) * 80, MathUtil.sin(base) * 80, 0.12f, 7f, w.type.color, ParticleSystem.DOT);
        g.audio.play(Sfx.SHOOT);
    }

    /** Nearest enemy, but focus the boss when it is in range and nothing is dangerously close. */
    private Enemy pickTarget(Player p, float range) {
        Enemy n = g.enemies.nearest(p.x, p.y, range);
        Enemy b = g.bosses.current;
        if (b != null && b.alive && n != b) {
            float rr = range + b.radius;
            if (MathUtil.dist2(p.x, p.y, b.x, b.y) < rr * rr) {
                boolean threat = n != null && MathUtil.dist2(p.x, p.y, n.x, n.y) < 110f * 110f;
                if (!threat || MathUtil.rand() < 0.35f) return b;
            }
        }
        return n;
    }

    /** Current world positions of orbiting blades are derived from angle + index. */
    public float bladeRadius(Weapon w, Player p) { return w.type.range * (1f + 0.06f * (w.level - 1)) * (0.6f + 0.4f * p.rangeMult); }

    private void updateBlades(Weapon w, Player p, float dt) {
        w.angle += w.type.projSpeed * (1f + 0.05f * (w.level - 1)) * dt;
        int n = w.count(p);
        float orbit = bladeRadius(w, p);
        float dmg = w.damage(p);
        float cd = w.cooldown(p);
        EntityList<Enemy> list = g.enemies.list;
        for (int b = 0; b < n; b++) {
            float a = w.angle + b * MathUtil.TAU / n;
            float bx = p.x + MathUtil.cos(a) * orbit, by = p.y + MathUtil.sin(a) * orbit;
            int c = g.grid.query(list, bx, by, BLADE_HIT_RADIUS, hits);
            for (int k = 0; k < c; k++) {
                Enemy e = list.get(hits[k]);
                if (e.bladeHitTimer > 0) continue;
                e.bladeHitTimer = cd;
                float tx = -MathUtil.sin(a), ty = MathUtil.cos(a);
                g.combat.damageEnemy(e, dmg, w.type.critBonus, tx * 180f, ty * 180f);
            }
        }
    }

    private void updateChain(Weapon w, Player p, float dt) {
        w.timer -= dt;
        if (w.timer > 0) return;
        Enemy first = g.enemies.nearest(p.x, p.y, w.range(p));
        if (first == null) { w.timer = 0.1f; return; }
        w.timer = w.cooldown(p);
        chainId++;
        int jumps = w.count(p);
        float dmg = w.damage(p);
        float fx = p.x, fy = p.y;
        Enemy cur = first;
        EntityList<Enemy> list = g.enemies.list;
        float jumpRange = Config.CHAIN_JUMP * (0.6f + 0.4f * p.weaponPower);
        for (int j = 0; j < jumps && cur != null; j++) {
            g.fx.bolt(fx, fy, cur.x, cur.y, w.type.color);
            cur.lastChainId = chainId;
            fx = cur.x; fy = cur.y;
            g.combat.damageEnemy(cur, dmg, w.type.critBonus, 0, 0);
            // next target: nearest not yet hit in this chain
            Enemy next = null;
            float bd = jumpRange * jumpRange;
            int c = g.grid.query(list, fx, fy, jumpRange, hits);
            for (int k = 0; k < c; k++) {
                Enemy e = list.get(hits[k]);
                if (!e.alive || e.lastChainId == chainId) continue;
                float d = MathUtil.dist2(fx, fy, e.x, e.y);
                if (d < bd) { bd = d; next = e; }
            }
            cur = next;
            dmg *= 0.9f;
        }
        g.audio.play(Sfx.ZAP);
    }

    private void updatePulse(Weapon w, Player p, float dt) {
        w.timer -= dt;
        if (w.timer > 0) return;
        float r = w.areaRadius(p);
        if (g.enemies.nearest(p.x, p.y, r) == null) { w.timer = 0.15f; return; }
        w.timer = w.cooldown(p);
        g.combat.areaDamage(p.x, p.y, r, w.damage(p), 160f);
        g.fx.ring(p.x, p.y, r, w.type.color, 0.4f);
        g.audio.play(Sfx.PULSE);
    }
}
