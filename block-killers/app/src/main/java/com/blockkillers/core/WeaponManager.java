package com.blockkillers.core;

/**
 * 5-slot gun hotbar. Only the gun in the player's hand (the active slot) fires; tapping a hotbar slot switches.
 * Firing logic for every gun kind: bullets, parallel, spread, orbit blades, chain lightning, pulse, laser beam, mines.
 */
public final class WeaponManager {
    public static final float BLADE_HIT_RADIUS = 26f;
    private final Game g;
    public final Weapon[] slots = new Weapon[Config.HOTBAR];
    public int active;
    public float switchTimer;
    private final int[] hits = new int[256];
    private int chainId;

    public WeaponManager(Game g) { this.g = g; }

    public void reset() {
        for (int i = 0; i < slots.length; i++) slots[i] = null;
        slots[0] = new Weapon(Config.WEAPONS[Config.W_BLASTER]);
        active = 0; switchTimer = 0;
    }

    public Weapon activeWeapon() { return slots[active]; }
    public int count() { int n = 0; for (Weapon w : slots) if (w != null) n++; return n; }
    public int firstFree() { for (int i = 0; i < slots.length; i++) if (slots[i] == null) return i; return -1; }
    public boolean canAddMore() { return firstFree() >= 0; }

    public Weapon get(int typeId) {
        for (Weapon w : slots) if (w != null && w.type.id == typeId) return w;
        return null;
    }

    /** Adds a common gun of the given type into the first free slot (if any). */
    public void add(int typeId) {
        int s = firstFree();
        if (s < 0) return;
        Weapon w = new Weapon(Config.WEAPONS[typeId]);
        w.itemLevel = g.loot.itemLevel();
        slots[s] = w;
    }

    public int addInstance(Weapon w) {
        int s = firstFree();
        if (s >= 0) slots[s] = w;
        return s;
    }

    /** Puts a gun into a slot (replacing what was there). */
    public void place(int slot, Weapon w) {
        slots[slot] = w;
        w.timer = 0.15f;
        if (slot == active) switchTimer = Config.SWITCH_DELAY;
    }

    public void select(int slot) {
        if (slot < 0 || slot >= slots.length || slots[slot] == null || slot == active) return;
        Weapon old = slots[active];
        if (old != null) old.beamTime = 0;
        active = slot;
        switchTimer = Config.SWITCH_DELAY;
        g.audio.play(Sfx.CLICK);
    }

    public void levelUp(int slot, int levels) {
        Weapon w = slot >= 0 && slot < slots.length ? slots[slot] : null;
        if (w != null) w.level = Math.min(Config.WEAPON_MAX_LEVEL, w.level + levels);
    }

