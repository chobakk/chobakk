package com.blockkillers.core;

/** Developer / test settings (main menu → DEV). Applied at the start of each run while enabled. */
public final class DevOptions {
    public boolean godMode, legendaryLoadout, allPets, oneShot;
    public int startLevel = 1, startMinute = 0;

    public void applyToRun(Game g) {
        g.godMode = godMode;
        if (startLevel > 1) {
            g.player.level = startLevel;
            // grant the matching number of random stat upgrades so the build is realistic
            for (int i = 1; i < startLevel; i++) {
                g.upgrades.roll(false);
                for (int c = 0; c < g.upgrades.cardCount; c++) if (g.upgrades.cards[c].type == UpgradeManager.T_STAT) { g.upgrades.apply(c); break; }
            }
            g.player.hp = g.player.maxHp;
        }
        if (startMinute > 0) {
            g.time = startMinute * 60f;
            if (g.time >= Config.BOSS_FIRST_TIME - Config.BOSS_WARNING) g.bosses.nextBossTime = g.time + 15f;
        }
        if (legendaryLoadout) {
            int[] types = {Config.W_RAILGUN, Config.W_SEEKER, Config.W_FLAMER, Config.W_LASER, Config.W_GLAIVE};
            for (int i = 0; i < Config.HOTBAR; i++) g.weapons.slots[i] = g.loot.roll(types[i], Loot.LEGENDARY, Math.max(1, g.loot.itemLevel()));
        }
        if (allPets) for (int t = 0; t < PetManager.TYPES; t++) g.pets.add(t);
        if (oneShot) g.player.damageMult *= 100f;
    }
}
