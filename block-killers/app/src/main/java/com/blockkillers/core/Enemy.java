package com.blockkillers.core;

/** Pooled enemy instance (also used for bosses). */
public final class Enemy {
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

    public void init(EnemyType t, float x, float y, float hpMult, float dmgMult, float spdMult) {
        type = t; alive = true; boss = false; bossKind = 0;
        this.x = x; this.y = y; vx = vy = kx = ky = 0;
        radius = t.radius; maxHp = hp = t.hp * hpMult; damage = t.damage * dmgMult; speed = t.speed * spdMult;
        attackTimer = 0; flash = 0; rot = MathUtil.rand(0, MathUtil.TAU); rotSpeed = MathUtil.rand(-1.5f, 1.5f);
        this.t = MathUtil.rand(0, 10); state = 0; stateTimer = MathUtil.rand(0.5f, 2f); dirX = dirY = 0;
        wobble = MathUtil.rand(0, MathUtil.TAU); spawnAnim = 0; bladeHitTimer = 0; lastChainId = -1;
        xp = t.xp; score = t.score;
    }
}
