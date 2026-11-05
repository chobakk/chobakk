package com.blockkillers.core;

/** Centralised, tweakable game balance. Everything gameplay-related that is a number lives here. */
public final class Config {
    private Config() {}

    // ---------- World / view ----------
    public static final float VIRTUAL_WIDTH = 720f;
    public static final float WORLD_HALF = 4000f;          // world is [-4000, 4000]^2
    public static final float GRID_CELL = 80f;
    public static final float CAMERA_LERP = 8f;

    // ---------- Player ----------
    public static final float PLAYER_RADIUS = 22f;
    public static final float PLAYER_MAX_HP = 100f;
    public static final float PLAYER_SPEED = 250f;
    public static final float PLAYER_CRIT = 0.05f;
    public static final float PLAYER_CRIT_MULT = 2.0f;
    public static final float PLAYER_INVULN = 0.3f;
    public static final int MAX_WEAPONS = 6;
    public static final float ARMOR_K = 25f;              // reduction = armor / (armor + K)

    // ---------- Ability: Nova Burst ----------
    public static final float NOVA_COOLDOWN = 14f;
    public static final float NOVA_RADIUS = 230f;
    public static final float NOVA_DAMAGE = 40f;
    public static final float NOVA_KNOCKBACK = 420f;

    // ---------- XP ----------
    public static int xpToNext(int level) {
        int l = level - 1;
        return (int) (5 + 4f * l + 0.32f * l * l);
    }

    // ---------- Difficulty (t = seconds survived) ----------
    public static float hpMult(float t) { float m = t / 60f; return 1f + 0.12f * m + 0.035f * m * m + 0.0025f * m * m * m; }
    public static float dmgMult(float t) { float m = t / 60f; return 1f + 0.08f * m + 0.004f * m * m; }
    public static float speedMult(float t) { return Math.min(1.4f, 1f + 0.025f * (t / 60f)); }
    public static float xpMult(float t) { return 1f + 0.04f * (t / 60f); }
    public static float spawnInterval(float t) { return Math.max(0.10f, 1.05f / (1f + 0.42f * (t / 60f))); }
    public static int spawnBatch(float t) { return 1 + (int) (t / 180f); }
    public static int maxEnemies(float t, int quality) {
        int cap = quality == 0 ? 160 : (quality == 1 ? 260 : 360);
        return Math.min(cap, 22 + (int) (24 * (t / 60f)));
    }
    public static final float WAVE_LENGTH = 60f;

    // ---------- Bosses ----------
    public static final float BOSS_FIRST_TIME = 300f;     // first boss at 5:00
    public static final float BOSS_INTERVAL = 300f;       // then every 5 minutes
    public static final float BOSS_WARNING = 3f;
    public static final float BOSS_BASE_HP = 1150f;
    public static final float BOSS_DAMAGE = 28f;
    public static final int BOSS_XP = 60;
    public static final int BOSS_SCORE = 2500;

    // ---------- Score ----------
    public static final int SCORE_PER_SECOND = 2;
    public static final int SCORE_PER_ACHIEVEMENT = 500;

    // ---------- Enemies ----------
    public static final int C_RED = 0xFFFF3D6E, C_AMBER = 0xFFFFB300, C_VIOLET = 0xFF9D6BFF, C_GREEN = 0xFF00E676,
            C_MAGENTA = 0xFFE040FB, C_CRIMSON = 0xFFFF1744, C_ORANGE = 0xFFFF6D00, C_TANK = 0xFF00BFA5,
            C_YELLOW = 0xFFFFEA00, C_PINK = 0xFFFF80AB;

