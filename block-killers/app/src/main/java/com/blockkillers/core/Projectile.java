package com.blockkillers.core;

/** Pooled player projectile (bullets, rockets, flames, discs, homing missiles, mines). */
public final class Projectile {
    public float x, y, vx, vy, life, radius, damage, critChance, explodeRadius, age, speed;
    public int pierceLeft, color, weaponId, special, element, bounces;
    public boolean alive, returning, canSplit;
    public Weapon src;
    /** Enemies already hit (piercing projectiles must not hit the same enemy twice). */
    public final Enemy[] hits = new Enemy[12];
    public int hitCount;
}
