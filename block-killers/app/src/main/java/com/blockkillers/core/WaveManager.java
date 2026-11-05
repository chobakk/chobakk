package com.blockkillers.core;

/** Continuous spawning and difficulty curve; announces each new wave and newly introduced enemy types. */
public final class WaveManager {
    private final Game g;
    private float spawnTimer;
    public int wave;
    private final boolean[] introduced = new boolean[Config.ENEMIES.length];
    private final float[] pt = new float[2];

    public WaveManager(Game g) { this.g = g; }

    public void reset() {
        spawnTimer = 1.2f; wave = 0;
        for (int i = 0; i < introduced.length; i++) introduced[i] = false;
    }

    public void update(float dt) {
        float t = g.time;
        int w = 1 + (int) (t / Config.WAVE_LENGTH);
        if (w != wave) {
            wave = w;
            String newName = null;
            for (EnemyType et : Config.ENEMIES) {
                if (et.spawnWeight > 0 && !introduced[et.id] && et.unlockTime <= t) {
                    introduced[et.id] = true;
                    if (et.unlockTime > 0) newName = g.strings.get(et.nameKey);
                }
            }
            g.ui.announce(g.strings.get("wave") + " " + wave, newName == null ? null : g.strings.get("new_enemy") + ": " + newName);
        } else {
            // enemy types unlocked mid-wave
            for (EnemyType et : Config.ENEMIES) {
                if (et.spawnWeight > 0 && !introduced[et.id] && et.unlockTime <= t) {
                    introduced[et.id] = true;
                    g.ui.announce(g.strings.get("new_enemy"), g.strings.get(et.nameKey));
                }
            }
        }

        spawnTimer -= dt;
        if (spawnTimer > 0) return;
        float interval = Config.spawnInterval(t);
        if (g.bosses.bossActive()) interval *= 2.2f;
        spawnTimer = interval;
        int max = Config.maxEnemies(t, g.settings.quality);
        int batch = Config.spawnBatch(t);
        for (int b = 0; b < batch && g.enemies.aliveCount() < max; b++) spawnOne(t);
    }

    private void spawnOne(float t) {
        float total = 0;
        for (EnemyType et : Config.ENEMIES) if (et.spawnWeight > 0 && et.unlockTime <= t) total += et.spawnWeight;
        float r = MathUtil.rand() * total;
        EnemyType pick = Config.ENEMIES[0];
        for (EnemyType et : Config.ENEMIES) {
            if (et.spawnWeight <= 0 || et.unlockTime > t) continue;
            r -= et.spawnWeight;
            if (r <= 0) { pick = et; break; }
        }
        g.enemies.offscreenPoint(pt);
        if (pick.groupSize > 1) {
            int n = pick.groupSize + MathUtil.randInt(3);
            for (int i = 0; i < n; i++) g.enemies.spawn(pick, pt[0] + MathUtil.rand(-50, 50), pt[1] + MathUtil.rand(-50, 50));
        } else {
            g.enemies.spawn(pick, pt[0], pt[1]);
        }
    }

    /** Spawns a specific enemy type near the player (used by bosses). */
    public void spawnNear(EnemyType t, float x, float y, int n) {
        for (int i = 0; i < n; i++) {
            float a = MathUtil.rand(0, MathUtil.TAU);
            Enemy e = g.enemies.spawn(t, x + MathUtil.cos(a) * 60, y + MathUtil.sin(a) * 60);
            if (e != null) { e.kx = MathUtil.cos(a) * 300; e.ky = MathUtil.sin(a) * 300; }
        }
    }
}
