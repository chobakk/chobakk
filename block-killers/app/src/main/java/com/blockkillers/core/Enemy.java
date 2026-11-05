package com.blockkillers.core;

/** Pooled enemy instance (also used for bosses). */
public final class Enemy {
    /** Applies a fusion tier: bigger, tougher, more XP, combined silhouette. */
    public void fuse(int tier, int shapeA, int shapeB, int color) {
        this.tier = tier; fuseShapeA = shapeA; fuseShapeB = shapeB; fuseColor = color;
        radius *= Config.TIER_SIZE[tier]; maxHp *= Config.TIER_HP[tier]; hp = maxHp; damage *= Config.TIER_DMG[tier];
        xp = Math.round(xp * Config.TIER_XP[tier]); score = Math.round(score * Config.TIER_XP[tier]);
        speed *= tier == 2 ? 0.9f : 1f;
        armored = true;
        if (tier == 2) { maxShield = shield = maxHp * 0.5f; } else if (maxShield > 0) { maxShield = shield = maxHp * 0.6f; }
    }

    public EnemyType type;
    public boolean alive, boss;
    public int bossKind;
    public float x, y, vx, vy, kx, ky;      // kx,ky = knockback velocity
    public float radius, hp, maxHp, damage, speed;
    public float attackTimer, flash, rot, rotSpeed, t;
    public int state;                       // behaviour state machine
    public float stateTimer, dirX, dirY, wobble;
    public float spawnAnim;                 // 0..1 grow-in animation
    public float bladeHitTimer;             // per-enemy cooldown for orbiting blades
    public int lastChainId;                 // to avoid hitting twice in one chain
    public int xp, score;
    // fusion tier (harder enemies are mashed-up shapes)
    public int tier, fuseShapeA, fuseShapeB, fuseColor;
    // elemental status effects
    public float burnT, burnDps, poisonT, poisonDps, slowT, slowAmt, slagT;
    /** Shield must be broken first (blue ring); armored enemies have steel plating. */
    public float shield, maxShield, shieldFlash;
    public boolean armored;
    public Weapon lastHitBy;
    public Hero lastHitHero;

    public void init(EnemyType t, float x, float y, float hpMult, float dmgMult, float spdMult) {
        type = t; alive = true; boss = false; bossKind = 0;
        this.x = x; this.y = y; vx = vy = kx = ky = 0;
        radius = t.radius; maxHp = hp = t.hp * hpMult; damage = t.damage * dmgMult; speed = t.speed * spdMult;
        attackTimer = 0; flash = 0; rot = MathUtil.rand(0, MathUtil.TAU); rotSpeed = MathUtil.rand(-1.5f, 1.5f);
        this.t = MathUtil.rand(0, 10); state = 0; stateTimer = MathUtil.rand(0.5f, 2f); dirX = dirY = 0;
        wobble = MathUtil.rand(0, MathUtil.TAU); spawnAnim = 0; bladeHitTimer = 0; lastChainId = -1;
        xp = t.xp; score = t.score;
        tier = 0; fuseShapeA = fuseShapeB = 0; fuseColor = t.color;
        burnT = burnDps = poisonT = poisonDps = slowT = slowAmt = slagT = 0; lastHitBy = null; lastHitHero = null;
        armored = t.id == 6 || t.id == 9;
        maxShield = shield = (t.id == 3 || t.id == 8 || t.id == 4) ? maxHp * 0.6f : 0; shieldFlash = 0;
    }
}
