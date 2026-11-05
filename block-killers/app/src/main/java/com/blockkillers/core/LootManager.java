package com.blockkillers.core;

/** Rolls randomized guns, drops them on the ground (enemies, bosses, chests) and handles pickup / equip / scrap. */
public final class LootManager {
    public static final float PICKUP_RANGE = 95f, GROUND_LIFE = 60f;
    private final Game g;
    public final EntityList<GroundItem> items = new EntityList<>(30, GroundItem::new);
    /** Item the player is standing on (shows the PICK UP button). */
    public GroundItem nearby;
    /** Item being inspected on the loot screen. */
    public GroundItem viewing;
    public int legendariesThisRun, chestsThisRun, dropsThisRun;

    public LootManager(Game g) { this.g = g; }

    public void reset() { items.clear(); nearby = viewing = null; legendariesThisRun = chestsThisRun = dropsThisRun = 0; }

    public int itemLevel() { return 1 + (int) (g.time / Config.ITEM_LEVEL_SECONDS); }

    /** Weighted rarity roll; luck rises with time. */
    public int rollRarity(int min, float boost) {
        float luck = g.time / 600f;
        float total = 0;
        float[] w = new float[Loot.TIERS];
        for (int i = min; i < Loot.TIERS; i++) {
            w[i] = Loot.DROP_WEIGHT[i] * (1f + luck * i * 0.35f) * (i >= Loot.EPIC ? boost * g.shop.luck() : 1f);
            total += w[i];
        }
        float r = MathUtil.rand() * total;
        for (int i = min; i < Loot.TIERS; i++) { r -= w[i]; if (r <= 0) return i; }
        return Loot.TIERS - 1;
    }

    public Weapon roll(int typeId, int rarity, int ilvl) {
        WeaponType t = Config.WEAPONS[typeId];
        Weapon w = new Weapon(t);
        w.rarity = rarity; w.itemLevel = ilvl;
        float sc = Loot.PART_SCALE[rarity];
        int parts = Loot.PARTS[rarity];
        boolean[] used = new boolean[Loot.PART_COUNT];
        for (int i = 0; i < parts; i++) {
            int p = -1;
            for (int tries = 0; tries < 12 && p < 0; tries++) {
                int c = MathUtil.randInt(Loot.PART_COUNT);
                if (used[c]) continue;
                if ((c == Loot.P_PROJ || c == Loot.P_PIERCE || c == Loot.P_SPEED) && !t.usesProjectiles()) continue;
                p = c;
            }
            if (p < 0) continue;
            used[p] = true;
            if (w.prefix < 0) w.prefix = p;
            switch (p) {
                case Loot.P_DAMAGE: w.dmgMod += MathUtil.rand(0.12f, 0.22f) * sc; break;
                case Loot.P_RATE: w.rateMod += MathUtil.rand(0.10f, 0.20f) * sc; break;
                case Loot.P_CRIT: w.critMod += MathUtil.rand(0.05f, 0.10f) * sc; break;
                case Loot.P_SPEED: w.speedMod += MathUtil.rand(0.15f, 0.30f) * sc; break;
                case Loot.P_RANGE: w.rangeMod += MathUtil.rand(0.10f, 0.20f) * sc; break;
                case Loot.P_PROJ: w.projMod += 1; break;
                case Loot.P_PIERCE: w.pierceMod += 1 + (rarity >= Loot.EPIC ? 1 : 0); break;
            }
        }
        // manufacturer + parts: every gun is assembled from random pieces
        boolean proj = t.usesProjectiles();
        w.maker = MathUtil.randInt(Loot.MAKERS);
        w.barrel = MathUtil.randInt(Loot.BARREL_KEYS.length);
        w.grip = MathUtil.randInt(Loot.GRIP_KEYS.length);
        w.sight = MathUtil.randInt(Loot.SIGHT_KEYS.length);
        w.accessory = MathUtil.randInt(Loot.ACC_KEYS.length);
        float elementBonus = 0;
        switch (w.maker) {
            case Loot.MK_KRUGG: w.dmgMod += 0.25f; w.rateMod -= 0.15f; break;
            case Loot.MK_VOLTIS: elementBonus += 0.30f; break;
            case Loot.MK_HALCYON: w.critMod += 0.08f; w.speedMod += 0.15f; break;
            case Loot.MK_FERRO: w.rateMod += 0.20f; w.dmgMod -= 0.08f; break;
            case Loot.MK_IRONJAW: if (proj) w.pierceMod += 1; w.rangeMod += 0.10f; break;
            case Loot.MK_NIMBUS: if (proj && MathUtil.rand() < 0.5f) w.projMod += 1; else w.dmgMod += 0.08f; break;
        }
        switch (w.barrel) {
            case 1: w.rangeMod += 0.15f; w.speedMod += 0.15f; w.rateMod -= 0.05f; break;
            case 2: w.dmgMod += 0.15f; w.rateMod -= 0.08f; break;
            case 3: if (proj) { w.projMod += 1; w.dmgMod -= 0.15f; } else w.dmgMod += 0.08f; break;
        }
        switch (w.grip) {
            case 1: w.rateMod += 0.12f; break;
            case 2: w.critMod += 0.05f; break;
            case 3: w.dmgMod += 0.08f; w.rateMod -= 0.04f; break;
        }
        switch (w.sight) {
            case 1: w.critMod += 0.03f; w.speedMod += 0.05f; break;
            case 2: w.critMod += 0.08f; w.rangeMod += 0.10f; w.rateMod -= 0.04f; break;
            case 3: w.rateMod += 0.06f; break;
        }
        switch (w.accessory) {
            case 1: w.dmgMod += 0.10f; break;
            case 2: w.rateMod += 0.15f; break;
            case 3: w.speedMod += 0.20f; break;
            case 4: elementBonus += 0.30f; break;
        }
        if (t.element == Loot.E_NONE && MathUtil.rand() < Loot.ELEMENT_CHANCE[rarity] + (rarity > 0 ? elementBonus : 0)) w.element = 1 + MathUtil.randInt(Loot.ELEMENTS);
        if (rarity == Loot.LEGENDARY) {
            int[] traits = t.usesProjectiles() ? new int[]{Loot.T_VAMPIRIC, Loot.T_VOLATILE, Loot.T_OVERCHARGE, Loot.T_FRACTAL}
                    : new int[]{Loot.T_VAMPIRIC, Loot.T_VOLATILE, Loot.T_OVERCHARGE};
            w.trait = traits[MathUtil.randInt(traits.length)];
        }
        return w;
    }

