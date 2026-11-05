package com.blockkillers.core;

/** A weapon owned by the player during a run. */
public final class Weapon {
    public final WeaponType type;
    public int level = 1;
    public float timer;
    public float angle;   // for orbiting blades

    public Weapon(WeaponType type) { this.type = type; }

    public float damage(Player p) {
        float d = type.damage * (1f + 0.25f * (level - 1)) * p.damageMult;
        if (type.id != Config.W_BLASTER) d *= p.weaponPower;
        return d;
    }
    public float cooldown(Player p) { return type.cooldown * (float) Math.pow(0.92, level - 1) / p.attackSpeedMult; }
    public float range(Player p) { return type.range * p.rangeMult; }
    public float projSpeed(Player p) { return type.projSpeed * p.projSpeedMult; }
    public int count(Player p) {
        switch (type.kind) {
            case WeaponType.K_ORBIT: return type.count + (level - 1) / 2 + Math.min(2, p.extraProjectiles / 2);
            case WeaponType.K_CHAIN: return type.count + (level - 1);
            case WeaponType.K_AREA: return 1;
            case WeaponType.K_SPREAD: return type.count + (level - 1) / 2 + p.extraProjectiles;
            default: return type.count + (level >= 4 ? 1 : 0) + p.extraProjectiles;
        }
    }
    public int pierce() { return type.pierce + (type.id == Config.W_PIERCER ? level - 1 : 0); }
    public float explodeRadius(Player p) { return type.explodeRadius * (1f + 0.08f * (level - 1)) * (0.5f + 0.5f * p.weaponPower); }
    public float areaRadius(Player p) { return type.range * (1f + 0.1f * (level - 1)) * (0.5f + 0.5f * p.weaponPower) * p.rangeMult; }
}
