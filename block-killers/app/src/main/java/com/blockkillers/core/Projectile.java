package com.blockkillers.core;

/** Pooled player projectile. */
public final class Projectile {
    public float x, y, vx, vy, life, radius, damage, critChance, explodeRadius;
    public int pierceLeft, color, weaponId;
    public boolean alive;
    /** Last enemies hit (piercing projectiles must not hit the same enemy twice). */
    public final Enemy[] hits = new Enemy[12];
    public int hitCount;
}
