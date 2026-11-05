package com.blockkillers.core;

/** Experience and levelling. Level-ups are queued and resolved by the upgrade screen. */
public final class XpManager {
    private final Game g;
    public int pendingLevelUps;
    private float fraction;

    public XpManager(Game g) { this.g = g; }

    public void reset() { pendingLevelUps = 0; fraction = 0; }

    public int needed() { return Config.xpToNext(g.player.level); }

    public float progressOf(Player p) { return Math.min(1f, p.xp / (float) Config.xpToNext(p.level)); }

    public float progress() { return Math.min(1f, g.player.xp / (float) needed()); }

    public void addXp(float amount) {
        Player p = g.player;
        fraction += amount * p.xpMult;
        int whole = (int) fraction;
        fraction -= whole;
        p.xp += whole;
        if (whole > 0) g.audio.play(Sfx.XP);
        while (p.xp >= Config.xpToNext(p.level)) {
            p.xp -= Config.xpToNext(p.level);
            p.level++;
            pendingLevelUps++;
        }
    }
}
