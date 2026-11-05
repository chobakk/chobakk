package com.blockkillers.core;

/**
 * Comedic sound layer (toggle in Settings): air-horn on multi-kills, boom on kill milestones and bosses,
 * bonk on mega-fused kills, "oof" when hit, dramatic sting on death. All original synthesized sounds;
 * players can replace them with their own files (see AndroidAudio custom sounds folder).
 */
public final class MemeSounds {
    private final Game g;
    private final float[] recent = new float[16];
    private int recentN;
    private float hornCooldown;

    public MemeSounds(Game g) { this.g = g; }

    public void reset() { recentN = 0; hornCooldown = 0; }

    private boolean on() { return g.settings.memeSounds; }

    public void update(float dt) { if (hornCooldown > 0) hornCooldown -= dt; }

    public void onKill(Enemy e) {
        if (!on()) return;
        float now = g.realTime;
        // sliding 0.8 s window of kill times
        int keep = 0;
        for (int i = 0; i < recentN; i++) if (now - recent[i] < 0.8f) recent[keep++] = recent[i];
        recentN = keep;
        if (recentN < recent.length) recent[recentN++] = now;
        if (recentN >= 8 && hornCooldown <= 0) {
            hornCooldown = 3f;
            g.audio.play(Sfx.AIRHORN);
            g.fx.text(g.player.x, g.player.y - g.player.radius - 50, g.strings.get("multi_kill"), 0xFFFFD740, 34f);
        } else if (e.tier == 2) {
            g.audio.play(Sfx.BONK);
        } else if (g.kills > 0 && g.kills % 100 == 0) {
            g.audio.play(Sfx.BOOM);
            g.fx.text(g.player.x, g.player.y - g.player.radius - 50, g.kills + " " + g.strings.get("kills").toUpperCase() + "!", 0xFFFF4081, 34f);
        }
    }

    public void onBossKilled() { if (on()) g.audio.play(Sfx.BOOM); }

    /** Returns true if it replaced the normal hurt sound. */
    public boolean onHurt() { if (!on()) return false; g.audio.play(Sfx.OOF); return true; }

    public void onDeath() { g.audio.play(on() ? Sfx.DEATH_STING : Sfx.GAME_OVER); }

    public void onGameOverScreen() { if (on()) g.audio.play(Sfx.SAD_TROMBONE); }
}
