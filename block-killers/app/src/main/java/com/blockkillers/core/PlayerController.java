package com.blockkillers.core;

/** Moves the player from joystick input, handles regeneration, invulnerability and the Nova ability cooldown. */
public final class PlayerController {
    private final Game g;
    public float abilityCooldown;
    /** Optional external input (used by automated tests); when set it overrides the joystick. */
    public boolean overrideInput;
    public float inputX, inputY;
    private final float[] pos = new float[2];

    public PlayerController(Game g) { this.g = g; }

    public void reset() { abilityCooldown = 3f; overrideInput = false; inputX = inputY = 0; }

    public void update(float dt) {
        Player p = g.player;
        float ix, iy;
        if (overrideInput) { ix = inputX; iy = inputY; }
        else { ix = g.joystick.outX; iy = g.joystick.outY; }
        if (g.map.corridor()) iy = 0;   // narrow lane: left and right only
        float m = MathUtil.len(ix, iy);
        if (m > 1f) { ix /= m; iy /= m; }
        float sp = p.speed();
        float k = Math.min(1f, 22f * dt);   // near-instant response with slight smoothing
        p.vx += (ix * sp - p.vx) * k;
        p.vy += (iy * sp - p.vy) * k;
        p.x = g.map.clampX(p.x + p.vx * dt, p.radius);
        p.y = g.map.clampY(p.y + p.vy * dt, p.radius);
        pos[0] = p.x; pos[1] = p.y;
        if (g.map.resolve(pos, p.radius)) { p.x = pos[0]; p.y = pos[1]; }
        // grow with level (smoothly) and keep exploring the map
        float targetR = p.baseRadius * Config.playerGrowth(p.level);
        p.radius += (targetR - p.radius) * Math.min(1f, 3f * dt);
        if (p.morph > 0) p.morph = Math.max(0f, p.morph - dt * 1.5f);
        g.map.reveal(p.x, p.y, 750f);
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
        abilityCooldown = Config.NOVA_COOLDOWN * g.shop.novaMult();
        g.combat.nova();
    }
}
