package com.blockkillers.core;

/** Configurable weapon (gun) definition. Instances live in {@link Config#WEAPONS}. */
public final class WeaponType {
    public static final int K_PROJECTILE = 0, K_PARALLEL = 1, K_SPREAD = 2, K_ORBIT = 3, K_CHAIN = 4, K_AREA = 5, K_BEAM = 6, K_MINE = 7;
    /** Special projectile behaviours. */
    public static final int SP_NONE = 0, SP_HOMING = 1, SP_BOOMERANG = 2, SP_RICOCHET = 3, SP_FLAME = 4, SP_RAIL = 5, SP_MINE = 6;
    /** Gun silhouettes drawn in the player's hand. */
    public static final int G_PISTOL = 0, G_DOUBLE = 1, G_WIDE = 2, G_LONG = 3, G_TUBE = 4, G_HILT = 5, G_ROD = 6, G_DISH = 7,
            G_MINIGUN = 8, G_NOZZLE = 9, G_DISC = 10, G_EMITTER = 11, G_BOX = 12;

    public final int id;
    public final String nameKey, descKey;
    public final int kind, special, element, gun;
    public final float damage, cooldown, range, projSpeed;
    public final int count, pierce;
    public final float explodeRadius, spreadDeg, inaccuracy, critBonus;
    public final int color;

    public WeaponType(int id, String nameKey, int kind, int special, int element, int gun, float damage, float cooldown, float range,
                      float projSpeed, int count, int pierce, float explodeRadius, float spreadDeg, float inaccuracy, float critBonus, int color) {
        this.id = id; this.nameKey = nameKey; this.descKey = "wd" + nameKey.substring(1); this.kind = kind; this.special = special;
        this.element = element; this.gun = gun; this.damage = damage; this.cooldown = cooldown; this.range = range;
        this.projSpeed = projSpeed; this.count = count; this.pierce = pierce; this.explodeRadius = explodeRadius;
        this.spreadDeg = spreadDeg; this.inaccuracy = inaccuracy; this.critBonus = critBonus; this.color = color;
    }

    public boolean usesProjectiles() { return kind == K_PROJECTILE || kind == K_PARALLEL || kind == K_SPREAD; }
}
