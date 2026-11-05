package com.blockkillers.core;

/** Configurable definition of one enemy class. Instances live in {@link Config#ENEMIES}. */
public final class EnemyType {
    public static final int B_CHASE = 0, B_ZIGZAG = 1, B_CHARGE = 2, B_SHOOT = 3, B_SPLIT = 4, B_SWARM = 5;

    public final int id;
    public final String nameKey;
    public final int shape;
    public final float radius, hp, damage, speed, attackCooldown;
    public final int xp, score;
    public final float spawnWeight, unlockTime;
    public final int color, behavior;
    /** Group size for swarm spawns (1 = single). */
    public final int groupSize;

    public EnemyType(int id, String nameKey, int shape, float radius, float hp, float damage, float speed,
                     float attackCooldown, int xp, float spawnWeight, float unlockTime, int color, int behavior, int groupSize) {
        this.id = id; this.nameKey = nameKey; this.shape = shape; this.radius = radius; this.hp = hp;
        this.damage = damage; this.speed = speed; this.attackCooldown = attackCooldown; this.xp = xp;
        this.score = xp * 10; this.spawnWeight = spawnWeight; this.unlockTime = unlockTime;
        this.color = color; this.behavior = behavior; this.groupSize = groupSize;
    }
}