    public void update(float dt) {
        for (Weapon w : slots) if (w != null && w.recoil > 0) w.recoil = Math.max(0, w.recoil - dt * 8f);
        if (switchTimer > 0) { switchTimer -= dt; return; }
        Weapon w = slots[active];
        if (w == null) return;
        Player p = g.player;
        switch (w.type.kind) {
            case WeaponType.K_ORBIT: updateBlades(w, p, dt); break;
            case WeaponType.K_CHAIN: updateChain(w, p, dt); break;
            case WeaponType.K_AREA: updatePulse(w, p, dt); break;
            case WeaponType.K_BEAM: updateBeam(w, p, dt); break;
            case WeaponType.K_MINE: updateMines(w, p, dt); break;
            default: updateGun(w, p, dt);
        }
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

    private float aim(Player p, Enemy t) {
        float a = MathUtil.atan2(t.y - p.y, t.x - p.x);
        p.facing = a;
        return a;
    }

    private float shotDamage(Weapon w, Player p) {
        w.shots++;
        float d = w.damage(p);
        if (w.trait == Loot.T_OVERCHARGE && w.shots % 4 == 0) d *= 3f;
        return d;
    }

    private void updateGun(Weapon w, Player p, float dt) {
        w.timer -= dt;
        if (w.timer > 0) return;
        float range = w.range(p);
        Enemy target = pickTarget(p, range);
        if (target == null) { w.timer = 0.06f; return; }
        w.timer = w.cooldown(p);
        float base = aim(p, target);
        float dmg = shotDamage(w, p), spd = w.projSpeed(p);
        int count = w.count(p);
        WeaponType t = w.type;
        float rad = t.special == WeaponType.SP_FLAME ? 10f : (t.id == Config.W_ROCKET || t.special == WeaponType.SP_HOMING ? 9f
                : (t.special == WeaponType.SP_BOOMERANG || t.special == WeaponType.SP_RICOCHET ? 11f : (t.id == Config.W_MINIGUN ? 4.5f : 6.5f)));
        float explode = w.explodeRadius(p);
        float muzzle = p.radius + 22f;
        float sx = p.x + MathUtil.cos(base) * muzzle, sy = p.y + MathUtil.sin(base) * muzzle;
        int col = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : t.color;
        switch (t.kind) {
            case WeaponType.K_PARALLEL: {
                float nx = -MathUtil.sin(base), ny = MathUtil.cos(base);
                for (int k = 0; k < count; k++) {
                    float off = (k - (count - 1) / 2f) * 14f;
                    fire(w, sx + nx * off, sy + ny * off, base + MathUtil.rand(-t.inaccuracy, t.inaccuracy), spd, dmg, explode, range, col, rad);
                }
                break;
            }
            case WeaponType.K_SPREAD: {
                float step = (float) Math.toRadians(t.spreadDeg);
                for (int k = 0; k < count; k++) {
                    float a = base + (k - (count - 1) / 2f) * step + MathUtil.rand(-t.inaccuracy, t.inaccuracy);
                    fire(w, sx, sy, a, spd * MathUtil.rand(0.92f, 1.08f), dmg, explode, range, col, rad);
                }
                break;
            }
            default: {
                float step = (float) Math.toRadians(t.special == WeaponType.SP_HOMING ? 28 : 7);
                for (int k = 0; k < count; k++) {
                    float a = base + (k - (count - 1) / 2f) * step + MathUtil.rand(-t.inaccuracy, t.inaccuracy);
                    float s = t.special == WeaponType.SP_FLAME ? spd * MathUtil.rand(0.75f, 1.15f) : spd;
                    fire(w, sx, sy, a, s, dmg, explode, range, col, rad);
                }
            }
        }
        w.recoil = 1f;
        if (t.special != WeaponType.SP_FLAME) g.fx.spawn(sx, sy, MathUtil.cos(base) * 80, MathUtil.sin(base) * 80, 0.12f, 7f, col, ParticleSystem.DOT);
        g.audio.play(t.special == WeaponType.SP_RAIL ? Sfx.ZAP : Sfx.SHOOT);
    }

    private void fire(Weapon w, float x, float y, float a, float spd, float dmg, float explode, float range, int col, float rad) {
        WeaponType t = w.type;
        g.combat.fireProjectile(x, y, a, spd, dmg, w.critBonus(), t.special == WeaponType.SP_RICOCHET ? 0 : w.pierce(), explode, range,
                col, t.id, rad, t.special, w.element, t.special == WeaponType.SP_RICOCHET ? w.pierce() : 0, w.trait == Loot.T_FRACTAL, w);
    }

    /** Current world positions of orbiting blades are derived from angle + index. */
    public float bladeRadius(Weapon w, Player p) { return (w.type.range + p.radius - Config.PLAYER_RADIUS) * (1f + 0.06f * (w.level - 1)) * (0.6f + 0.4f * p.rangeMult) * w.rangeMod; }

    private void updateBlades(Weapon w, Player p, float dt) {
        w.angle += w.type.projSpeed * (1f + 0.05f * (w.level - 1)) * w.rateMod * dt;
        int n = w.count(p);
        float orbit = bladeRadius(w, p);
        float dmg = w.damage(p);
        float cd = w.cooldown(p);
        EntityList<Enemy> list = g.enemies.list;
        for (int b = 0; b < n; b++) {
            float a = w.angle + b * MathUtil.TAU / n;
            float bx = p.x + MathUtil.cos(a) * orbit, by = p.y + MathUtil.sin(a) * orbit;
            g.combat.clearBullets(bx, by, BLADE_HIT_RADIUS);
            int c = g.grid.query(list, bx, by, BLADE_HIT_RADIUS, hits);
            for (int k = 0; k < c; k++) {
                Enemy e = list.get(hits[k]);
                if (e.bladeHitTimer > 0) continue;
                e.bladeHitTimer = cd;
                float tx = -MathUtil.sin(a), ty = MathUtil.cos(a);
                g.combat.damageEnemy(e, dmg, w.critBonus(), tx * 180f, ty * 180f, w);
            }
        }
        Enemy t = g.enemies.nearest(p.x, p.y, 400);
        if (t != null) aim(p, t);
    }

    private void updateChain(Weapon w, Player p, float dt) {
        w.timer -= dt;
        if (w.timer > 0) return;
        Enemy first = pickTarget(p, w.range(p));
        if (first == null) { w.timer = 0.1f; return; }
        w.timer = w.cooldown(p);
        aim(p, first);
        w.recoil = 1f;
        chainId++;
        int jumps = w.count(p);
        float dmg = shotDamage(w, p);
        float fx = p.x + MathUtil.cos(p.facing) * (p.radius + 26), fy = p.y + MathUtil.sin(p.facing) * (p.radius + 26);
        Enemy cur = first;
        EntityList<Enemy> list = g.enemies.list;
        float jumpRange = Config.CHAIN_JUMP * (0.6f + 0.4f * p.weaponPower);
        int col = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color;
        for (int j = 0; j < jumps && cur != null; j++) {
            g.fx.bolt(fx, fy, cur.x, cur.y, col);
            cur.lastChainId = chainId;
            fx = cur.x; fy = cur.y;
            g.combat.damageEnemy(cur, dmg, w.critBonus(), 0, 0, w);
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
        float r = w.areaRadius(p) + (p.radius - Config.PLAYER_RADIUS);
        Enemy n = g.enemies.nearest(p.x, p.y, r);
        if (n == null) { w.timer = 0.15f; return; }
        aim(p, n);
        w.timer = w.cooldown(p);
        w.recoil = 1f;
        g.combat.areaDamage(p.x, p.y, r, shotDamage(w, p), 160f, w);
        g.fx.ring(p.x, p.y, r, w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color, 0.4f);
        g.audio.play(Sfx.PULSE);
    }

    private void updateBeam(Weapon w, Player p, float dt) {
        float range = w.range(p);
        if (w.beamTime > 0) {
            w.beamTime -= dt;
            Enemy t = pickTarget(p, range);
            if (t != null) {
                float want = MathUtil.atan2(t.y - p.y, t.x - p.x);
                float diff = want - w.angle;
                while (diff > MathUtil.PI) diff -= MathUtil.TAU;
                while (diff < -MathUtil.PI) diff += MathUtil.TAU;
                w.angle += MathUtil.clamp(diff, -6f * dt, 6f * dt);
            }
            p.facing = w.angle;
            w.recoil = 0.5f;
            w.beamTick -= dt;
            if (w.beamTick <= 0) {
                w.beamTick = 0.1f;
                float dmg = w.damage(p) * 0.1f;
                float x0 = p.x, y0 = p.y, dx = MathUtil.cos(w.angle), dy = MathUtil.sin(w.angle);
                g.combat.clearBulletsOnLine(x0, y0, dx, dy, range, Config.LASER_WIDTH);
                EntityList<Enemy> list = g.enemies.list;
                for (int i = 0; i < list.size; i++) {
                    Enemy e = list.get(i);
                    if (!e.alive) continue;
                    float ex = e.x - x0, ey = e.y - y0;
                    float along = ex * dx + ey * dy;
                    if (along < 0 || along > range + e.radius) continue;
                    float perp = Math.abs(ex * dy - ey * dx);
                    if (perp < Config.LASER_WIDTH + e.radius) g.combat.damageEnemy(e, dmg, w.critBonus(), dx * 40f, dy * 40f, w);
                }
            }
            if (w.beamTime <= 0) w.timer = w.cooldown(p);
            return;
        }
        w.timer -= dt;
        if (w.timer > 0) return;
        Enemy t = pickTarget(p, range);
        if (t == null) { w.timer = 0.1f; return; }
        w.angle = aim(p, t);
        w.beamTime = Config.LASER_DURATION;
        w.beamTick = 0;
        g.audio.play(Sfx.NOVA);
    }

    private void updateMines(Weapon w, Player p, float dt) {
        w.timer -= dt;
        if (w.timer > 0) return;
        w.timer = w.cooldown(p);
        w.recoil = 1f;
        float a = p.facing + MathUtil.PI + MathUtil.rand(-0.6f, 0.6f);
        float x = p.x + MathUtil.cos(a) * (p.radius + 14), y = p.y + MathUtil.sin(a) * (p.radius + 14);
        int col = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color;
        g.combat.fireProjectile(x, y, 0, 0, shotDamage(w, p), w.critBonus(), 0, w.explodeRadius(p), 0, col, w.type.id, 12f,
                WeaponType.SP_MINE, w.element, 0, false, w);
        g.audio.play(Sfx.CLICK);
    }

    /** Laser beam end point for rendering (or null-equivalent when not firing). */
    public boolean beamActive() { Weapon w = slots[active]; return w != null && w.type.kind == WeaponType.K_BEAM && w.beamTime > 0; }
}
