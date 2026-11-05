package com.blockkillers.core;

import java.util.ArrayList;

/** Draws the arena, enemies, bosses, projectiles, the player and effects with view culling. */
public final class WorldRenderer {
    private final Game g;
    private final float[] tri = new float[6];

    public WorldRenderer(Game g) { this.g = g; }

    public void render(Gfx gfx) {
        Camera cam = g.camera;
        float cx = cam.x + cam.offX, cy = cam.y + cam.offY;
        float hw = g.viewW / 2f, hh = g.viewH / 2f;
        float l = cx - hw - 60, r = cx + hw + 60, t = cy - hh - 60, b = cy + hh + 60;
        gfx.clear(0xFF070B16);
        gfx.save();
        gfx.translate(hw - cx, hh - cy);
        drawArena(gfx, cx, cy, hw, hh);
        drawEnemies(gfx, l, t, r, b);
        drawProjectiles(gfx, l, t, r, b);
        drawBlades(gfx);
        drawPlayer(gfx);
        g.fx.render(gfx, l, t, r, b);
        drawBullets(gfx, l, t, r, b);
        gfx.restore();
        drawOffscreenBossArrow(gfx, hw, hh, cx, cy);
        Player p = g.player;
        if (p.hurtFlash > 0) gfx.rect(0, 0, g.viewW, g.viewH, MathUtil.alpha(0x55FF1744, p.hurtFlash / 0.25f));
        if (p.alive && p.hp < p.maxHp * 0.25f) {
            float a = 0.25f + 0.2f * MathUtil.sin(g.realTime * 6f);
            gfx.ring(hw, hh, Math.max(g.viewW, g.viewH) * 0.75f, Math.max(g.viewW, g.viewH) * 0.35f, MathUtil.alpha(0xFFFF1744, a));
        }
    }

    private void drawArena(Gfx gfx, float cx, float cy, float hw, float hh) {
        float cell = 80f;
        float x0 = (float) Math.floor((cx - hw) / cell) * cell, x1 = cx + hw;
        float y0 = (float) Math.floor((cy - hh) / cell) * cell, y1 = cy + hh;
        int minor = 0x1418FFFF, major = 0x2618FFFF;
        for (float x = x0; x <= x1; x += cell) {
            boolean maj = Math.round(x / cell) % 5 == 0;
            gfx.line(x, cy - hh, x, cy + hh, maj ? 2f : 1f, maj ? major : minor);
        }
        for (float y = y0; y <= y1; y += cell) {
            boolean maj = Math.round(y / cell) % 5 == 0;
            gfx.line(cx - hw, y, cx + hw, y, maj ? 2f : 1f, maj ? major : minor);
        }
        if (g.settings.quality == SettingsManager.Q_HIGH) {
            // parallax dust
            float px = cx * 0.5f, py = cy * 0.5f, sp = 160f;
            float sx0 = (float) Math.floor((px - hw) / sp) * sp, sy0 = (float) Math.floor((py - hh) / sp) * sp;
            for (float x = sx0; x < px + hw; x += sp) for (float y = sy0; y < py + hh; y += sp) {
                int hsh = (int) (x * 7.13f + y * 3.71f) & 0xFF;
                float ox = (hsh % 97), oy = (hsh * 13 % 89);
                gfx.circle(cx + (x + ox - px), cy + (y + oy - py), 1.5f + (hsh % 3), 0x22B388FF);
            }
        }
        // world border
        float W = Config.WORLD_HALF;
        int bc = 0xFFFF1744;
        if (cx - hw < -W + 10) gfx.rect(cx - hw - 10, cy - hh, -W, cy + hh, 0x40FF1744);
        if (cx + hw > W - 10) gfx.rect(W, cy - hh, cx + hw + 10, cy + hh, 0x40FF1744);
        if (cy - hh < -W + 10) gfx.rect(cx - hw, cy - hh - 10, cx + hw, -W, 0x40FF1744);
        if (cy + hh > W - 10) gfx.rect(cx - hw, W, cx + hw, cy + hh + 10, 0x40FF1744);
        gfx.line(-W, -W, W, -W, 5, bc); gfx.line(W, -W, W, W, 5, bc);
        gfx.line(W, W, -W, W, 5, bc); gfx.line(-W, W, -W, -W, 5, bc);
    }

