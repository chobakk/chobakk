package com.blockkillers.core;

import java.util.ArrayList;

/** Builds randomized upgrade cards (stats with rarity, new weapons, weapon levels) and applies them. */
public final class UpgradeManager {
    public static final int DAMAGE = 0, ATTACK_SPEED = 1, MOVE_SPEED = 2, MAX_HP = 3, RECOVERY = 4, CRIT = 5, ARMOR = 6,
            RANGE = 7, XP_BOOST = 8, PROJ_SPEED = 9, EXTRA_PROJ = 10, WEAPON_POWER = 11;
    public static final int STAT_COUNT = 12;
    public static final String[] STAT_KEYS = {"u_damage", "u_atk_speed", "u_move", "u_max_hp", "u_recovery", "u_crit",
            "u_armor", "u_range", "u_xp", "u_proj_speed", "u_extra_proj", "u_weapon_power"};
    /** Base (common) magnitude of each stat upgrade. */
    public static final float[] BASE = {0.10f, 0.08f, 0.06f, 15f, 0.25f, 0.04f, 3f, 0.10f, 0.10f, 0.12f, 1f, 0.12f};
    public static final float[] WEIGHT = {10, 10, 7, 8, 6, 7, 6, 6, 5, 5, 2.5f, 6};

    public static final int T_STAT = 0, T_NEW_WEAPON = 1, T_WEAPON_LEVEL = 2, T_PET_NEW = 3, T_PET_LEVEL = 4;

    public static final class Card {
        /** weapon = type id for new-weapon cards, slot index for weapon-level cards. */
        public int type, stat, weapon, rarity, levels;
        public float value;
        public Weapon rolled;   // new-weapon cards carry a rolled gun (loot rarity)
    }

    private final Game g;
    public final Card[] cards = {new Card(), new Card(), new Card()};
    public int cardCount;
    public boolean bossReward;
    private final ArrayList<Card> candidates = new ArrayList<>();
    private final ArrayList<Float> weights = new ArrayList<>();

    public UpgradeManager(Game g) { this.g = g; }

    private int rollRarity(boolean boss, int min) {
        float luck = Math.min(6f, g.player.level * 0.08f);
        float[] w = {Config.RARITY_WEIGHT[0], Config.RARITY_WEIGHT[1] + luck, Config.RARITY_WEIGHT[2] + luck * 0.6f, Config.RARITY_WEIGHT[3] + luck * 0.25f};
        if (boss) min = Math.max(min, 2);
        float total = 0;
        for (int i = min; i < 4; i++) total += w[i];
        float r = MathUtil.rand() * total;
        for (int i = min; i < 4; i++) { r -= w[i]; if (r <= 0) return i; }
        return 3;
    }

    private boolean statAllowed(int s) {
        Player p = g.player;
        switch (s) {
            case CRIT: return p.critChance < 0.75f;
            case MOVE_SPEED: return p.speedMult < 2.0f;
            case EXTRA_PROJ: return p.extraProjectiles < 4;
            case ATTACK_SPEED: return p.attackSpeedMult < 3f;
            default: return true;
        }
    }

    /** Fills {@link #cards} with three distinct options. */
    public void roll(boolean bossReward) {
        this.bossReward = bossReward;
        candidates.clear(); weights.clear();
        for (int s = 0; s < STAT_COUNT; s++) {
            if (!statAllowed(s)) continue;
            Card c = new Card(); c.type = T_STAT; c.stat = s;
            candidates.add(c); weights.add(WEIGHT[s]);
        }
        WeaponManager wm = g.weapons;
        for (int i = 0; i < wm.slots.length; i++) {
            Weapon w = wm.slots[i];
            if (w == null || w.level >= Config.WEAPON_MAX_LEVEL) continue;
            Card c = new Card(); c.type = T_WEAPON_LEVEL; c.weapon = i;
            candidates.add(c); weights.add(i == wm.active ? 7f : 4f);
        }
        if (wm.canAddMore()) {
            for (WeaponType t : Config.WEAPONS) {
                if (wm.get(t.id) != null) continue;
                Card c = new Card(); c.type = T_NEW_WEAPON; c.weapon = t.id;
                candidates.add(c); weights.add(wm.count() < 3 ? 2.2f : 1.2f);
            }
        }
        PetManager pm = g.pets;
        for (int i = 0; i < pm.count; i++) {
            if (pm.pets[i].level >= PetManager.MAX_LEVEL) continue;
            Card c = new Card(); c.type = T_PET_LEVEL; c.weapon = i;
            candidates.add(c); weights.add(3.5f);
        }
        if (pm.canAdd()) {
            for (int t = 0; t < PetManager.TYPES; t++) {
                if (pm.has(t)) continue;
                Card c = new Card(); c.type = T_PET_NEW; c.weapon = t;
                candidates.add(c); weights.add(1.6f);
            }
        }
        cardCount = 0;
        while (cardCount < 3 && !candidates.isEmpty()) {
            float total = 0;
            for (Float w : weights) total += w;
            float r = MathUtil.rand() * total;
            int pick = candidates.size() - 1;
            for (int i = 0; i < candidates.size(); i++) { r -= weights.get(i); if (r <= 0) { pick = i; break; } }
            Card src = candidates.remove(pick); weights.remove(pick);
            Card c = cards[cardCount++];
            c.type = src.type; c.stat = src.stat; c.weapon = src.weapon; c.rolled = null;
            if (c.type == T_STAT) {
                c.rarity = rollRarity(bossReward, c.stat == EXTRA_PROJ ? 1 : 0);
                c.value = c.stat == EXTRA_PROJ ? (c.rarity >= 3 ? 2 : 1) : BASE[c.stat] * Config.RARITY_MULT[c.rarity];
                if (c.stat == ARMOR || c.stat == MAX_HP) c.value = Math.round(c.value);
            } else if (c.type == T_WEAPON_LEVEL) {
                c.rarity = bossReward ? 2 : 1;
                c.levels = bossReward ? 2 : 1;
            } else if (c.type == T_PET_NEW || c.type == T_PET_LEVEL) {
                c.rarity = c.type == T_PET_NEW ? 2 : 1; c.levels = 1;
            } else {
                int lr = g.loot.rollRarity(bossReward ? Loot.EPIC : Loot.COMMON, 1.5f);
                c.rolled = g.loot.roll(c.weapon, lr, g.loot.itemLevel());
                c.rarity = lr; c.levels = 1;
            }
        }
    }

