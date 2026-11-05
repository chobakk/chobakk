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

    /** Passive: every PASSIVE_KILLS kills restore PASSIVE_HEAL HP. */
    public static final int PASSIVE_KILLS = 20;
    public static final float PASSIVE_HEAL = 2f;

    // ---------- Ability: Nova Burst ----------
    public static final float NOVA_COOLDOWN = 14f;
    public static final float NOVA_RADIUS = 230f;
    public static final float NOVA_DAMAGE = 40f;
    public static final float NOVA_KNOCKBACK = 420f;

    // ---------- XP ----------
    public static int xpToNext(int level) {
        int l = level - 1;
        return (int) (5 + 4f * l + 0.32f * l * l + 0.004f * l * l * l);
    }

    // ---------- Difficulty (t = seconds survived) ----------
    public static float hpMult(float t) { float m = t / 60f; return 1f + 0.12f * m + 0.035f * m * m + 0.004f * m * m * m; }
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

    // ---------- Growth & camera zoom ----------
    /** Player grows with every level. */
    public static float playerGrowth(int level) { return Math.min(1.8f, 1f + 0.022f * (level - 1)); }
    /** Enemies grow as the run goes on. */
    public static float enemyGrowth(float t) { return Math.min(1.6f, 1f + t / 1000f); }
    /** Camera zooms out as everything grows, so the visible arena stays readable. */
    public static float zoomFor(int level, float t) {
        float g = Math.max(playerGrowth(level), (enemyGrowth(t) + playerGrowth(level)) / 2f);
        return Math.max(0.55f, 1f / (float) Math.pow(g, 0.95));
    }

    // ---------- Enemy fusion tiers (harder enemies = mashed-up shapes) ----------
    public static final int TIER_NORMAL = 0, TIER_FUSED = 1, TIER_MEGA = 2;
    public static final float[] TIER_HP = {1f, 2.6f, 6f}, TIER_DMG = {1f, 1.4f, 2f}, TIER_SIZE = {1f, 1.28f, 1.55f}, TIER_XP = {1f, 3f, 8f};
    public static float fusedChance(float t) { return t < 100 ? 0 : Math.min(0.45f, (t - 100) / 800f); }
    public static float megaChance(float t) { return t < 330 ? 0 : Math.min(0.30f, (t - 330) / 1200f); }

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

    // ---------- Weapons (guns) ----------
    public static final int W_BLASTER = 0, W_TWIN = 1, W_TRI = 2, W_PIERCER = 3, W_ROCKET = 4, W_BLADES = 5, W_LIGHTNING = 6, W_PULSE = 7,
            W_SCATTER = 8, W_RAILGUN = 9, W_MINIGUN = 10, W_FLAMER = 11, W_SEEKER = 12, W_GLAIVE = 13, W_RICOCHET = 14, W_LASER = 15, W_MINES = 16;
    public static final int WEAPON_MAX_LEVEL = 6;
    /** Hotbar size: guns carried at once (only the one in hand fires). */
    public static final int HOTBAR = 5;
    public static final float SWITCH_DELAY = 0.2f;
    /** Item level rises every this many seconds; later loot is stronger. */
    public static final float ITEM_LEVEL_SECONDS = 45f;
    public static final float ITEM_LEVEL_DAMAGE = 0.07f;
    private static final int P = WeaponType.K_PROJECTILE, PAR = WeaponType.K_PARALLEL, SPR = WeaponType.K_SPREAD;
    public static final WeaponType[] WEAPONS = {
        //             id name          kind                   special                    element      gun                    dmg   cd     range spd   cnt prc expl spread inacc crit  color
        new WeaponType(0,  "w_blaster",  P,   WeaponType.SP_NONE,      Loot.E_NONE,  WeaponType.G_PISTOL,  14,  0.42f, 440,  780, 1, 0,   0,  0,  0.02f, 0,     0xFF80D8FF),
        new WeaponType(1,  "w_twin",     PAR, WeaponType.SP_NONE,      Loot.E_NONE,  WeaponType.G_DOUBLE,  12,  0.50f, 440,  740, 2, 0,   0,  0,  0.02f, 0,     0xFF84FFFF),
        new WeaponType(2,  "w_tri",      SPR, WeaponType.SP_NONE,      Loot.E_NONE,  WeaponType.G_WIDE,    11,  0.60f, 400,  720, 3, 0,   0, 14,  0.02f, 0,     0xFFB9F6CA),
        new WeaponType(3,  "w_piercer",  P,   WeaponType.SP_NONE,      Loot.E_NONE,  WeaponType.G_LONG,    24,  0.85f, 600, 1000, 1, 3,   0,  0,  0,     0.05f, 0xFFFFFFFF),
        new WeaponType(4,  "w_rocket",   P,   WeaponType.SP_NONE,      Loot.E_NONE,  WeaponType.G_TUBE,    32,  1.15f, 520,  500, 1, 0,  90,  0,  0.03f, 0,     0xFFFFAB40),
        new WeaponType(5,  "w_blades",   WeaponType.K_ORBIT, WeaponType.SP_NONE, Loot.E_NONE, WeaponType.G_HILT, 16, 0.35f, 78, 3.6f, 3, 0, 0, 0, 0, 0,     0xFFEA80FC),
        new WeaponType(6,  "w_lightning",WeaponType.K_CHAIN, WeaponType.SP_NONE, Loot.E_SHOCK, WeaponType.G_ROD, 22, 0.95f, 430, 0, 4, 0, 0, 0, 0, 0.05f,      0xFFFFFF8D),
        new WeaponType(7,  "w_pulse",    WeaponType.K_AREA,  WeaponType.SP_NONE, Loot.E_NONE, WeaponType.G_DISH, 22, 1.10f, 165, 0, 1, 0, 0, 0, 0, 0,         0xFF64FFDA),
        new WeaponType(8,  "w_scatter",  SPR, WeaponType.SP_NONE,      Loot.E_NONE,  WeaponType.G_WIDE,     8,  0.80f, 300,  820, 6, 0,   0,  8,  0.06f, 0,     0xFFFFD180),
        new WeaponType(9,  "w_railgun",  P,   WeaponType.SP_RAIL,      Loot.E_NONE,  WeaponType.G_LONG,    85,  1.70f, 950, 2400, 1, 99,  0,  0,  0,     0.10f, 0xFF00E5FF),
        new WeaponType(10, "w_minigun",  P,   WeaponType.SP_NONE,      Loot.E_NONE,  WeaponType.G_MINIGUN,  6,  0.08f, 430,  920, 1, 0,   0,  0,  0.13f, 0,     0xFFFFF59D),
        new WeaponType(11, "w_flamer",   P,   WeaponType.SP_FLAME,     Loot.E_FIRE,  WeaponType.G_NOZZLE,   5,  0.06f, 240,  430, 1, 3,   0,  0,  0.22f, 0,     0xFFFF7043),
        new WeaponType(12, "w_seeker",   P,   WeaponType.SP_HOMING,    Loot.E_NONE,  WeaponType.G_TUBE,    18,  0.90f, 560,  430, 2, 0,  45, 30,  0.05f, 0,     0xFFFF8A80),
        new WeaponType(13, "w_glaive",   P,   WeaponType.SP_BOOMERANG, Loot.E_NONE,  WeaponType.G_DISC,    24,  1.00f, 380,  600, 1, 99,  0,  0,  0,     0,     0xFFB388FF),
        new WeaponType(14, "w_ricochet", P,   WeaponType.SP_RICOCHET,  Loot.E_NONE,  WeaponType.G_DISC,    17,  0.70f, 470,  680, 1, 3,   0,  0,  0,     0,     0xFF69F0AE),
        new WeaponType(15, "w_laser",    WeaponType.K_BEAM, WeaponType.SP_NONE, Loot.E_NONE, WeaponType.G_EMITTER, 70, 1.60f, 500, 0, 1, 0, 0, 0, 0, 0,     0xFFFF4081),
        new WeaponType(16, "w_mines",    WeaponType.K_MINE, WeaponType.SP_MINE, Loot.E_NONE, WeaponType.G_BOX,     55, 1.10f, 100, 0, 1, 0, 100, 0, 0, 0,    0xFFFFEB3B),
    };
    public static final float LASER_DURATION = 1.3f, LASER_WIDTH = 16f;
    public static final float MINE_LIFE = 12f, MINE_ARM = 0.4f, MINE_TRIGGER = 48f;
    public static final float CHAIN_JUMP = 175f;

    // ---------- Upgrades ----------
    public static final String[] RARITY_KEYS = {"r_common", "r_rare", "r_epic", "r_legendary"};
    public static final int[] RARITY_COLORS = {0xFFB0BEC5, 0xFF40C4FF, 0xFFD500F9, 0xFFFFC400};
    public static final float[] RARITY_MULT = {1f, 1.6f, 2.4f, 3.5f};
    public static final float[] RARITY_WEIGHT = {62f, 26f, 9.5f, 2.5f};
}
