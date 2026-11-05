package com.blockkillers.desktop;

import com.blockkillers.core.*;

/** Shared helpers: game creation, fixed-step ticking and an automated "bot" player. */
public final class Harness {
    public static final float DT = 1f / 60f;
    public final DesktopPlatform platform;
    public final Game game;
    public final int pxW, pxH;
    public boolean bot = true;
    private float levelWait;

    public Harness(DesktopPlatform.MemStorage st, int pxW, int pxH) {
        platform = new DesktopPlatform(st);
        game = new Game(platform);
        this.pxW = pxW; this.pxH = pxH;
        game.resize(pxW, pxH);
    }

    /** Touch at virtual coordinates (converted to pixels like a real screen). */
    public void tap(float vx, float vy) {
        float s = game.scale;
        game.touchDown(9, vx * s, vy * s);
        game.touchUp(9, vx * s, vy * s);
    }
    public void tapButton(UIManager.Button b) { tap(b.cx(), b.cy()); }

    public void tick(float seconds) {
        int n = Math.round(seconds / DT);
        for (int i = 0; i < n; i++) frame();
    }

    public void frame() {
        if (bot) drive();
        game.update(DT);
    }

    /** Simple kiting AI: flee from nearby threats, orbit, stay away from the walls, and pick upgrades. */
    private void drive() {
        Game g = game;
        if (g.state == Game.S_LEVELUP) {
            levelWait += DT;
            if (levelWait > 0.5f) {
                levelWait = 0;
                int pick = MathUtil.randInt(g.upgrades.cardCount);
                // tap the card through the real touch path
                float s = g.scale;
                UIManager.Button b = cardButton(pick);
                g.touchDown(7, b.cx() * s, b.cy() * s);
                g.touchUp(7, b.cx() * s, b.cy() * s);
            }
            return;
        }
        if (g.state != Game.S_PLAYING) return;
        Player p = g.player;
        float fx = 0, fy = 0;
        EntityList<Enemy> list = g.enemies.list;
        for (int i = 0; i < list.size; i++) {
            Enemy e = list.get(i);
            float dx = p.x - e.x, dy = p.y - e.y;
            float d2 = dx * dx + dy * dy;
            float r = 260 + e.radius;
            if (d2 < r * r && d2 > 1) {
                float d = (float) Math.sqrt(d2);
                float w = (r - d) / r * (e.boss ? 3f : 1f);
                fx += dx / d * w; fy += dy / d * w;
            }
        }
        EntityList<EnemyBullet> bl = g.combat.bullets;
        for (int i = 0; i < bl.size; i++) {
            EnemyBullet b = bl.get(i);
            float dx = p.x - b.x, dy = p.y - b.y, d2 = dx * dx + dy * dy;
            if (d2 < 160 * 160 && d2 > 1) { float d = (float) Math.sqrt(d2); fx += dx / d * 1.5f; fy += dy / d * 1.5f; }
        }
        // engage the boss: keep ~300 units away instead of running off
        Enemy boss = g.bosses.current;
        if (boss != null && boss.alive) {
            float dx = boss.x - p.x, dy = boss.y - p.y, d = MathUtil.len(dx, dy) + 1;
            if (d > 360) { fx += dx / d * 1.2f; fy += dy / d * 1.2f; }
        }
        // orbit around the world centre to avoid corners
        float cx = -p.x, cy = -p.y, cd = MathUtil.len(cx, cy) + 1;
        float pull = Math.min(1f, cd / 1500f);
        fx += cx / cd * pull * 0.6f - cy / cd * 0.25f;
        fy += cy / cd * pull * 0.6f + cx / cd * 0.25f;
        float m = MathUtil.len(fx, fy);
        g.playerCtl.overrideInput = true;
        if (m > 0.05f) { g.playerCtl.inputX = fx / m; g.playerCtl.inputY = fy / m; }
        else { g.playerCtl.inputX = 0; g.playerCtl.inputY = 0; }
        // use Nova when surrounded
        int close = 0;
        for (int i = 0; i < list.size; i++) { Enemy e = list.get(i); if (MathUtil.dist2(e.x, e.y, p.x, p.y) < 150 * 150) close++; }
        if (close >= 6 && g.playerCtl.abilityReady()) g.playerCtl.useAbility();
    }

    public UIManager.Button cardButton(int i) {
        // card buttons are laid out deterministically by UIManager.layout()
        return game.ui.levelButton(i);
    }
}