    public Weapon rollRandom(int minRarity, float boost) {
        return roll(MathUtil.randInt(Config.WEAPONS.length), rollRarity(minRarity, boost), itemLevel());
    }

    public void drop(float x, float y, Weapon w) {
        if (items.full()) {
            // replace the item closest to despawning (prefer low rarity)
            int worst = 0; float ws = Float.MAX_VALUE;
            for (int i = 0; i < items.size; i++) {
                GroundItem it = items.get(i);
                float s = it.life + it.weapon.rarity * 100;
                if (s < ws) { ws = s; worst = i; }
            }
            if (items.get(worst) == nearby) nearby = null;
            items.removeAt(worst);
        }
        GroundItem it = items.obtain();
        float a = MathUtil.rand(0, MathUtil.TAU);
        it.x = x; it.y = y; it.vx = MathUtil.cos(a) * 160; it.vy = MathUtil.sin(a) * 160; it.life = GROUND_LIFE; it.t = 0; it.weapon = w;
        dropsThisRun++;
        g.fx.ring(x, y, 60 + w.rarity * 20, Loot.COLORS[w.rarity], 0.5f);
        g.audio.play(Sfx.LOOT);
        if (w.rarity == Loot.LEGENDARY) {
            legendariesThisRun++;
            g.ui.announce(g.strings.get("legendary_drop"), w.name(g.strings));
            g.platformVibrate(120);
        }
    }

    /** Called for every regular kill. */
    public void onEnemyKilled(Enemy e) {
        float chance = 0.009f;
        switch (e.type.id) {
            case 9: chance = 0.12f; break;          // tank
            case 6: case 7: case 8: chance = 0.03f; break;
            case 5: chance = 0.02f; break;
        }
        chance *= (1f + e.tier * 2.5f) * g.shop.luck();
        if (MathUtil.rand() < chance) drop(e.x, e.y, rollRandom(e.tier >= 2 ? Loot.UNCOMMON : Loot.COMMON, 1f));
    }

    public void onBossKilled(Enemy e) {
        int n = 3;
        for (int i = 0; i < n; i++) drop(e.x, e.y, rollRandom(Loot.EPIC, i == 0 ? 2.5f : 1f));
    }

    public void update(float dt) {
        Player p = g.player;
        nearby = null;
        float range = PICKUP_RANGE * g.shop.magnet() * p.radius / Config.PLAYER_RADIUS;
        float best = range * range;
        for (int i = items.size - 1; i >= 0; i--) {
            GroundItem it = items.get(i);
            it.t += dt;
            it.life -= dt;
            if (it.life <= 0) { items.removeAt(i); continue; }
            it.x += it.vx * dt; it.y += it.vy * dt;
            float d = Math.max(0f, 1f - 5f * dt);
            it.vx *= d; it.vy *= d;
            float d2 = MathUtil.dist2(it.x, it.y, p.x, p.y);
            if (d2 < best) { best = d2; nearby = it; }
        }
        // chests open on touch
        WorldMap m = g.map;
        for (int i = 0; i < m.chestCount; i++) {
            if (m.opened[i]) continue;
            float rr = p.radius + 34;
            if (MathUtil.dist2(m.cx[i], m.cy[i], p.x, p.y) < rr * rr) openChest(i);
        }
    }

    public void openChest(int i) {
        WorldMap m = g.map;
        m.opened[i] = true;
        chestsThisRun++;
        g.addCoins(m.golden[i] ? 40 : 15);
        int n = m.golden[i] ? 2 : 1;
        for (int k = 0; k < n; k++) drop(m.cx[i], m.cy[i], rollRandom(m.golden[i] ? Loot.RARE : Loot.UNCOMMON, m.golden[i] ? 3f : 1.6f));
        g.fx.burst(m.cx[i], m.cy[i], 0xFFFFD740, 24, 380, 7);
        g.audio.play(Sfx.CHEST);
        g.camera.addShake(0.15f);
    }

    // ---- loot screen actions ----
    public void equipInto(int slot) {
        if (viewing == null) return;
        g.weapons.place(slot, viewing.weapon);
        remove(viewing);
        g.audio.play(Sfx.SELECT);
    }

    public void scrap() {
        if (viewing == null) return;
        g.xp.addXp(Loot.SCRAP_XP[viewing.weapon.rarity] * (1 + itemLevel() / 4));
        g.fx.burst(viewing.x, viewing.y, Loot.COLORS[viewing.weapon.rarity], 16, 260, 5);
        remove(viewing);
        g.audio.play(Sfx.XP);
    }

    private void remove(GroundItem it) {
        for (int i = 0; i < items.size; i++) if (items.get(i) == it) { items.removeAt(i); break; }
        if (nearby == it) nearby = null;
        viewing = null;
    }
}