    private void drawEnemies(Gfx gfx, float l, float t, float r, float b) {
        EntityList<Enemy> list = g.enemies.list;
        boolean high = g.settings.quality == SettingsManager.Q_HIGH;
        for (int i = 0; i < list.size; i++) {
            Enemy e = list.get(i);
            if (!e.alive || e.x + e.radius < l || e.x - e.radius > r || e.y + e.radius < t || e.y - e.radius > b) continue;
            if (e.boss) { drawBoss(gfx, e); continue; }
            EnemyType et = e.type;
            float s = e.radius * (0.3f + 0.7f * easeOutBack(e.spawnAnim));
            int col = et.color;
            boolean windup = et.behavior == EnemyType.B_CHARGE && e.state == 1;
            if (windup && ((int) (e.t * 16) & 1) == 0) col = 0xFFFFFFFF;
            float rot = e.rot;
            if (et.behavior == EnemyType.B_CHARGE || et.id == 1 || et.behavior == EnemyType.B_SWARM) {
                // point triangles toward their movement
                rot = MathUtil.atan2(e.vy, e.vx) + MathUtil.PI / 2f;
                if (windup) rot = MathUtil.atan2(e.dirY, e.dirX) + MathUtil.PI / 2f;
            }
            if (high) Shapes.draw(gfx, et.shape, e.x, e.y, s * 1.45f, rot, MathUtil.alpha(col, 0.16f));
            if (e.flash > 0) col = 0xFFFFFFFF;
            Shapes.draw(gfx, et.shape, e.x, e.y, s, rot, col);
            Shapes.draw(gfx, et.shape, e.x, e.y, s * 0.55f, rot, MathUtil.mix(col, 0xFF000000, 0.45f));
            if (et.behavior == EnemyType.B_SHOOT) gfx.circle(e.x, e.y, s * 0.22f, 0xFFFFFFFF);
            if (windup) gfx.line(e.x, e.y, e.x + e.dirX * 120, e.y + e.dirY * 120, 3, 0x88FF6D00);
            if (e.maxHp >= 80 && e.hp < e.maxHp) {
                float w = e.radius * 1.8f, k = e.hp / e.maxHp;
                gfx.rect(e.x - w / 2, e.y - e.radius - 12, e.x + w / 2, e.y - e.radius - 7, 0xAA000000);
                gfx.rect(e.x - w / 2, e.y - e.radius - 12, e.x - w / 2 + w * k, e.y - e.radius - 7, col);
            }
        }
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1;
        float v = x - 1;
        return 1 + c3 * v * v * v + c1 * v * v;
    }

    private void drawBoss(Gfx gfx, Enemy e) {
        BossManager bm = g.bosses;
        int col = bm.color(e), shape = bm.shape(e);
        float s = e.radius * (0.3f + 0.7f * easeOutBack(e.spawnAnim));
        float pulse = 1f + 0.04f * MathUtil.sin(e.t * 5f);
        if (bm.telegraphing(e)) {
            float a = 0.4f + 0.4f * MathUtil.sin(e.t * 30f);
            if (e.bossKind == BossManager.MONOLITH) {
                gfx.line(e.x, e.y, e.x + e.dirX * 520, e.y + e.dirY * 520, e.radius * 1.4f, MathUtil.alpha(col, a * 0.35f));
            } else {
                gfx.ring(e.dirX, e.dirY, e.radius, 4, MathUtil.alpha(col, a));
                gfx.circle(e.dirX, e.dirY, e.radius, MathUtil.alpha(col, a * 0.2f));
            }
        }
        gfx.circle(e.x, e.y, s * 1.7f, MathUtil.alpha(col, 0.10f));
        Shapes.draw(gfx, shape, e.x, e.y, s * 1.3f * pulse, -e.rot * 0.5f, MathUtil.alpha(col, 0.25f));
        Shapes.draw(gfx, shape, e.x, e.y, s * pulse, e.rot, e.flash > 0 ? 0xFFFFFFFF : col);
        Shapes.draw(gfx, shape, e.x, e.y, s * 0.65f, -e.rot, MathUtil.mix(col, 0xFF000000, 0.5f));
        Shapes.stroke(gfx, shape, e.x, e.y, s * 0.4f, e.rot * 2, 4, 0xFFFFFFFF);
        gfx.circle(e.x, e.y, s * 0.16f, 0xFFFFFFFF);
    }

    private void drawProjectiles(Gfx gfx, float l, float t, float r, float b) {
        EntityList<Projectile> list = g.combat.projectiles;
        for (int i = 0; i < list.size; i++) {
            Projectile p = list.get(i);
            if (p.x < l || p.x > r || p.y < t || p.y > b) continue;
            float len = MathUtil.len(p.vx, p.vy) + 0.01f;
            float tx = p.vx / len, ty = p.vy / len;
            float tail = p.weaponId == Config.W_PIERCER ? 34 : 18;
            gfx.line(p.x - tx * tail, p.y - ty * tail, p.x, p.y, p.radius * 1.2f, MathUtil.alpha(p.color, 0.45f));
            if (p.weaponId == Config.W_ROCKET) {
                gfx.circle(p.x, p.y, p.radius + 3, MathUtil.alpha(0xFFFF6D00, 0.5f));
                if (MathUtil.rand() < 0.5f) g.fx.spawn(p.x - tx * 10, p.y - ty * 10, -tx * 60, -ty * 60, 0.3f, 5f, 0xFFFF9100, ParticleSystem.DOT);
            }
            gfx.circle(p.x, p.y, p.radius, p.color);
            gfx.circle(p.x, p.y, p.radius * 0.5f, 0xFFFFFFFF);
        }
    }

