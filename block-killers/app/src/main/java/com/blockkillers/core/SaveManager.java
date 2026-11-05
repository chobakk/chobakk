package com.blockkillers.core;

/** Persistent progression: records, lifetime statistics and achievement flags. */
public final class SaveManager {
    private final Storage st;
    public int bestScore, gamesPlayed, highestLevel, bossesDefeated, achievementMask;
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
        st.flush();
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
