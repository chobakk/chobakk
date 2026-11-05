package com.blockkillers.core;

/** A gun instance: base type + loot rarity, item level, rolled parts, element and (legendary) trait. */
public final class Weapon {
    public final WeaponType type;
    public int level = 1;           // raised by level-up cards
    public int rarity = Loot.COMMON;
    public int itemLevel = 1;
    public int element;
    public int trait = Loot.T_NONE;
    public int prefix = -1;         // index into Loot.PART_PREFIX, or -1
    public float dmgMod = 1f, rateMod = 1f, critMod = 0f, speedMod = 1f, rangeMod = 1f;
    public int projMod, pierceMod;
    /** Manufacturer (-1 = stock starter gun) and rolled parts. */
    public int maker = -1, barrel, grip, sight, accessory;
    // runtime state
    public float timer, angle, beamTime, beamTick, recoil;
    public int shots, kills;
    /** Hero carrying this gun (for crits, kill credit and PvP). */
    public Hero owner;

    public Weapon(WeaponType type) { this.type = type; this.element = type.element; }

    private float baseDamage() {
        return type.damage * Loot.DAMAGE[rarity] * (1f + Config.ITEM_LEVEL_DAMAGE * (itemLevel - 1)) * (1f + 0.25f * (level - 1)) * dmgMod;
    }
    public float damage(Player p) {
        float d = baseDamage() * p.damageMult;
        if (type.id != Config.W_BLASTER) d *= p.weaponPower;
        return d;
    }
    public float cooldown(Player p) { return type.cooldown * (float) Math.pow(0.92, level - 1) / (p.attackSpeedMult * rateMod); }
    public float range(Player p) { return type.range * p.rangeMult * rangeMod; }
    public float projSpeed(Player p) { return type.projSpeed * p.projSpeedMult * speedMod; }
    public float critBonus() { return type.critBonus + critMod; }
    public int count(Player p) {
        switch (type.kind) {
            case WeaponType.K_ORBIT: return type.count + (level - 1) / 2 + Math.min(2, p.extraProjectiles / 2) + projMod;
            case WeaponType.K_CHAIN: return type.count + (level - 1) + projMod;
            case WeaponType.K_AREA: case WeaponType.K_BEAM: case WeaponType.K_MINE: return 1;
            case WeaponType.K_SPREAD: return type.count + (level - 1) / 2 + p.extraProjectiles + projMod;
            default: return type.count + (level >= 4 ? 1 : 0) + p.extraProjectiles + projMod;
        }
    }
    public int pierce() { return type.pierce + (type.id == Config.W_PIERCER ? level - 1 : 0) + pierceMod; }
    public float explodeRadius(Player p) { return type.explodeRadius * (1f + 0.08f * (level - 1)) * (0.5f + 0.5f * p.weaponPower); }
    public float areaRadius(Player p) { return type.range * (1f + 0.1f * (level - 1)) * (0.5f + 0.5f * p.weaponPower) * p.rangeMult * rangeMod; }
    public int color() { return Loot.COLORS[rarity]; }

    /** Approximate damage per second, used for loot comparison (shown to the player). */
    public float dps(Player p) {
        float crit = 1f + Math.min(1f, p.critChance + critBonus()) * (p.critMult - 1f);
        float d = damage(p) * crit;
        switch (type.kind) {
            case WeaponType.K_BEAM: return d * Config.LASER_DURATION / (Config.LASER_DURATION + cooldown(p));
            case WeaponType.K_ORBIT: return d * count(p) / Math.max(0.15f, cooldown(p)) * 0.5f;
            case WeaponType.K_CHAIN: return d * count(p) * 0.8f / cooldown(p);
            case WeaponType.K_AREA: return d * 3f / cooldown(p);
            case WeaponType.K_MINE: return d * 2f / cooldown(p);
            default: {
                float hits = count(p) * (type.kind == WeaponType.K_SPREAD ? 0.7f : 1f);
                if (type.explodeRadius > 0) hits *= 1.8f;
                if (pierce() > 0) hits *= 1f + Math.min(2f, pierce() * 0.25f);
                if (type.special == WeaponType.SP_RICOCHET) hits *= 1.6f;
                if (trait == Loot.T_OVERCHARGE) hits *= 1.5f;
                return d * hits / cooldown(p);
            }
        }
    }

    public String name(Strings s) {
        String base = s.get(type.nameKey);
        String px = null;
        if (trait != Loot.T_NONE) px = s.get(Loot.TRAIT_KEYS[trait]);
        else if (element != Loot.E_NONE && element != type.element) px = s.get(Loot.ELEMENT_PREFIX[element]);
        else if (prefix >= 0) px = s.get(Loot.PART_PREFIX[prefix]);
        String mk = maker >= 0 ? s.get(Loot.MAKER_KEYS[maker]) : "";
        String out = px == null ? base : s.get("name_fmt").replace("{p}", px).replace("{w}", base);
        return mk.isEmpty() ? out : s.get("maker_fmt").replace("{m}", mk).replace("{n}", out);
    }
}
