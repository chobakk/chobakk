package com.blockkillers.core;

/** Loot tiers, elements, rolled weapon parts and legendary traits. */
public final class Loot {
    private Loot() {}

    // ---- Rarity tiers (colour coded) ----
    public static final int COMMON = 0, UNCOMMON = 1, RARE = 2, EPIC = 3, LEGENDARY = 4;
    public static final int TIERS = 5;
    public static final String[] RARITY_KEYS = {"lr_common", "lr_uncommon", "lr_rare", "lr_epic", "lr_legendary"};
    public static final int[] COLORS = {0xFFECEFF1, 0xFF4CE65A, 0xFF3D9BFF, 0xFFB45CFF, 0xFFFF9800};
    public static final float[] DAMAGE = {1.00f, 1.12f, 1.28f, 1.50f, 1.85f};
    public static final int[] PARTS = {0, 1, 2, 3, 3};
    public static final float[] PART_SCALE = {1f, 1f, 1.12f, 1.25f, 1.45f};
    public static final float[] ELEMENT_CHANCE = {0f, 0.15f, 0.30f, 0.50f, 1f};
    public static final float[] DROP_WEIGHT = {52f, 28f, 13f, 5.5f, 1.5f};
    public static final int[] SCRAP_XP = {3, 6, 12, 25, 50};

    // ---- Elements ----
    /** Elements: Burn (DoT, +vs unprotected), Shock (x2 vs shields, arcs), Frost (slow), Acid (DoT, x1.75 vs armor), Slag (+50% damage taken). */
    public static final int E_NONE = 0, E_FIRE = 1, E_SHOCK = 2, E_FROST = 3, E_ACID = 4, E_SLAG = 5;
    public static final int ELEMENTS = 5;
    public static final String[] ELEMENT_KEYS = {"el_none", "el_fire", "el_shock", "el_frost", "el_acid", "el_slag"};
    public static final int[] ELEMENT_COLORS = {0xFFFFFFFF, 0xFFFF6D00, 0xFF40C4FF, 0xFFB3E5FC, 0xFF76FF03, 0xFFAA00FF};
    public static final String[] ELEMENT_PREFIX = {"", "px_fire", "px_shock", "px_frost", "px_acid", "px_slag"};

    /** Damage multiplier of an element against shields / armor / unprotected health. */
    public static float vsShield(int el) { return el == E_SHOCK ? 2f : (el == E_FIRE || el == E_ACID ? 0.75f : 1f); }
    public static float vsArmor(int el) { return el == E_ACID ? 1.75f : (el == E_FIRE ? 0.75f : 1f); }
    public static float vsFlesh(int el) { return el == E_FIRE ? 1.5f : (el == E_ACID ? 0.9f : 1f); }

    // ---- Rolled parts ----
    public static final int P_DAMAGE = 0, P_RATE = 1, P_CRIT = 2, P_SPEED = 3, P_RANGE = 4, P_PROJ = 5, P_PIERCE = 6;
    public static final int PART_COUNT = 7;
    public static final String[] PART_PREFIX = {"px_damage", "px_rate", "px_crit", "px_speed", "px_range", "px_proj", "px_pierce"};

    // ---- Manufacturers (original) : each has a style ----
    public static final int MAKERS = 6;
    public static final int MK_KRUGG = 0, MK_VOLTIS = 1, MK_HALCYON = 2, MK_FERRO = 3, MK_IRONJAW = 4, MK_NIMBUS = 5;
    public static final String[] MAKER_KEYS = {"mf_krugg", "mf_voltis", "mf_halcyon", "mf_ferro", "mf_ironjaw", "mf_nimbus"};
    public static final String[] MAKER_DESC = {"mfd_krugg", "mfd_voltis", "mfd_halcyon", "mfd_ferro", "mfd_ironjaw", "mfd_nimbus"};
    public static final int[] MAKER_BODY = {0xFF3E2723, 0xFF0D2B45, 0xFFCFD8DC, 0xFF5D1010, 0xFF33402A, 0xFF311B4D};
    public static final int[] MAKER_ACCENT = {0xFFFF8F00, 0xFF00B0FF, 0xFF2979FF, 0xFFFF1744, 0xFFAEEA00, 0xFFD500F9};

    // ---- Gun parts (4 slots, each rolled independently) ----
    public static final String[] BARREL_KEYS = {"pt_barrel_std", "pt_barrel_long", "pt_barrel_heavy", "pt_barrel_split"};
    public static final String[] GRIP_KEYS = {"pt_grip_std", "pt_grip_quick", "pt_grip_stable", "pt_grip_heavy"};
    public static final String[] SIGHT_KEYS = {"pt_sight_iron", "pt_sight_dot", "pt_sight_scope", "pt_sight_holo"};
    public static final String[] ACC_KEYS = {"pt_acc_none", "pt_acc_bayonet", "pt_acc_drum", "pt_acc_comp", "pt_acc_coil"};

    // ---- Legendary traits ----
    public static final int T_NONE = 0, T_VAMPIRIC = 1, T_VOLATILE = 2, T_OVERCHARGE = 3, T_FRACTAL = 4;
    public static final String[] TRAIT_KEYS = {"", "tr_vampiric", "tr_volatile", "tr_overcharge", "tr_fractal"};
    public static final String[] TRAIT_DESC = {"", "trd_vampiric", "trd_volatile", "trd_overcharge", "trd_fractal"};
}