    public void apply(int index) {
        if (index < 0 || index >= cardCount) return;
        Card c = cards[index];
        Player p = g.player;
        p.upgradesTaken++;
        p.morph = 1f;   // body morph animation
        switch (c.type) {
            case T_NEW_WEAPON: {
                int s = g.weapons.addInstance(c.rolled != null ? c.rolled : g.loot.roll(c.weapon, Loot.COMMON, g.loot.itemLevel()));
                if (s >= 0) g.weapons.select(s);
                break;
            }
            case T_WEAPON_LEVEL: g.weapons.levelUp(c.weapon, c.levels); break;
            case T_PET_NEW: g.pets.add(c.weapon); break;
            case T_PET_LEVEL: g.pets.levelUp(c.weapon); break;
            default:
                p.features[c.stat]++;
                switch (c.stat) {
                    case DAMAGE: p.damageMult += c.value; break;
                    case ATTACK_SPEED: p.attackSpeedMult += c.value; break;
                    case MOVE_SPEED: p.speedMult += c.value; break;
                    case MAX_HP: p.maxHp += c.value; p.hp = Math.min(p.maxHp, p.hp + c.value); break;
                    case RECOVERY: p.hp = Math.min(p.maxHp, p.hp + p.maxHp * c.value); p.regen += c.value * 1.2f; break;
                    case CRIT: p.critChance = Math.min(0.8f, p.critChance + c.value); break;
                    case ARMOR: p.armor += c.value; break;
                    case RANGE: p.rangeMult += c.value; break;
                    case XP_BOOST: p.xpMult += c.value; break;
                    case PROJ_SPEED: p.projSpeedMult += c.value; break;
                    case EXTRA_PROJ: p.extraProjectiles += (int) c.value; break;
                    case WEAPON_POWER: p.weaponPower += c.value; break;
                }
        }
    }

    /** Human readable effect text of a card. */
    public String describe(Card c) {
        Strings s = g.strings;
        if (c.type == T_NEW_WEAPON) return s.get(Config.WEAPONS[c.weapon].descKey);
        if (c.type == T_PET_NEW) return s.get(PetManager.DESC_KEYS[c.weapon]);
        if (c.type == T_PET_LEVEL) {
            int l = g.pets.pets[c.weapon].level;
            return s.get("card_pet_level").replace("{a}", Integer.toString(l)).replace("{b}", Integer.toString(l + 1));
        }
        if (c.type == T_WEAPON_LEVEL) {
            Weapon w = g.weapons.slots[c.weapon];
            int lvl = w == null ? 1 : w.level;
            return s.get("card_weapon_level").replace("{a}", Integer.toString(lvl)).replace("{b}", Integer.toString(Math.min(Config.WEAPON_MAX_LEVEL, lvl + c.levels)));
        }
        String v;
        switch (c.stat) {
            case MAX_HP: case ARMOR: case EXTRA_PROJ: v = "+" + (int) c.value; break;
            case RECOVERY: v = "+" + Math.round(c.value * 100) + "%"; break;
            default: v = "+" + Math.round(c.value * 100) + "%";
        }
        return s.get(STAT_KEYS[c.stat] + "_d").replace("{v}", v);
    }

    public String title(Card c) {
        Strings s = g.strings;
        if (c.type == T_PET_NEW) return s.get("card_new") + " " + s.get(PetManager.NAME_KEYS[c.weapon]);
        if (c.type == T_PET_LEVEL) return s.get(PetManager.NAME_KEYS[g.pets.pets[c.weapon].type]) + " +1";
        if (c.type == T_NEW_WEAPON) return s.get("card_new") + " " + (c.rolled != null ? c.rolled.name(s) : s.get(Config.WEAPONS[c.weapon].nameKey));
        if (c.type == T_WEAPON_LEVEL) {
            Weapon w = g.weapons.slots[c.weapon];
            return (w == null ? "?" : w.name(s)) + " +" + c.levels;
        }
        return s.get(STAT_KEYS[c.stat]);
    }
}
