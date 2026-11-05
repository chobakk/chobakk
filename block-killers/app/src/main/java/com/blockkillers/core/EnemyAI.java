package com.blockkillers.core;

/** Per-type movement and attack behaviour for regular enemies. Bosses are driven by {@link BossManager}. */
public final class EnemyAI {
    private final Game g;
    public EnemyAI(Game g) { this.g = g; }

    public void update(Enemy e, float dt) {
        Player p = g.player;
        float dx = p.x - e.x, dy = p.y - e.y;
        float d = MathUtil.len(dx, dy) + 0.0001f;
        float nx = dx / d, ny = dy / d;
        switch (e.type.behavior) {
            case EnemyType.B_ZIGZAG: {
                e.stateTimer -= dt;
                if (e.stateTimer <= 0) { e.stateTimer = MathUtil.rand(0.4f, 1.1f); e.wobble = MathUtil.rand(-1.2f, 1.2f); }
                float side = MathUtil.sin(e.t * 6f) * 0.9f + e.wobble;
                e.vx = (nx - ny * side) * e.speed;
                e.vy = (ny + nx * side) * e.speed;
                break;
            }
            case EnemyType.B_CHARGE: chargeAI(e, dt, nx, ny, d); break;
            case EnemyType.B_SHOOT: shootAI(e, dt, nx, ny, d); break;
            case EnemyType.B_SWARM: {
                float w = MathUtil.sin(e.t * 4f + e.wobble) * 0.5f;
                e.vx = (nx - ny * w) * e.speed;
                e.vy = (ny + nx * w) * e.speed;
                break;
            }
            default:
                e.vx = nx * e.speed;
                e.vy = ny * e.speed;
        }
    }

    private void chargeAI(Enemy e, float dt, float nx, float ny, float d) {
        switch (e.state) {
            case 0: // approach
                e.vx = nx * e.speed; e.vy = ny * e.speed;
                if (d < Config.CHARGE_RANGE) { e.state = 1; e.stateTimer = Config.CHARGE_WINDUP; e.dirX = nx; e.dirY = ny; }
                break;
            case 1: // wind-up (telegraph)
                e.vx = -e.dirX * 20; e.vy = -e.dirY * 20;
                e.dirX = nx; e.dirY = ny;
                e.stateTimer -= dt;
                e.rotSpeed = 14f;
                if (e.stateTimer <= 0) { e.state = 2; e.stateTimer = Config.CHARGE_TIME; }
                break;
            case 2: // dash
                e.vx = e.dirX * e.speed * Config.CHARGE_SPEED_MULT; e.vy = e.dirY * e.speed * Config.CHARGE_SPEED_MULT;
                e.stateTimer -= dt;
                if (MathUtil.rand() < 0.5f) g.fx.spawn(e.x, e.y, -e.vx * 0.1f, -e.vy * 0.1f, 0.3f, 5f, e.type.color, ParticleSystem.DOT);
                if (e.stateTimer <= 0) { e.state = 3; e.stateTimer = Config.CHARGE_REST; e.rotSpeed = 1.5f; }
                break;
            default: // rest
                e.vx = nx * e.speed * 0.5f; e.vy = ny * e.speed * 0.5f;
                e.stateTimer -= dt;
                if (e.stateTimer <= 0) e.state = 0;
        }
    }

    private void shootAI(Enemy e, float dt, float nx, float ny, float d) {
        float s = e.speed;
        if (d > Config.SHOOT_MAX) { e.vx = nx * s; e.vy = ny * s; }
        else if (d < Config.SHOOT_MIN) { e.vx = -nx * s; e.vy = -ny * s; }
        else { float dir = e.wobble > MathUtil.PI ? 1 : -1; e.vx = -ny * s * 0.6f * dir; e.vy = nx * s * 0.6f * dir; }
        e.stateTimer -= dt;
        if (e.stateTimer <= 0 && d < Config.SHOOT_MAX + 120) {
            e.stateTimer = e.type.attackCooldown;
            g.combat.fireEnemyBullet(e.x, e.y, nx, ny, Config.ENEMY_BULLET_SPEED, Config.ENEMY_BULLET_DAMAGE * Config.dmgMult(g.time), 0xFFFFEA00);
            e.flash = 0.08f;
        }
    }
}