    private void drawBullets(Gfx gfx, float l, float t, float r, float b) {
        EntityList<EnemyBullet> list = g.combat.bullets;
        for (int i = 0; i < list.size; i++) {
            EnemyBullet bl = list.get(i);
            if (bl.x < l || bl.x > r || bl.y < t || bl.y > b) continue;
            gfx.circle(bl.x, bl.y, bl.radius + 4, MathUtil.alpha(bl.color, 0.3f));
            gfx.circle(bl.x, bl.y, bl.radius, bl.color);
            gfx.circle(bl.x, bl.y, bl.radius * 0.45f, 0xFF000000);
        }
    }

    private void drawBlades(Gfx gfx) {
        Player p = g.player;
        if (!p.alive) return;
        ArrayList<Weapon> owned = g.weapons.owned;
        for (int i = 0; i < owned.size(); i++) {
            Weapon w = owned.get(i);
            if (w.type.kind != WeaponType.K_ORBIT) continue;
            int n = w.count(p);
            float orbit = g.weapons.bladeRadius(w, p);
            gfx.ring(p.x, p.y, orbit, 1.5f, MathUtil.alpha(w.type.color, 0.15f));
            for (int k = 0; k < n; k++) {
                float a = w.angle + k * MathUtil.TAU / n;
                float bx = p.x + MathUtil.cos(a) * orbit, by = p.y + MathUtil.sin(a) * orbit;
                float rot = g.realTime * 14f;
                for (int j = 0; j < 3; j++) {
                    float ba = rot + j * MathUtil.TAU / 3;
                    tri[0] = bx + MathUtil.cos(ba) * 18; tri[1] = by + MathUtil.sin(ba) * 18;
                    tri[2] = bx + MathUtil.cos(ba + 0.5f) * 6; tri[3] = by + MathUtil.sin(ba + 0.5f) * 6;
                    tri[4] = bx + MathUtil.cos(ba - 0.5f) * 6; tri[5] = by + MathUtil.sin(ba - 0.5f) * 6;
                    gfx.poly(tri, 3, w.type.color);
                }
                gfx.circle(bx, by, 5, 0xFFFFFFFF);
            }
        }
    }

    private void drawPlayer(Gfx gfx) {
        Player p = g.player;
        if (!p.alive) return;
        if (p.invuln > 0 && ((int) (g.realTime * 20) & 1) == 0) return;
        float r = p.radius;
        float pulse = 1f + 0.06f * MathUtil.sin(g.realTime * 4f);
        gfx.circle(p.x, p.y, r * 2.0f * pulse, 0x1A40C4FF);
        gfx.circle(p.x, p.y, r * 1.45f, 0x3340C4FF);
        gfx.circle(p.x, p.y, r, p.hurtFlash > 0 ? 0xFFFF5252 : 0xFF40C4FF);
        gfx.circle(p.x, p.y, r * 0.62f, 0xFFFFFFFF);
        gfx.ring(p.x, p.y, r + 5, 2.5f, 0xCCFFFFFF);
        // aim indicator
        float a = p.facing;
        float tx = p.x + MathUtil.cos(a) * (r + 14), ty = p.y + MathUtil.sin(a) * (r + 14);
        tri[0] = tx; tri[1] = ty;
        tri[2] = p.x + MathUtil.cos(a + 0.35f) * (r + 4); tri[3] = p.y + MathUtil.sin(a + 0.35f) * (r + 4);
        tri[4] = p.x + MathUtil.cos(a - 0.35f) * (r + 4); tri[5] = p.y + MathUtil.sin(a - 0.35f) * (r + 4);
        gfx.poly(tri, 3, 0xFFFFFFFF);
    }

    /** Arrow at the screen edge pointing towards an off-screen boss. */
    private void drawOffscreenBossArrow(Gfx gfx, float hw, float hh, float cx, float cy) {
        Enemy b = g.bosses.current;
        if (b == null || !b.alive) return;
        float dx = b.x - cx, dy = b.y - cy;
        if (Math.abs(dx) < hw && Math.abs(dy) < hh) return;
        float a = MathUtil.atan2(dy, dx);
        float k = Math.min((hw - 40) / Math.max(1e-3f, Math.abs(MathUtil.cos(a))), (hh - 140) / Math.max(1e-3f, Math.abs(MathUtil.sin(a))));
        float x = hw + MathUtil.cos(a) * k, y = hh + MathUtil.sin(a) * k;
        int col = g.bosses.color(b);
        tri[0] = x + MathUtil.cos(a) * 26; tri[1] = y + MathUtil.sin(a) * 26;
        tri[2] = x + MathUtil.cos(a + 2.4f) * 18; tri[3] = y + MathUtil.sin(a + 2.4f) * 18;
        tri[4] = x + MathUtil.cos(a - 2.4f) * 18; tri[5] = y + MathUtil.sin(a - 2.4f) * 18;
        gfx.poly(tri, 3, MathUtil.alpha(col, 0.6f + 0.4f * MathUtil.sin(g.realTime * 8)));
    }
}
