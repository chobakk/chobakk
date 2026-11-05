package com.blockkillers.core;

/** Persistent progression: records, lifetime statistics and achievement flags. */
public final class SaveManager {
    private final Storage st;
    public int bestScore, gamesPlayed, highestLevel, bossesDefeated, achievementMask, legendariesFound, chestsOpened, coins;
    public long coinsEarned;
    public int selectedMap;
    public String playerName = "Player", serverAddress = "";
    public final int[] bestByMap = new int[WorldMap.MODES];
    public long totalKills;
    public float totalTime, bestTime;

    public SaveManager(Storage st) { this.st = st; load(); }

    public void load() {
        bestScore = st.getInt("best_score", 0);
        gamesPlayed = st.getInt("games_played", 0);
        highestLevel = st.getInt("highest_level", 0);
        bossesDefeated = st.getInt("bosses_defeated", 0);
        achievementMask = st.getInt("achievements", 0);
        totalKills = st.getLong("total_kills", 0);
        totalTime = st.getFloat("total_time", 0);
        bestTime = st.getFloat("best_time", 0);
        legendariesFound = st.getInt("legendaries", 0);
        chestsOpened = st.getInt("chests", 0);
        coins = st.getInt("coins", 0);
        coinsEarned = st.getLong("coins_earned", 0);
        selectedMap = MathUtil.clamp(st.getInt("map_mode", 0), 0, WorldMap.MODES - 1);
        playerName = st.getString("player_name", "Player");
        serverAddress = st.getString("server_addr", "");
        for (int i = 0; i < bestByMap.length; i++) bestByMap[i] = st.getInt("best_map_" + i, 0);
    }

    public void save() {
        st.putInt("best_score", bestScore);
        st.putInt("games_played", gamesPlayed);
        st.putInt("highest_level", highestLevel);
        st.putInt("bosses_defeated", bossesDefeated);
        st.putInt("achievements", achievementMask);
        st.putLong("total_kills", totalKills);
        st.putFloat("total_time", totalTime);
        st.putFloat("best_time", bestTime);
        st.putInt("legendaries", legendariesFound);
        st.putInt("chests", chestsOpened);
        st.putInt("coins", coins);
        st.putLong("coins_earned", coinsEarned);
        st.putInt("map_mode", selectedMap);
        st.putString("player_name", playerName);
        st.putString("server_addr", serverAddress);
        for (int i = 0; i < bestByMap.length; i++) st.putInt("best_map_" + i, bestByMap[i]);
        st.flush();
    }

    public boolean recordMapBest(int map, int score) {
        if (score <= bestByMap[map]) return false;
        bestByMap[map] = score; save();
        return true;
    }

    /** Wipes all progress (developer menu). */
    public void resetAll() {
        bestScore = gamesPlayed = highestLevel = bossesDefeated = achievementMask = legendariesFound = chestsOpened = coins = 0;
        totalKills = coinsEarned = 0; totalTime = bestTime = 0;
        for (int i = 0; i < bestByMap.length; i++) bestByMap[i] = 0;
        save();
    }

    public void addCoins(int c) { coins += c; coinsEarned += c; save(); }

    public void recordLoot(int legendaries, int chests) {
        legendariesFound += legendaries;
        chestsOpened += chests;
        save();
    }

    /** Commit a finished run. Returns true if it set a new best score. */
    public boolean recordRun(int score, int kills, float time, int level, int bosses) {
        gamesPlayed++;
        totalKills += kills;
        totalTime += time;
        bossesDefeated += bosses;
        highestLevel = Math.max(highestLevel, level);
        bestTime = Math.max(bestTime, time);
        boolean best = score > bestScore;
        if (best) bestScore = score;
        save();
        return best;
    }
}
