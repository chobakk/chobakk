package com.blockkillers.core;

/** Achievement definitions, unlocking, notifications and the permanent bonuses they grant. */
public final class AchievementManager {
    public static final int FIRST_KILL = 0, KILLS_100 = 1, KILLS_500 = 2, KILLS_1000 = 3, FIRST_BOSS = 4, LEVEL_10 = 5,
            LEVEL_20 = 6, SURVIVE_5 = 7, SURVIVE_10 = 8, SURVIVE_20 = 9, BOSS_3 = 10, SCORE_50K = 11, LEGENDARY = 12, CHESTS_10 = 13;
    public static final int COUNT = 14;
    public static final String[] KEYS = {"a_first_kill", "a_kills_100", "a_kills_500", "a_kills_1000", "a_first_boss",
            "a_level_10", "a_level_20", "a_survive_5", "a_survive_10", "a_survive_20", "a_boss_3", "a_score_50k", "a_legendary", "a_chests_10"};
    /** Bonus description keys (shown in Statistics). */
    public static final String[] BONUS_KEYS = {"b_dmg2", "b_hp5", "b_dmg5", "b_speed3", "b_hp10", "b_xp5", "b_crit2",
            "b_armor2", "b_atk5", "b_dmg10", "b_armor3", "b_xp5", "b_dmg5", "b_hp5"};

    private final Game g;
    public final float[] toastTimer = new float[1];
    public int toastId = -1;
    private final int[] queue = new int[COUNT];
    private int queueSize;

    public AchievementManager(Game g) { this.g = g; }

    public boolean unlocked(int id) { return (g.save.achievementMask & (1 << id)) != 0; }
    public int unlockedCount() { return Integer.bitCount(g.save.achievementMask & ((1 << COUNT) - 1)); }

    /** Called every frame during gameplay. */
    public void check() {
        long kills = g.save.totalKills + g.kills;
        int bosses = g.save.bossesDefeated + g.bossesKilled;
        float t = g.time;
        int lvl = g.player.level;
        if (g.kills >= 1) unlock(FIRST_KILL);
        if (kills >= 100) unlock(KILLS_100);
        if (kills >= 500) unlock(KILLS_500);
        if (kills >= 1000) unlock(KILLS_1000);
        if (bosses >= 1) unlock(FIRST_BOSS);
        if (bosses >= 3) unlock(BOSS_3);
        if (lvl >= 10) unlock(LEVEL_10);
        if (lvl >= 20) unlock(LEVEL_20);
        if (t >= 300) unlock(SURVIVE_5);
        if (t >= 600) unlock(SURVIVE_10);
        if (t >= 1200) unlock(SURVIVE_20);
        if (g.score >= 50000) unlock(SCORE_50K);
        if (g.save.legendariesFound + g.loot.legendariesThisRun >= 1) unlock(LEGENDARY);
        if (g.save.chestsOpened + g.loot.chestsThisRun >= 10) unlock(CHESTS_10);
    }

    private void unlock(int id) {
        if (unlocked(id)) return;
        g.save.achievementMask |= 1 << id;
        g.save.save();
        g.achievementsThisRun++;
        if (queueSize < COUNT) queue[queueSize++] = id;
    }

    public void update(float dt) {
        if (toastId >= 0 && (g.state == Game.S_PLAYING || g.state == Game.S_GAMEOVER || g.state == Game.S_MENU)) {
            toastTimer[0] -= dt;
            if (toastTimer[0] <= 0) toastId = -1;
        }
        boolean show = g.state == Game.S_PLAYING || g.state == Game.S_GAMEOVER || g.state == Game.S_MENU;
        if (toastId < 0 && queueSize > 0 && show) {
            toastId = queue[0];
            for (int i = 1; i < queueSize; i++) queue[i - 1] = queue[i];
            queueSize--;
            toastTimer[0] = 3f;
            g.audio.play(Sfx.ACHIEVEMENT);
            g.platformVibrate(40);
        }
    }

    public void clearToasts() { toastId = -1; queueSize = 0; }

    /** Permanent bonuses from every unlocked achievement. */
    public void fillBonus(Player.PermanentBonus b) {
        b.clear();
        if (unlocked(FIRST_KILL)) b.damage += 0.02f;
        if (unlocked(KILLS_100)) b.maxHp += 5;
        if (unlocked(KILLS_500)) b.damage += 0.05f;
        if (unlocked(KILLS_1000)) b.speed += 0.03f;
        if (unlocked(FIRST_BOSS)) b.maxHp += 10;
        if (unlocked(LEVEL_10)) b.xp += 0.05f;
        if (unlocked(LEVEL_20)) b.crit += 0.02f;
        if (unlocked(SURVIVE_5)) b.armor += 2;
        if (unlocked(SURVIVE_10)) b.attackSpeed += 0.05f;
        if (unlocked(SURVIVE_20)) b.damage += 0.10f;
        if (unlocked(BOSS_3)) b.armor += 3;
        if (unlocked(SCORE_50K)) b.xp += 0.05f;
        if (unlocked(LEGENDARY)) b.damage += 0.05f;
        if (unlocked(CHESTS_10)) b.maxHp += 5;
    }
}
