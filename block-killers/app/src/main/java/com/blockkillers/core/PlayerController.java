package com.blockkillers.core;

/** Moves the player from joystick input, handles regeneration, invulnerability and the Nova ability cooldown. */
public final class PlayerController {
    private final Game g;
    public float abilityCooldown;
    /** Optional external input (used by automated tests); when set it overrides the joystick. */
    public boolean overrideInput;
    public float inputX, inputY;

    public PlayerController(Game g) { this.g = g; }

    public void reset() { abilityCooldown = 3f; overrideInput = false; inputX = inputY = 0; }

    public void update(float dt) {
        Player p = g.player;
        float ix, iy;
        if (overrideInput) { ix = inputX; iy = inputY; }
        else { ix = g.joystick.outX; iy = g.joystick.outY; }
        float m = MathUtil.len(ix, iy);
        if (m > 1f) { ix /= m; iy /= m; }
        float sp = p.speed();
        float k = Math.min(1f, 22f * dt);   // near-instant response with slight smoothing
        p.vx += (ix * sp - p.vx) * k;
        p.vy += (iy * sp - p.vy) * k;
        p.x = MathUtil.clamp(p.x + p.vx * dt, -Config.WORLD_HALF + p.radius, Config.WORLD_HALF - p.radius);
        p.y = MathUtil.clamp(p.y + p.vy * dt, -Config.WORLD_HALF + p.radius, Config.WORLD_HALF - p.radius);
        if (p.invuln > 0) p.invuln -= dt;
        if (p.hurtFlash > 0) p.hurtFlash -= dt;
        if (p.regen > 0) p.hp = Math.min(p.maxHp, p.hp + p.regen * dt);
        if (abilityCooldown > 0) abilityCooldown -= dt;
        if (m > 0.1f && g.settings.quality > 0 && MathUtil.rand() < 0.4f) {
            g.fx.spawn(p.x - ix * p.radius, p.y - iy * p.radius, -p.vx * 0.2f, -p.vy * 0.2f, 0.3f, 4f, 0xFF40C4FF, ParticleSystem.DOT);
        }
    }

    public boolean abilityReady() { return abilityCooldown <= 0; }

    public void useAbility() {
        if (!abilityReady() || !g.player.alive) return;
        abilityCooldown = Config.NOVA_COOLDOWN;
        g.combat.nova();
    }
}
