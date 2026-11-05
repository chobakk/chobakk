package com.blockkillers.core;

/** Player state and all upgradable attributes. */
public final class Player {
    public float x, y, vx, vy;
    public float radius = Config.PLAYER_RADIUS;
    public float hp, maxHp;
    public float speedMult, damageMult, attackSpeedMult, rangeMult, projSpeedMult, xpMult, weaponPower;
    public float critChance, critMult, armor, regen;
    public int extraProjectiles;
    public int level, xp;
    public float invuln, hurtFlash, facing;
    /** First-person view heading (3D mode) and small vertical look offset. */
    public float view, viewPitch;
    public boolean alive;
    /** Visible body features gained from upgrades (index = UpgradeManager stat id). */
    public final int[] features = new int[UpgradeManager.STAT_COUNT];
    public int upgradesTaken;
    public float baseRadius = Config.PLAYER_RADIUS, morph;

    public void reset(PermanentBonus pb) {
        x = 0; y = 0; vx = 0; vy = 0;
        maxHp = Config.PLAYER_MAX_HP + pb.maxHp;
        hp = maxHp;
        speedMult = 1f + pb.speed;
        damageMult = 1f + pb.damage;
        attackSpeedMult = 1f + pb.attackSpeed;
        rangeMult = 1f; projSpeedMult = 1f; weaponPower = 1f;
        xpMult = 1f + pb.xp;
        critChance = Config.PLAYER_CRIT + pb.crit;
        critMult = Config.PLAYER_CRIT_MULT;
        armor = pb.armor; regen = 0f; extraProjectiles = 0;
        level = 1; xp = 0; invuln = 0; hurtFlash = 0; facing = 0; view = 0; viewPitch = 0;
        alive = true;
        for (int i = 0; i < features.length; i++) features[i] = 0;
        upgradesTaken = 0; morph = 0; radius = baseRadius;
    }
    public float speed() { return Config.PLAYER_SPEED * speedMult; }

    /** Permanent bonuses granted by achievements; applied at the start of each run. */
    public static final class PermanentBonus {
        public float maxHp, speed, damage, attackSpeed, xp, crit, armor;
        public void clear() { maxHp = speed = damage = attackSpeed = xp = crit = armor = 0; }
    }
}
