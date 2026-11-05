package com.blockkillers.core;

/**
 * Companions: up to 3 pets follow the player, shoot on their own and scale with the player's level.
 * New pets and pet upgrades are offered on level-up cards.
 */
public final class PetManager {
    public static final int DRONE = 0, SPRITE = 1, TURRET = 2, WISP = 3;
    public static final int TYPES = 4, MAX_PETS = 3, MAX_LEVEL = 6;
    public static final String[] NAME_KEYS = {"pet_drone", "pet_sprite", "pet_turret", "pet_wisp"};
    public static final String[] DESC_KEYS = {"petd_drone", "petd_sprite", "petd_turret", "petd_wisp"};
    public static final int[] COLORS = {0xFF18FFFF, 0xFFFFEA00, 0xFFFF9100, 0xFF69F0AE};
    private static final float[] DAMAGE = {7f, 9f, 22f, 4f};
    private static final float[] COOLDOWN = {0.45f, 1.1f, 1.6f, 0.9f};
    private static final float RANGE = 420f;

    public static final class Pet {
        public int type, level = 1;
        public float x, y, timer, aim, anim, healTimer;
    }

    private final Game g;
    public final Pet[] pets = new Pet[MAX_PETS];
    public int count;
    private final int[] hits = new int[64];

    public PetManager(Game g) { this.g = g; for (int i = 0; i < MAX_PETS; i++) pets[i] = new Pet(); }

    /** Every run starts with a Drone companion. */
    public void reset() { count = 0; add(DRONE); }

    public boolean has(int type) { for (int i = 0; i < count; i++) if (pets[i].type == type) return true; return false; }
    public boolean canAdd() { return count < MAX_PETS; }

    public void add(int type) {
        if (count >= MAX_PETS || has(type)) return;
        Pet p = pets[count++];
        p.type = type; p.level = 1; p.timer = 0.5f; p.healTimer = 6f;
        p.x = g.player.x; p.y = g.player.y;
    }

    public void levelUp(int index) { if (index < count) pets[index].level = Math.min(MAX_LEVEL, pets[index].level + 1); }

    /** Pets grow with their own level and with the player's level. */
    public float damage(Pet p) {
        return DAMAGE[p.type] * (1f + 0.3f * (p.level - 1)) * (1f + 0.045f * (g.player.level - 1)) * (0.5f + 0.5f * g.player.damageMult);
    }
    public float cooldown(Pet p) { return COOLDOWN[p.type] * (float) Math.pow(0.9, p.level - 1); }

    public void update(float dt) {
        Player pl = g.player;
        for (int i = 0; i < count; i++) {
            Pet p = pets[i];
            p.anim += dt;
            // formation slots around the player, slowly rotating
            float a = g.realTime * 0.6f + i * MathUtil.TAU / Math.max(1, count);
            float dist = pl.radius * 2.6f + 14;
            float tx = pl.x + MathUtil.cos(a) * dist, ty = pl.y + MathUtil.sin(a) * dist;
            float k = Math.min(1f, 6f * dt);
            p.x += (tx - p.x) * k; p.y += (ty - p.y) * k;
            if (p.type == WISP) {
                p.healTimer -= dt;
                if (p.healTimer <= 0) {
                    p.healTimer = 6f * (float) Math.pow(0.9, p.level - 1);
                    float heal = 2f + p.level;
                    pl.hp = Math.min(pl.maxHp, pl.hp + heal);
                    g.fx.ring(pl.x, pl.y, pl.radius * 2, COLORS[WISP], 0.4f);
                    g.fx.text(pl.x, pl.y - pl.radius - 24, "+" + (int) heal, COLORS[WISP], 22f);
                }
            }
            p.timer -= dt;
            if (p.timer > 0) continue;
            Enemy t = g.enemies.nearest(p.x, p.y, RANGE);
            if (t == null) { p.timer = 0.15f; continue; }
            p.timer = cooldown(p);
            p.aim = MathUtil.atan2(t.y - p.y, t.x - p.x);
            float dmg = damage(p);
            int col = COLORS[p.type];
            switch (p.type) {
                case SPRITE: {
                    // zap chains to 2 + level/2 enemies
                    int jumps = 2 + p.level / 2;
                    float fx = p.x, fy = p.y;
                    Enemy cur = t;
                    for (int j = 0; j < jumps && cur != null; j++) {
                        g.fx.bolt(fx, fy, cur.x, cur.y, col);
                        fx = cur.x; fy = cur.y;
                        Enemy hitE = cur;
                        cur = null;
                        int n = g.grid.query(g.enemies.list, fx, fy, 160f, hits);
                        for (int q = 0; q < n; q++) { Enemy o = g.enemies.list.get(hits[q]); if (o != hitE && o.alive) { cur = o; break; } }
                        g.combat.damageEnemy(hitE, dmg, 0, 0, 0);
                    }
                    break;
                }
                case TURRET:
                    g.combat.fireProjectile(p.x, p.y, p.aim, 520f, dmg, 0, 0, 60f + p.level * 6, RANGE, col, Config.W_ROCKET, 7f);
                    break;
                default: {
                    int shots = p.type == DRONE ? 1 + (p.level >= 4 ? 1 : 0) : 1;
                    for (int s = 0; s < shots; s++)
                        g.combat.fireProjectile(p.x, p.y, p.aim + (s - (shots - 1) / 2f) * 0.12f, 800f, dmg, 0.05f, p.level >= 3 ? 1 : 0, 0, RANGE, col, -1, 5f);
                }
            }
            g.audio.play(Sfx.SHOOT);
        }
    }

