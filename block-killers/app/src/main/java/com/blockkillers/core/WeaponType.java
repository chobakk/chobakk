package com.blockkillers.core;

/** Configurable weapon definition. Instances live in {@link Config#WEAPONS}. */
public final class WeaponType {
    public static final int K_PROJECTILE = 0, K_PARALLEL = 1, K_SPREAD = 2, K_ORBIT = 3, K_CHAIN = 4, K_AREA = 5;

    public final int id;
    public final String nameKey, descKey;
    public final int kind;
    public final float damage, cooldown, range, projSpeed;
    public final int count, pierce;
    public final float explodeRadius, spreadDeg, critBonus;
    public final int color;

    public WeaponType(int id, String nameKey, String descKey, int kind, float damage, float cooldown, float range,
                      float projSpeed, int count, int pierce, float explodeRadius, float spreadDeg, float critBonus, int color) {
        this.id = id; this.nameKey = nameKey; this.descKey = descKey; this.kind = kind; this.damage = damage;
        this.cooldown = cooldown; this.range = range; this.projSpeed = projSpeed; this.count = count; this.pierce = pierce;
        this.explodeRadius = explodeRadius; this.spreadDeg = spreadDeg; this.critBonus = critBonus; this.color = color;
    }
}