    public static final EnemyType[] ENEMIES = {
        //            id name           shape              r    hp    dmg  spd  atkCd xp  weight unlock color      behavior               group
        new EnemyType(0, "e_block",    Shapes.SQUARE,    18,  18,   8,   88, 0.8f, 1, 10f,   0,   C_RED,     EnemyType.B_CHASE,  1),
        new EnemyType(1, "e_triangle", Shapes.TRIANGLE,  15,  10,   6,  145, 0.8f, 1,  7f,  30,   C_AMBER,   EnemyType.B_CHASE,  1),
        new EnemyType(2, "e_swarm",    Shapes.TRIANGLE,  10,   6,   4,  160, 0.8f, 1,  2f,  60,   C_PINK,    EnemyType.B_SWARM,  7),
        new EnemyType(3, "e_pentagon", Shapes.PENTAGON,  21,  40,  10,  100, 0.8f, 3,  5f,  90,   C_VIOLET,  EnemyType.B_CHASE,  1),
        new EnemyType(4, "e_diamond",  Shapes.DIAMOND,   17,  24,   8,  140, 0.8f, 2,  4f, 120,   C_MAGENTA, EnemyType.B_ZIGZAG, 1),
        new EnemyType(5, "e_charger",  Shapes.TRIANGLE,  20,  35,  18,   85, 0.8f, 3,  3f, 150,   C_ORANGE,  EnemyType.B_CHARGE, 1),
        new EnemyType(6, "e_hexagon",  Shapes.HEXAGON,   26,  90,  16,   62, 0.9f, 5,  3f, 180,   C_GREEN,   EnemyType.B_CHASE,  1),
        new EnemyType(7, "e_splitter", Shapes.SQUARE,    30,  60,  12,   78, 0.9f, 4,  3f, 210,   C_CRIMSON, EnemyType.B_SPLIT,  1),
        new EnemyType(8, "e_shooter",  Shapes.PENTAGON,  20,  45,   8,   95, 2.2f, 4, 2.5f, 240,  C_YELLOW,  EnemyType.B_SHOOT,  1),
        new EnemyType(9, "e_tank",     Shapes.HEXAGON,   42, 320,  30,   52, 1.0f, 15, 1f, 330,   C_TANK,    EnemyType.B_CHASE,  1),
        // Not spawned directly: produced by splitters.
        new EnemyType(10, "e_mini",    Shapes.SQUARE,    12,  12,   6,  115, 0.8f, 1,  0f, 1e9f, C_CRIMSON, EnemyType.B_CHASE,  1),
    };
    public static final int MINI_ID = 10;
    public static final int SPLIT_COUNT = 3;
    public static final float ENEMY_BULLET_SPEED = 260f;
    public static final float ENEMY_BULLET_DAMAGE = 10f;
    public static final float CHARGE_RANGE = 280f, CHARGE_WINDUP = 0.6f, CHARGE_TIME = 0.55f, CHARGE_SPEED_MULT = 4.2f, CHARGE_REST = 1.8f;
    public static final float SHOOT_MIN = 230f, SHOOT_MAX = 360f;

    // ---------- Weapons ----------
    public static final int W_BLASTER = 0, W_TWIN = 1, W_TRI = 2, W_PIERCER = 3, W_ROCKET = 4, W_BLADES = 5, W_LIGHTNING = 6, W_PULSE = 7;
    public static final int WEAPON_MAX_LEVEL = 6;
    public static final WeaponType[] WEAPONS = {
        //             id name          desc               kind                          dmg  cd    range spd  cnt prc expl  spread crit color
        new WeaponType(0, "w_blaster",  "wd_blaster",  WeaponType.K_PROJECTILE, 10, 0.55f, 430, 760, 1, 0,   0,   0,   0,  0xFF80D8FF),
        new WeaponType(1, "w_twin",     "wd_twin",     WeaponType.K_PARALLEL,    8, 0.70f, 430, 720, 2, 0,   0,   0,   0,  0xFF84FFFF),
        new WeaponType(2, "w_tri",      "wd_tri",      WeaponType.K_SPREAD,      7, 0.80f, 380, 700, 3, 0,   0,  14,   0,  0xFFB9F6CA),
        new WeaponType(3, "w_piercer",  "wd_piercer",  WeaponType.K_PROJECTILE, 15, 1.10f, 580, 980, 1, 3,   0,   0,   0.05f, 0xFFFFFFFF),
        new WeaponType(4, "w_rocket",   "wd_rocket",   WeaponType.K_PROJECTILE, 20, 1.50f, 500, 480, 1, 0,  85,   0,   0,  0xFFFFAB40),
        new WeaponType(5, "w_blades",   "wd_blades",   WeaponType.K_ORBIT,       9, 0.40f,  78,   3.2f, 2, 0, 0,  0,   0,  0xFFEA80FC),
        new WeaponType(6, "w_lightning","wd_lightning",WeaponType.K_CHAIN,      16, 1.40f, 420, 0,   3, 0,   0,   0,   0.05f, 0xFFFFFF8D),
        new WeaponType(7, "w_pulse",    "wd_pulse",    WeaponType.K_AREA,       13, 2.00f, 145, 0,   1, 0,   0,   0,   0,  0xFF64FFDA),
    };
    public static final float CHAIN_JUMP = 175f;

    // ---------- Upgrades ----------
    public static final String[] RARITY_KEYS = {"r_common", "r_rare", "r_epic", "r_legendary"};
    public static final int[] RARITY_COLORS = {0xFFB0BEC5, 0xFF40C4FF, 0xFFD500F9, 0xFFFFC400};
    public static final float[] RARITY_MULT = {1f, 1.6f, 2.4f, 3.5f};
    public static final float[] RARITY_WEIGHT = {62f, 26f, 9.5f, 2.5f};
}