    /** Draws one pet (also used for UI icons). */
    public static void draw(Gfx gfx, int type, int level, float x, float y, float aim, float t) {
        int c = COLORS[type];
        float bob = MathUtil.sin(t * 5f) * 3f;
        y += bob;
        gfx.circle(x, y, 20, MathUtil.alpha(c, 0.15f));
        switch (type) {
            case DRONE: {
                Shapes.draw(gfx, Shapes.TRIANGLE, x, y, 13, aim + MathUtil.PI / 2, 0xFF263238);
                Shapes.stroke(gfx, Shapes.TRIANGLE, x, y, 13, aim + MathUtil.PI / 2, 2.5f, c);
                gfx.circle(x + MathUtil.cos(aim) * 4, y + MathUtil.sin(aim) * 4, 4, c);
                gfx.circle(x - MathUtil.cos(aim) * 12, y - MathUtil.sin(aim) * 12, 3 + MathUtil.sin(t * 30) * 1.5f, 0xFFFF9100);
                break;
            }
            case SPRITE: {
                Shapes.draw(gfx, Shapes.DIAMOND, x, y, 11, t * 2, c);
                for (int k = 0; k < 3; k++) {
                    float a = t * 6 + k * 2.09f;
                    gfx.circle(x + MathUtil.cos(a) * 16, y + MathUtil.sin(a) * 16, 2.5f, 0xFFFFFFFF);
                }
                break;
            }
            case TURRET: {
                Shapes.draw(gfx, Shapes.HEXAGON, x, y, 13, 0, 0xFF37474F);
                Shapes.stroke(gfx, Shapes.HEXAGON, x, y, 13, 0, 2.5f, c);
                gfx.line(x, y, x + MathUtil.cos(aim) * 20, y + MathUtil.sin(aim) * 20, 6, 0xFF263238);
                gfx.circle(x + MathUtil.cos(aim) * 20, y + MathUtil.sin(aim) * 20, 3.5f, c);
                gfx.circle(x, y, 5, c);
                break;
            }
            default: {
                gfx.circle(x, y, 12, MathUtil.alpha(c, 0.8f));
                gfx.circle(x, y, 7, 0xFFFFFFFF);
                gfx.rect(x - 1.5f, y - 5, x + 1.5f, y + 5, c);
                gfx.rect(x - 5, y - 1.5f, x + 5, y + 1.5f, c);
            }
        }
        for (int k = 0; k < level; k++) gfx.circle(x - (level - 1) * 3.5f + k * 7, y + 22, 2.2f, c);
    }
}
