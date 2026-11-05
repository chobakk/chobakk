package com.blockkillers.core;

/** Permanent upgrades bought with coins between runs. Levels persist in {@link Storage}. */
public final class ShopManager {
    public static final int VITALITY = 0, POWER = 1, SWIFT = 2, PLATING = 3, PRECISION = 4, WISDOM = 5, GREED = 6,
            NOVA = 7, MAGNET = 8, LUCK = 9, ARSENAL = 10, SECOND_WIND = 11;
    public static final int COUNT = 12;
    public static final String[] KEYS = {"sh_vitality", "sh_power", "sh_swift", "sh_plating", "sh_precision", "sh_wisdom",
            "sh_greed", "sh_nova", "sh_magnet", "sh_luck", "sh_arsenal", "sh_second_wind"};
    public static final int[] MAX = {10, 10, 5, 8, 8, 8, 10, 5, 5, 6, 2, 1};
    public static final int[] BASE_COST = {40, 60, 50, 60, 70, 50, 80, 70, 40, 90, 250, 400};

    private final Storage st;
    private final SaveManager save;
    public final int[] levels = new int[COUNT];

    public ShopManager(Storage st, SaveManager save) {
        this.st = st; this.save = save;
        for (int i = 0; i < COUNT; i++) levels[i] = MathUtil.clamp(st.getInt("shop_" + i, 0), 0, MAX[i]);
    }

    public boolean maxed(int i) { return levels[i] >= MAX[i]; }
    public int cost(int i) { return (int) (BASE_COST[i] * Math.pow(1.6, levels[i])); }
    public boolean canBuy(int i) { return !maxed(i) && save.coins >= cost(i); }

    public boolean buy(int i) {
        if (!canBuy(i)) return false;
        save.coins -= cost(i);
        levels[i]++;
        st.putInt("shop_" + i, levels[i]);
        save.save();   // flushes storage too
        return true;
    }

    /** Adds shop bonuses to the run's starting stats. */
    public void apply(Player.PermanentBonus b) {
        b.maxHp += 10 * levels[VITALITY];
        b.damage += 0.05f * levels[POWER];
        b.speed += 0.04f * levels[SWIFT];
        b.armor += 2 * levels[PLATING];
        b.crit += 0.02f * levels[PRECISION];
        b.xp += 0.06f * levels[WISDOM];
    }
    public float coinMult() { return 1f + 0.10f * levels[GREED]; }
    public float novaMult() { return 1f - 0.06f * levels[NOVA]; }
    public float magnet() { return 1f + 0.20f * levels[MAGNET]; }
    public float luck() { return 1f + 0.08f * levels[LUCK]; }
    public int arsenal() { return levels[ARSENAL]; }
    public boolean secondWind() { return levels[SECOND_WIND] > 0; }

    /** Effect text for the current level (e.g. "+30 max HP"). */
    public String effect(int i, Strings s) {
        int l = levels[i];
        String v;
        switch (i) {
            case VITALITY: v = "+" + 10 * l; break;
            case PLATING: v = "+" + 2 * l; break;
            case POWER: v = "+" + 5 * l + "%"; break;
            case SWIFT: v = "+" + 4 * l + "%"; break;
            case PRECISION: v = "+" + 2 * l + "%"; break;
            case WISDOM: v = "+" + 6 * l + "%"; break;
            case GREED: v = "+" + 10 * l + "%"; break;
            case NOVA: v = "-" + 6 * l + "%"; break;
            case MAGNET: v = "+" + 20 * l + "%"; break;
            case LUCK: v = "+" + 8 * l + "%"; break;
            default: v = Integer.toString(l);
        }
        return s.get("shd" + KEYS[i].substring(2)).replace("{v}", v);
    }
}
