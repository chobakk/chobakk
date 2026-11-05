package com.blockkillers.core;

/**
 * Draws the arena (regions, obstacles, chests, loot), enemies (incl. fused mash-ups), bosses, projectiles,
 * the evolving player body with the gun in hand, and effects — all under the growth-driven camera zoom.
 */
public final class WorldRenderer {
    private final Game g;
    private final float[] tri = new float[8];

    public WorldRenderer(Game g) { this.g = g; }

    public void render(Gfx gfx) {
        Camera cam = g.camera;
        float z = cam.zoom;
        float cx = cam.x + cam.offX, cy = cam.y + cam.offY;
        float hw = g.viewW / 2f, hh = g.viewH / 2f;
        float vw = hw / z, vh = hh / z;
        float l = cx - vw - 80, r = cx + vw + 80, t = cy - vh - 80, b = cy + vh + 80;
        gfx.clear(0xFF070B16);
        gfx.save();
        gfx.translate(hw, hh);
        gfx.scale(z);
        gfx.translate(-cx, -cy);
        drawArena(gfx, cx, cy, vw, vh);
        drawChests(gfx, l, t, r, b);
        drawGroundLoot(gfx, l, t, r, b);
        drawObstacles(gfx, l, t, r, b);
        drawEnemies(gfx, l, t, r, b);
        drawProjectiles(gfx, l, t, r, b);
        Hero bound = g.hero;
        for (int i = 0; i < g.heroCount; i++) {
            Hero h = g.heroes[i];
            if (!h.active) continue;
            g.bind(h);
            if (h.downed) { drawDowned(gfx, h); continue; }
            drawBeam(gfx);
            drawBlades(gfx);
            drawPets(gfx);
            drawPlayer(gfx);
            if (g.mp()) drawNameTag(gfx, h);
        }
        g.bind(bound);
        g.fx.render(gfx, l, t, r, b);
        drawBullets(gfx, l, t, r, b);
        drawLootLabels(gfx);
        gfx.restore();
        drawOffscreenBossArrow(gfx, hw, hh, cx, cy, z);
        Player p = g.player;
        if (p.hurtFlash > 0) gfx.rect(0, 0, g.viewW, g.viewH, MathUtil.alpha(0x55FF1744, p.hurtFlash / 0.25f));
        if (p.alive && p.hp < p.maxHp * 0.25f) {
            float a = 0.25f + 0.2f * MathUtil.sin(g.realTime * 6f);
            gfx.ring(hw, hh, Math.max(g.viewW, g.viewH) * 0.75f, Math.max(g.viewW, g.viewH) * 0.35f, MathUtil.alpha(0xFFFF1744, a));
        }
    }

    // ------------------------------------------------------------------ arena
    private final float[] rr4 = new float[4];

    private void drawArena(Gfx gfx, float cx, float cy, float vw, float vh) {
        WorldMap m = g.map;
        float W = m.hw, Hh = m.hh;
        float L = Math.max(-W, cx - vw), R = Math.min(W, cx + vw), T = Math.max(-Hh, cy - vh), B = Math.min(Hh, cy + vh);
        float cell = 80f;
        // outside the playable area: hazard walls
        int wall = 0xFF120612;
        if (cy - vh < -Hh) gfx.rect(cx - vw, cy - vh, cx + vw, -Hh, wall);
        if (cy + vh > Hh) gfx.rect(cx - vw, Hh, cx + vw, cy + vh, wall);
        if (cx - vw < -W) gfx.rect(cx - vw, cy - vh, -W, cy + vh, wall);
        if (cx + vw > W) gfx.rect(W, cy - vh, cx + vw, cy + vh, wall);
        if (m.corridor()) {
            // diagonal hazard stripes on the corridor walls
            float x0 = (float) Math.floor((cx - vw) / 60f) * 60f;
            for (float x = x0; x < cx + vw; x += 60f) {
                gfx.line(x, -Hh - 8, x + 40, -Hh - 48, 10, 0x33FF1744);
                gfx.line(x, Hh + 8, x + 40, Hh + 48, 10, 0x33FF1744);
            }
        }
        for (int q = 0; q < 4; q++) {
            m.regionRect(q, rr4);
            float ql = rr4[0], qr = rr4[2], qt = rr4[1], qb = rr4[3];
            float il = Math.max(ql, L), ir = Math.min(qr, R), it = Math.max(qt, T), ib = Math.min(qb, B);
            if (il >= ir || it >= ib) continue;
            gfx.rect(il, it, ir, ib, WorldMap.FLOOR[q]);
            int gc = WorldMap.GRID[q];
            for (float x = (float) Math.ceil(il / cell) * cell; x <= ir; x += cell) {
                boolean maj = Math.round(x / cell) % 5 == 0;
                gfx.line(x, it, x, ib, maj ? 2f : 1f, maj ? MathUtil.alpha(gc, 1.6f) : gc);
            }
            for (float y = (float) Math.ceil(it / cell) * cell; y <= ib; y += cell) {
                boolean maj = Math.round(y / cell) % 5 == 0;
                gfx.line(il, y, ir, y, maj ? 2f : 1f, maj ? MathUtil.alpha(gc, 1.6f) : gc);
            }
        }
        // region borders (neon seams)
        if (!m.corridor()) {
            if (L < 0 && R > 0) gfx.line(0, T, 0, B, 3, 0x33FFFFFF);
            if (T < 0 && B > 0) gfx.line(L, 0, R, 0, 3, 0x33FFFFFF);
        }
        if (g.settings.quality == SettingsManager.Q_HIGH) {
            float px = cx * 0.5f, py = cy * 0.5f, sp = 160f;
            float sx0 = (float) Math.floor((px - vw) / sp) * sp, sy0 = (float) Math.floor((py - vh) / sp) * sp;
            if (!m.corridor()) for (float x = sx0; x < px + vw; x += sp) for (float y = sy0; y < py + vh; y += sp) {
                int hsh = (int) (x * 7.13f + y * 3.71f) & 0xFF;
                gfx.circle(cx + (x + (hsh % 97) - px), cy + (y + (hsh * 13 % 89) - py), 1.5f + (hsh % 3), 0x22B388FF);
            }
        }
        int bc = 0xFFFF1744;
        gfx.line(-W, -Hh, W, -Hh, 6, bc); gfx.line(W, -Hh, W, Hh, 6, bc);
        gfx.line(W, Hh, -W, Hh, 6, bc); gfx.line(-W, Hh, -W, -Hh, 6, bc);
    }

    private void drawObstacles(Gfx gfx, float l, float t, float r, float b) {
        WorldMap m = g.map;
        for (int i = 0; i < m.count; i++) {
            float x = m.ox[i], y = m.oy[i], rad = m.or[i];
            if (x + rad < l || x - rad > r || y + rad < t || y - rad > b) continue;
            int col = WorldMap.OBST[m.regionAt(x, y)];
            Shapes.draw(gfx, m.oshape[i], x + 7, y + 10, rad, m.orot[i], 0x66000000);
            Shapes.draw(gfx, m.oshape[i], x, y, rad, m.orot[i], MathUtil.mix(col, 0xFF000000, 0.55f));
            Shapes.draw(gfx, m.oshape[i], x - rad * 0.06f, y - rad * 0.1f, rad * 0.78f, m.orot[i], MathUtil.mix(col, 0xFF000000, 0.25f));
            Shapes.stroke(gfx, m.oshape[i], x, y, rad, m.orot[i], 2.5f, MathUtil.alpha(col, 0.9f));
        }
    }

    private void drawChests(Gfx gfx, float l, float t, float r, float b) {
        WorldMap m = g.map;
        for (int i = 0; i < m.chestCount; i++) {
            float x = m.cx[i], y = m.cy[i];
            if (x < l || x > r || y < t || y > b) continue;
            int col = m.golden[i] ? 0xFFFFC400 : 0xFF26C6DA;
            if (m.opened[i]) {
                gfx.roundRect(x - 26, y - 14, x + 26, y + 18, 6, 0x55101820);
                gfx.strokeRoundRect(x - 26, y - 14, x + 26, y + 18, 6, 2, 0x44FFFFFF);
                continue;
            }
            float pulse = 0.5f + 0.5f * MathUtil.sin(g.realTime * 3f + i);
            gfx.circle(x, y, 46 + pulse * 6, MathUtil.alpha(col, 0.12f + 0.08f * pulse));
            gfx.roundRect(x - 28, y - 18, x + 28, y + 20, 7, MathUtil.mix(col, 0xFF000000, 0.55f));
            gfx.roundRect(x - 28, y - 22, x + 28, y - 4, 7, MathUtil.mix(col, 0xFF000000, 0.25f));
            gfx.strokeRoundRect(x - 28, y - 22, x + 28, y + 20, 7, 3, col);
            gfx.rect(x - 5, y - 8, x + 5, y + 4, 0xFFFFFFFF);
            gfx.circle(x, y - 2, 3, col);
        }
    }

    private void drawGroundLoot(Gfx gfx, float l, float t, float r, float b) {
        EntityList<GroundItem> items = g.loot.items;
        for (int i = 0; i < items.size; i++) {
            GroundItem it = items.get(i);
            if (it.x < l || it.x > r || it.y < t - 200 || it.y > b) continue;
            if (it.life < 5 && ((int) (it.life * 6) & 1) == 0) continue;
            Weapon w = it.weapon;
            int col = Loot.COLORS[w.rarity];
            float h = 70 + w.rarity * 35;
            // light beam
            for (int k = 0; k < 4; k++) {
                float wdt = 12 - k * 2.5f;
                gfx.rect(it.x - wdt, it.y - h * (1 - k * 0.18f), it.x + wdt, it.y, MathUtil.alpha(col, 0.08f + k * 0.05f));
            }
            gfx.circle(it.x, it.y, 26, MathUtil.alpha(col, 0.25f));
            float bob = MathUtil.sin(it.t * 3f) * 5f;
            gfx.save();
            gfx.translate(it.x, it.y - 10 + bob);
            gfx.rotate(-0.35f);
            drawGun(gfx, w, 1.1f, 0);
            gfx.restore();
            if (w.rarity == Loot.LEGENDARY) {
                for (int k = 0; k < 3; k++) {
                    float a = it.t * 2 + k * 2.1f;
                    gfx.circle(it.x + MathUtil.cos(a) * 30, it.y - 10 + MathUtil.sin(a) * 14, 3, 0xFFFFE082);
                }
            }
        }
    }

    private void drawLootLabels(Gfx gfx) {
        Player p = g.player;
        EntityList<GroundItem> items = g.loot.items;
        for (int i = 0; i < items.size; i++) {
            GroundItem it = items.get(i);
            if (MathUtil.dist2(it.x, it.y, p.x, p.y) > 320 * 320) continue;
            boolean near = it == g.loot.nearby;
            String n = it.weapon.name(g.strings);
            float y = it.y - 50;
            gfx.text(n, it.x + 2, y + 2, near ? 22 : 17, 0xCC000000, Gfx.ALIGN_CENTER, true);
            gfx.text(n, it.x, y, near ? 22 : 17, Loot.COLORS[it.weapon.rarity], Gfx.ALIGN_CENTER, true);
        }
    }

    // ------------------------------------------------------------------ enemies
    private void drawEnemies(Gfx gfx, float l, float t, float r, float b) {
        EntityList<Enemy> list = g.enemies.list;
        boolean high = g.settings.quality == SettingsManager.Q_HIGH;
        for (int i = 0; i < list.size; i++) {
            Enemy e = list.get(i);
            if (!e.alive || e.x + e.radius * 2 < l || e.x - e.radius * 2 > r || e.y + e.radius * 2 < t || e.y - e.radius * 2 > b) continue;
            if (e.boss) { drawBoss(gfx, e); continue; }
            EnemyType et = e.type;
            float s = e.radius * (0.3f + 0.7f * easeOutBack(e.spawnAnim));
            int col = e.fuseColor;
            if (e.burnT > 0) col = MathUtil.mix(col, 0xFFFF6D00, 0.35f + 0.25f * MathUtil.sin(e.t * 20));
            if (e.slowT > 0) col = MathUtil.mix(col, 0xFFB3E5FC, 0.5f);
            if (e.poisonT > 0) col = MathUtil.mix(col, 0xFF76FF03, 0.35f);
            if (e.slagT > 0) col = MathUtil.mix(col, 0xFFAA00FF, 0.45f);
            boolean windup = et.behavior == EnemyType.B_CHARGE && e.state == 1;
            if (windup && ((int) (e.t * 16) & 1) == 0) col = 0xFFFFFFFF;
            float rot = e.rot;
            if (et.behavior == EnemyType.B_CHARGE || et.id == 1 || et.behavior == EnemyType.B_SWARM) {
                rot = MathUtil.atan2(e.vy, e.vx) + MathUtil.PI / 2f;
                if (windup) rot = MathUtil.atan2(e.dirY, e.dirX) + MathUtil.PI / 2f;
            }
            if (e.tier > 0 && high) gfx.circle(e.x, e.y, s * 1.7f, MathUtil.alpha(e.tier == 2 ? 0xFFFF1744 : 0xFFFFAB00, 0.12f));
            if (!zombieSprite2D(gfx, e, s)) Zombies.drawTop(gfx, e, s);
            drawProtection(gfx, e, s);
            if (windup) gfx.line(e.x, e.y, e.x + e.dirX * 120, e.y + e.dirY * 120, 3, 0x88FF6D00);
            if ((e.maxHp >= 80 || e.tier > 0) && e.hp < e.maxHp) {
                float w = e.radius * 1.8f, k = e.hp / e.maxHp;
                gfx.rect(e.x - w / 2, e.y - e.radius - 14, e.x + w / 2, e.y - e.radius - 8, 0xAA000000);
                gfx.rect(e.x - w / 2, e.y - e.radius - 14, e.x - w / 2 + w * k, e.y - e.radius - 8, e.tier == 2 ? 0xFFFF1744 : col);
            }
        }
    }

    /** Shield ring (blue, fades as it drains) and armor plating (steel outline). */
    private void drawProtection(Gfx gfx, Enemy e, float s) {
        if (e.armored) for (int k = 0; k < 6; k++) { float a = k * MathUtil.TAU / 6 + e.t * 0.2f; gfx.arc(e.x, e.y, s * 1.18f, a, 0.6f, 0x88B0BEC5); }
        if (e.shield > 0) {
            float k = e.shield / e.maxShield;
            gfx.circle(e.x, e.y, s * 1.55f, MathUtil.alpha(0xFF448AFF, 0.10f + 0.12f * k + (e.shieldFlash > 0 ? 0.25f : 0)));
            gfx.ring(e.x, e.y, s * 1.55f, 2.5f + 2f * k, MathUtil.alpha(0xFF82B1FF, 0.5f + 0.4f * k));
        }
        if (e.slagT > 0) gfx.ring(e.x, e.y, s * 1.3f, 3f, MathUtil.alpha(0xFFAA00FF, 0.5f + 0.3f * MathUtil.sin(e.t * 10)));
    }

    /** Mashed-up silhouettes: tier 1 fuses two shapes, tier 2 fuses three plus orbiting fragments. */
    private void drawFused(Gfx gfx, Enemy e, float s, float rot, int col, boolean high) {
        int base = e.type.shape;
        int dark = MathUtil.mix(col, 0xFF000000, 0.5f);
        int alt = e.flash > 0 ? 0xFFFFFFFF : MathUtil.mix(e.type.color, 0xFFFFFFFF, 0.15f);
        if (high) gfx.circle(e.x, e.y, s * 1.6f, MathUtil.alpha(col, 0.12f));
        // spikes ring
        int spikes = e.tier == 2 ? 10 : 6;
        for (int k = 0; k < spikes; k++) {
            float a = -rot * 0.7f + k * MathUtil.TAU / spikes;
            float ca = MathUtil.cos(a), sa = MathUtil.sin(a);
            tri[0] = e.x + ca * s * 1.35f; tri[1] = e.y + sa * s * 1.35f;
            tri[2] = e.x + MathUtil.cos(a + 0.22f) * s * 0.85f; tri[3] = e.y + MathUtil.sin(a + 0.22f) * s * 0.85f;
            tri[4] = e.x + MathUtil.cos(a - 0.22f) * s * 0.85f; tri[5] = e.y + MathUtil.sin(a - 0.22f) * s * 0.85f;
            gfx.poly(tri, 3, alt);
        }
        Shapes.draw(gfx, base, e.x, e.y, s, rot, col);
        Shapes.draw(gfx, e.fuseShapeA, e.x, e.y, s * 0.92f, -rot * 1.3f + 0.4f, MathUtil.alpha(alt, 0.85f));
        if (e.tier == 2) {
            Shapes.draw(gfx, e.fuseShapeB, e.x, e.y, s * 0.7f, rot * 2f, col);
            for (int k = 0; k < 3; k++) {
                float a = e.t * 2.5f + k * MathUtil.TAU / 3;
                Shapes.draw(gfx, k == 0 ? base : (k == 1 ? e.fuseShapeA : e.fuseShapeB), e.x + MathUtil.cos(a) * s * 1.6f,
                        e.y + MathUtil.sin(a) * s * 1.6f, s * 0.28f, a, alt);
            }
            Shapes.stroke(gfx, base, e.x, e.y, s * 1.05f, rot, 3, 0xCCFFFFFF);
        }
        Shapes.draw(gfx, base, e.x, e.y, s * 0.45f, rot, dark);
        gfx.circle(e.x, e.y, s * 0.16f, e.tier == 2 ? 0xFFFF1744 : 0xFFFFFFFF);
    }

    /** Status colour for model sprites (multiply) and whether the hit-flash is on. */
    static int statusTint(Enemy e) {
        int c = 0xFFFFFFFF;
        if (e.burnT > 0) c = MathUtil.mix(c, 0xFFFF8A50, 0.55f);
        if (e.slowT > 0) c = MathUtil.mix(c, 0xFFB3E5FC, 0.6f);
        if (e.poisonT > 0) c = MathUtil.mix(c, 0xFFB2FF59, 0.5f);
        if (e.slagT > 0) c = MathUtil.mix(c, 0xFFCE93D8, 0.6f);
        if (e.tier == 2) c = MathUtil.mix(c, 0xFFFF8A80, 0.35f); else if (e.tier == 1) c = MathUtil.mix(c, 0xFFFFE082, 0.3f);
        return c;
    }

    /** Pre-rendered 3D zombie model, top-down frame, rotated to its heading. */
    private boolean zombieSprite2D(Gfx gfx, Enemy e, float s) {
        Sprites sp = g.sprites;
        if (sp == null || !sp.ok) return false;
        Object img = sp.zombies[Math.min(e.type.id, sp.zombies.length - 1)];
        if (img == null) return false;
        float dir = MathUtil.atan2(e.vy, e.vx);
        if (e.type.behavior == EnemyType.B_CHARGE && e.state == 1) dir = MathUtil.atan2(e.dirY, e.dirX);
        float fps = e.type.id == 1 || e.type.id == 4 || e.type.id == 10 ? 14f : 8f;
        int frame = ((int) (e.t * fps)) & 7;
        float size = s * 5.0f;
        gfx.circle(e.x + 3, e.y + 5, s * 0.95f, 0x40000000);
        gfx.save();
        gfx.translate(e.x, e.y);
        gfx.rotate(dir + MathUtil.PI / 2);
        int tint = statusTint(e);
        gfx.image(img, frame * sp.zcw, 8 * sp.zch, sp.zcw, sp.zch, -size / 2, -size / 2, size, size, 1f,
                e.flash > 0 ? Gfx.TINT_FLASH : (tint != 0xFFFFFFFF ? Gfx.TINT_MULTIPLY : Gfx.TINT_NONE), e.flash > 0 ? 0xCCFFFFFF : tint);
        gfx.restore();
        return true;
    }

    private boolean bossSprite2D(Gfx gfx, Enemy e, float s) {
        Sprites sp = g.sprites;
        if (sp == null || !sp.ok || sp.bosses[e.bossKind] == null) return false;
        float dir = MathUtil.atan2(e.vy, e.vx);
        int frame = ((int) (e.t * 6f)) & 7;
        float w = s * 3.4f, h = w * sp.bch / sp.bcw;
        gfx.save();
        gfx.translate(e.x, e.y);
        gfx.rotate(dir + MathUtil.PI / 2);
        gfx.image(sp.bosses[e.bossKind], frame * sp.bcw, 8 * sp.bch, sp.bcw, sp.bch, -w / 2, -h / 2, w, h, 1f,
                e.flash > 0 ? Gfx.TINT_FLASH : Gfx.TINT_NONE, 0xCCFFFFFF);
        gfx.restore();
        return true;
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
        gfx.circle(e.x, e.y, s * 1.7f * pulse, MathUtil.alpha(col, 0.12f));
        if (!bossSprite2D(gfx, e, s)) Zombies.drawBossTop(gfx, e, s);
        drawProtection(gfx, e, s);
    }

    // ------------------------------------------------------------------ projectiles
    private void drawProjectiles(Gfx gfx, float l, float t, float r, float b) {
        EntityList<Projectile> list = g.combat.projectiles;
        for (int i = 0; i < list.size; i++) {
            Projectile p = list.get(i);
            if (p.x < l || p.x > r || p.y < t || p.y > b) continue;
            float len = MathUtil.len(p.vx, p.vy) + 0.01f;
            float tx = p.vx / len, ty = p.vy / len;
            switch (p.special) {
                case WeaponType.SP_FLAME: {
                    float k = MathUtil.clamp(p.age / 0.5f, 0, 1);
                    gfx.circle(p.x, p.y, p.radius, MathUtil.alpha(MathUtil.mix(0xFFFFEB3B, p.color, k), 0.75f - k * 0.5f));
                    continue;
                }
                case WeaponType.SP_MINE: {
                    boolean blink = ((int) (p.age * (p.age > Config.MINE_ARM ? 4 : 12)) & 1) == 0;
                    gfx.circle(p.x, p.y, p.radius + 4, 0x66000000);
                    Shapes.draw(gfx, 8, p.x, p.y, p.radius, 0.39f, MathUtil.mix(p.color, 0xFF000000, 0.4f));
                    gfx.circle(p.x, p.y, 4, blink ? 0xFFFF1744 : 0xFF550000);
                    if (p.age > Config.MINE_ARM) gfx.ring(p.x, p.y, Config.MINE_TRIGGER, 1, MathUtil.alpha(p.color, 0.25f));
                    continue;
                }
                case WeaponType.SP_BOOMERANG: case WeaponType.SP_RICOCHET: {
                    float a = p.age * 18f;
                    if (p.special == WeaponType.SP_BOOMERANG) {
                        for (int k = 0; k < 3; k++) {
                            float ba = a + k * MathUtil.TAU / 3;
                            tri[0] = p.x + MathUtil.cos(ba) * p.radius * 1.5f; tri[1] = p.y + MathUtil.sin(ba) * p.radius * 1.5f;
                            tri[2] = p.x + MathUtil.cos(ba + 0.7f) * p.radius * 0.5f; tri[3] = p.y + MathUtil.sin(ba + 0.7f) * p.radius * 0.5f;
                            tri[4] = p.x; tri[5] = p.y;
                            gfx.poly(tri, 3, p.color);
                        }
                    } else {
                        gfx.circle(p.x, p.y, p.radius, p.color);
                        gfx.line(p.x - MathUtil.cos(a) * p.radius, p.y - MathUtil.sin(a) * p.radius, p.x + MathUtil.cos(a) * p.radius, p.y + MathUtil.sin(a) * p.radius, 3, 0xFF000000);
                    }
                    gfx.circle(p.x, p.y, p.radius * 0.35f, 0xFFFFFFFF);
                    continue;
                }
                case WeaponType.SP_RAIL:
                    gfx.line(p.x - tx * 140, p.y - ty * 140, p.x, p.y, 10, MathUtil.alpha(p.color, 0.3f));
                    gfx.line(p.x - tx * 140, p.y - ty * 140, p.x, p.y, 4, 0xFFFFFFFF);
                    continue;
                default:
            }
            float tail = p.weaponId == Config.W_PIERCER ? 34 : (p.weaponId == Config.W_MINIGUN ? 12 : 18);
            gfx.line(p.x - tx * tail, p.y - ty * tail, p.x, p.y, p.radius * 1.2f, MathUtil.alpha(p.color, 0.45f));
            if (p.weaponId == Config.W_ROCKET || p.special == WeaponType.SP_HOMING) {
                gfx.circle(p.x, p.y, p.radius + 3, MathUtil.alpha(0xFFFF6D00, 0.5f));
                if (MathUtil.rand() < 0.5f) g.fx.spawn(p.x - tx * 10, p.y - ty * 10, -tx * 60, -ty * 60, 0.3f, 5f, 0xFFFF9100, ParticleSystem.DOT);
            }
            gfx.circle(p.x, p.y, p.radius, p.color);
            gfx.circle(p.x, p.y, p.radius * 0.5f, 0xFFFFFFFF);
        }
    }

    private void drawBeam(Gfx gfx) {
        if (!g.weapons.beamActive() || !g.player.alive) return;
        Weapon w = g.weapons.activeWeapon();
        Player p = g.player;
        float len = w.range(p);
        float dx = MathUtil.cos(w.angle), dy = MathUtil.sin(w.angle);
        float sx = p.x + dx * (p.radius + 26), sy = p.y + dy * (p.radius + 26);
        int col = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color;
        float flick = 0.8f + 0.2f * MathUtil.sin(g.realTime * 60);
        gfx.line(sx, sy, p.x + dx * len, p.y + dy * len, Config.LASER_WIDTH * 2.4f * flick, MathUtil.alpha(col, 0.25f));
        gfx.line(sx, sy, p.x + dx * len, p.y + dy * len, Config.LASER_WIDTH * flick, MathUtil.alpha(col, 0.8f));
        gfx.line(sx, sy, p.x + dx * len, p.y + dy * len, 4, 0xFFFFFFFF);
        gfx.circle(sx, sy, 12 * flick, 0xFFFFFFFF);
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
        Weapon w = g.weapons.activeWeapon();
        if (w == null || w.type.kind != WeaponType.K_ORBIT || g.weapons.switchTimer > 0) return;
        int n = w.count(p);
        float orbit = g.weapons.bladeRadius(w, p);
        int col = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color;
        gfx.ring(p.x, p.y, orbit, 1.5f, MathUtil.alpha(col, 0.15f));
        for (int k = 0; k < n; k++) {
            float a = w.angle + k * MathUtil.TAU / n;
            float bx = p.x + MathUtil.cos(a) * orbit, by = p.y + MathUtil.sin(a) * orbit;
            float rot = g.realTime * 14f;
            for (int j = 0; j < 3; j++) {
                float ba = rot + j * MathUtil.TAU / 3;
                tri[0] = bx + MathUtil.cos(ba) * 18; tri[1] = by + MathUtil.sin(ba) * 18;
                tri[2] = bx + MathUtil.cos(ba + 0.5f) * 6; tri[3] = by + MathUtil.sin(ba + 0.5f) * 6;
                tri[4] = bx + MathUtil.cos(ba - 0.5f) * 6; tri[5] = by + MathUtil.sin(ba - 0.5f) * 6;
                gfx.poly(tri, 3, col);
            }
            gfx.circle(bx, by, 5, 0xFFFFFFFF);
        }
    }

    private void drawNameTag(Gfx gfx, Hero h) {
        Player p = h.player;
        float y = p.y - p.radius - 30;
        gfx.text(h.name, p.x + 1.5f, y + 1.5f, 18, 0xAA000000, Gfx.ALIGN_CENTER, true);
        gfx.text(h.name, p.x, y, 18, h.color, Gfx.ALIGN_CENTER, true);
        if (h.id != g.localHero || g.splitScreen) {
            float w = p.radius * 2;
            gfx.rect(p.x - w / 2, y + 6, p.x + w / 2, y + 11, 0x99000000);
            gfx.rect(p.x - w / 2, y + 6, p.x - w / 2 + w * MathUtil.clamp(p.hp / p.maxHp, 0, 1), y + 11, h.color);
        }
        // team ring so every player is easy to tell apart
        gfx.ring(p.x, p.y, p.radius * 1.22f, 3f, MathUtil.alpha(h.color, 0.9f));
    }

    /** Downed (co-op, revivable) or waiting to respawn (PvP). */
    private void drawDowned(Gfx gfx, Hero h) {
        Player p = h.player;
        if (g.pvp()) return;
        float pulse = 0.5f + 0.5f * MathUtil.sin(g.realTime * 6);
        gfx.circle(p.x, p.y, p.radius * 1.1f, 0xFF455A64);
        gfx.circle(p.x, p.y, p.radius * 0.5f, 0xFF90A4AE);
        gfx.ring(p.x, p.y, Config.REVIVE_RANGE, 2, MathUtil.alpha(0xFF00E676, 0.3f + 0.3f * pulse));
        if (h.reviveProgress > 0) gfx.arc(p.x, p.y, p.radius * 1.4f, -MathUtil.PI / 2, MathUtil.TAU * h.reviveProgress / Config.REVIVE_TIME, 0x8800E676);
        gfx.text(h.name + " ✚", p.x, p.y - p.radius - 24, 18, 0xFF00E676, Gfx.ALIGN_CENTER, true);
    }

    private void drawPets(Gfx gfx) {
        if (!g.player.alive) return;
        PetManager pm = g.pets;
        for (int i = 0; i < pm.count; i++) {
            PetManager.Pet p = pm.pets[i];
            PetManager.draw(gfx, p.type, p.level, p.x, p.y, p.aim, p.anim);
        }
    }

    // ------------------------------------------------------------------ player
    /** Body colour shifts as the player evolves (cyan -> violet -> gold). */
    private int bodyColor(Player p) {
        float k = Math.min(1f, p.upgradesTaken / 30f);
        return k < 0.5f ? MathUtil.mix(0xFF40C4FF, 0xFFB388FF, k * 2) : MathUtil.mix(0xFFB388FF, 0xFFFFD740, (k - 0.5f) * 2);
    }

    private void drawPlayer(Gfx gfx) {
        Player p = g.player;
        if (!p.alive) return;
        if (p.invuln > 0 && ((int) (g.realTime * 20) & 1) == 0) return;
        float r = p.radius * (1f + 0.25f * p.morph * MathUtil.sin(p.morph * 14f));
        int[] f = p.features;
        int body = p.hurtFlash > 0 ? 0xFFFF5252 : bodyColor(p);
        float time = g.realTime;
        float move = MathUtil.atan2(p.vy, p.vx);
        boolean moving = MathUtil.len(p.vx, p.vy) > 30;
        gfx.circle(p.x, p.y, r * 2.0f, MathUtil.alpha(body, 0.10f));
        if (f[UpgradeManager.WEAPON_POWER] > 0) gfx.circle(p.x, p.y, r * (1.7f + 0.08f * Math.min(4, f[UpgradeManager.WEAPON_POWER])), MathUtil.alpha(0xFFE040FB, 0.16f));
        // speed fins behind the movement direction
        int fins = Math.min(3, f[UpgradeManager.MOVE_SPEED]);
        if (fins > 0) {
            float a = moving ? move + MathUtil.PI : p.facing + MathUtil.PI;
            for (int k = 0; k < fins * 2; k++) {
                float side = (k % 2 == 0 ? 1 : -1) * (0.45f + 0.25f * (k / 2));
                tri[0] = p.x + MathUtil.cos(a + side) * r * 0.9f; tri[1] = p.y + MathUtil.sin(a + side) * r * 0.9f;
                tri[2] = p.x + MathUtil.cos(a + side * 0.6f) * r * (1.7f + 0.15f * fins); tri[3] = p.y + MathUtil.sin(a + side * 0.6f) * r * (1.7f + 0.15f * fins);
                tri[4] = p.x + MathUtil.cos(a + side * 1.4f) * r * 0.95f; tri[5] = p.y + MathUtil.sin(a + side * 1.4f) * r * 0.95f;
                gfx.poly(tri, 3, MathUtil.alpha(0xFF18FFFF, 0.8f));
            }
        }
        // damage spikes
        int spikes = f[UpgradeManager.DAMAGE] == 0 ? 0 : Math.min(14, 3 + 2 * (f[UpgradeManager.DAMAGE] - 1));
        for (int k = 0; k < spikes; k++) {
            float a = time * 0.6f + k * MathUtil.TAU / spikes;
            tri[0] = p.x + MathUtil.cos(a) * r * 1.6f; tri[1] = p.y + MathUtil.sin(a) * r * 1.6f;
            tri[2] = p.x + MathUtil.cos(a + 0.25f) * r * 1.02f; tri[3] = p.y + MathUtil.sin(a + 0.25f) * r * 1.02f;
            tri[4] = p.x + MathUtil.cos(a - 0.25f) * r * 1.02f; tri[5] = p.y + MathUtil.sin(a - 0.25f) * r * 1.02f;
            gfx.poly(tri, 3, 0xFFFF5252);
        }
        // armor shell (polygon gains sides)
        int armor = f[UpgradeManager.ARMOR];
        if (armor > 0) {
            int sides = Math.min(8, 3 + armor);
            Shapes.draw(gfx, sides, p.x, p.y, r * 1.28f, -time * 0.4f, MathUtil.alpha(0xFF90A4AE, 0.55f));
            Shapes.stroke(gfx, sides, p.x, p.y, r * 1.28f, -time * 0.4f, 3, 0xFFCFD8DC);
        }
        // body: an armed survivor (suit colour = team / evolution colour)
        Sprites sp = g.sprites;
        Object sv = sp != null && sp.ok ? sp.survivors[g.hero.id % sp.survivors.length] : null;
        if (sv != null) {
            int frame = moving ? ((int) (time * 10f)) & 7 : 0;
            float size = r * 5.0f;
            gfx.circle(p.x + 3, p.y + 5, r * 0.95f, 0x40000000);
            gfx.save(); gfx.translate(p.x, p.y); gfx.rotate(p.facing + MathUtil.PI / 2);
            gfx.image(sv, frame * sp.zcw, 8 * sp.zch, sp.zcw, sp.zch, -size / 2, -size / 2, size, size, 1f,
                    p.hurtFlash > 0 ? Gfx.TINT_FLASH : Gfx.TINT_NONE, 0xAAFF5252);
            gfx.restore();
        } else Zombies.drawSurvivorTop(gfx, p.x, p.y, r, p.facing, MathUtil.mix(body, 0xFF263238, 0.35f), 0xFFE0B48C, moving, time);
        // max-HP: extra armor rings on the vest
        int rings = Math.min(4, f[UpgradeManager.MAX_HP]);
        for (int k = 0; k < rings; k++) gfx.ring(p.x, p.y, r * (1.0f + k * 0.1f), 2f, MathUtil.alpha(body, 0.6f));
        // crit star in the core
        int crit = Math.min(3, f[UpgradeManager.CRIT]);
        if (crit > 0) {
            int pts = 3 + crit;
            for (int k = 0; k < pts; k++) {
                float a = -time + k * MathUtil.TAU / pts;
                tri[0] = p.x + MathUtil.cos(a) * r * 0.48f; tri[1] = p.y + MathUtil.sin(a) * r * 0.48f;
                tri[2] = p.x + MathUtil.cos(a + 0.5f) * r * 0.15f; tri[3] = p.y + MathUtil.sin(a + 0.5f) * r * 0.15f;
                tri[4] = p.x + MathUtil.cos(a - 0.5f) * r * 0.15f; tri[5] = p.y + MathUtil.sin(a - 0.5f) * r * 0.15f;
                gfx.poly(tri, 3, 0xFFFFD740);
            }
        }
        if (f[UpgradeManager.RECOVERY] > 0) {
            float s = r * 0.22f * (1f + 0.15f * MathUtil.sin(time * 4));
            gfx.rect(p.x - s * 0.35f, p.y - s, p.x + s * 0.35f, p.y + s, 0xFF00E676);
            gfx.rect(p.x - s, p.y - s * 0.35f, p.x + s, p.y + s * 0.35f, 0xFF00E676);
        }
        // XP halo
        if (f[UpgradeManager.XP_BOOST] > 0) gfx.ring(p.x, p.y, r * 1.45f, 2f + Math.min(4, f[UpgradeManager.XP_BOOST]), MathUtil.alpha(0xFFFFD740, 0.7f));
        // range ticks
        int ticks = Math.min(3, f[UpgradeManager.RANGE]);
        for (int k = 0; k < ticks * 4; k++) {
            float a = k * MathUtil.TAU / (ticks * 4) + time * 0.3f;
            gfx.line(p.x + MathUtil.cos(a) * r * 1.85f, p.y + MathUtil.sin(a) * r * 1.85f, p.x + MathUtil.cos(a) * r * 2.1f, p.y + MathUtil.sin(a) * r * 2.1f, 2, 0x9918FFFF);
        }
        // attack-speed orbiting nodes
        int nodes = Math.min(6, f[UpgradeManager.ATTACK_SPEED]);
        for (int k = 0; k < nodes; k++) {
            float a = time * 4f + k * MathUtil.TAU / nodes;
            gfx.circle(p.x + MathUtil.cos(a) * r * 1.6f, p.y + MathUtil.sin(a) * r * 1.6f, r * 0.14f, 0xFF64FFDA);
        }
        // extra projectile satellites / projectile speed streaks
        int sats = Math.min(4, f[UpgradeManager.EXTRA_PROJ]);
        for (int k = 0; k < sats; k++) {
            float a = p.facing + MathUtil.PI / 2 * (k % 2 == 0 ? 1 : -1) + (k / 2) * 0.5f;
            gfx.circle(p.x + MathUtil.cos(a) * r * 1.15f, p.y + MathUtil.sin(a) * r * 1.15f, r * 0.17f, 0xFF80D8FF);
        }
        if (f[UpgradeManager.PROJ_SPEED] > 0 && moving) {
            for (int k = 0; k < Math.min(3, f[UpgradeManager.PROJ_SPEED]); k++) {
                float off = (k - 1) * r * 0.5f;
                float nx = -MathUtil.sin(move), ny = MathUtil.cos(move);
                gfx.line(p.x + nx * off - MathUtil.cos(move) * r, p.y + ny * off - MathUtil.sin(move) * r,
                        p.x + nx * off - MathUtil.cos(move) * r * 2.2f, p.y + ny * off - MathUtil.sin(move) * r * 2.2f, 3, 0x6618FFFF);
            }
        }
        if (p.morph > 0) gfx.ring(p.x, p.y, r * (1.2f + (1 - p.morph) * 1.5f), 4, MathUtil.alpha(0xFFFFFFFF, p.morph));
        // gun in hand
        Weapon w = g.weapons.activeWeapon();
        if (w != null) {
            float sw = g.weapons.switchTimer > 0 ? g.weapons.switchTimer / Config.SWITCH_DELAY : 0;
            gfx.save();
            gfx.translate(p.x, p.y);
            gfx.rotate(p.facing + sw * 1.2f);
            float sc = p.radius / Config.PLAYER_RADIUS;
            gfx.translate(r * 0.7f - w.recoil * 6 * sc, r * 0.35f);
            if (!gunTopSprite(gfx, w, sc * (1f - 0.4f * sw))) drawGun(gfx, w, sc * (1f - 0.4f * sw), w.recoil);
            if (w.recoil > 0.7f && w.type.kind != WeaponType.K_ORBIT && w.type.kind != WeaponType.K_MINE) {
                float mx = 44 * sc, fl = 16 * sc * (w.recoil - 0.4f);
                int fc = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : 0xFFFFD54F;
                gfx.circle(mx, 0, fl, MathUtil.alpha(fc, 0.45f));
                gfx.circle(mx, 0, fl * 0.5f, 0xEEFFF8E1);
            }
            gfx.restore();
        }
    }

    /** Real gun model (top view) held in the hands, local space x = forward. */
    private boolean gunTopSprite(Gfx gfx, Weapon w, float s) {
        Sprites sp = g.sprites;
        if (sp == null || !sp.ok) return false;
        int gi = sp.gunIndex(w.type.id), row = sp.gunRow[gi];
        float len = 58 * s, h = len / 2;
        gfx.image(sp.guns, 256, row * 128, 256, 128, -len * 0.25f, -h / 2, len, h, 1f, Gfx.TINT_NONE, 0);
        return true;
    }

    /** Draws a gun in local space (x = forward): the real model (side view) with a rarity glow; procedural fallback. */
    public void drawGun(Gfx gfx, Weapon w, float s, float recoil) {
        Sprites sp = g.sprites;
        if (sp != null && sp.ok) {
            int gi = sp.gunIndex(w.type.id), row = sp.gunRow[gi];
            float len = 64 * s, h = len / 2;
            if (w.rarity >= Loot.RARE) gfx.circle(len * 0.28f, 0, len * 0.36f, MathUtil.alpha(Loot.COLORS[w.rarity], 0.16f + 0.05f * w.rarity));
            gfx.image(sp.guns, 0, row * 128, 256, 128, -len * 0.22f, -h / 2, len, h, 1f, Gfx.TINT_NONE, 0);
            if (w.element != Loot.E_NONE) gfx.circle(len * 0.7f, -h * 0.32f, Math.max(2, 4 * s), Loot.ELEMENT_COLORS[w.element]);
            return;
        }
        int trim = Loot.COLORS[w.rarity];
        int body = w.maker >= 0 ? Loot.MAKER_BODY[w.maker] : 0xFF263238, dark = 0xFF101418;
        int accent = w.maker >= 0 ? Loot.MAKER_ACCENT[w.maker] : trim;
        int glow = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color;
        gfx.save();
        gfx.scale(s);
        switch (w.type.gun) {
            case WeaponType.G_DOUBLE:
                gfx.roundRect(-6, -10, 16, 10, 4, body);
                gfx.rect(10, -9, 34, -3, dark); gfx.rect(10, 3, 34, 9, dark);
                gfx.rect(30, -9, 34, -3, glow); gfx.rect(30, 3, 34, 9, glow);
                gfx.strokeRoundRect(-6, -10, 16, 10, 4, 2, trim);
                break;
            case WeaponType.G_WIDE:
                tri[0] = 4; tri[1] = -6; tri[2] = 34; tri[3] = -13; tri[4] = 34; tri[5] = 13; tri[6] = 4; tri[7] = 6;
                gfx.poly(tri, 4, body); gfx.strokePoly(tri, 4, 2, trim);
                gfx.roundRect(-6, -7, 8, 7, 3, dark);
                gfx.rect(31, -13, 35, 13, glow);
                break;
            case WeaponType.G_LONG:
                gfx.roundRect(-8, -6, 18, 6, 3, body);
                gfx.rect(14, -3, 52, 3, dark);
                gfx.roundRect(2, -12, 18, -7, 2, trim);
                gfx.rect(48, -4, 53, 4, glow);
                gfx.strokeRoundRect(-8, -6, 18, 6, 3, 2, trim);
                break;
            case WeaponType.G_TUBE:
                gfx.roundRect(-8, -9, 32, 9, 5, body);
                gfx.circle(32, 0, 9, dark);
                gfx.circle(32, 0, 5, glow);
                gfx.strokeRoundRect(-8, -9, 32, 9, 5, 2, trim);
                break;
            case WeaponType.G_HILT:
                gfx.rect(-4, -3, 12, 3, body);
                gfx.rect(10, -9, 14, 9, trim);
                tri[0] = 14; tri[1] = -4; tri[2] = 38; tri[3] = 0; tri[4] = 14; tri[5] = 4;
                gfx.poly(tri, 3, glow);
                break;
            case WeaponType.G_ROD:
                gfx.rect(-6, -3, 28, 3, body);
                for (int k = 0; k < 3; k++) gfx.ring(6 + k * 7, 0, 6, 2, trim);
                gfx.circle(30, 0, 6 + recoil * 4, glow);
                break;
            case WeaponType.G_DISH:
                gfx.rect(-6, -4, 12, 4, body);
                gfx.arc(18, 0, 14, -MathUtil.PI / 2, MathUtil.PI, body);
                gfx.ring(18, 0, 14, 2, trim);
                gfx.circle(20, 0, 4 + recoil * 4, glow);
                break;
            case WeaponType.G_MINIGUN: {
                gfx.roundRect(-8, -10, 12, 10, 5, body);
                float spin = g.realTime * 30f * (0.3f + recoil);
                for (int k = 0; k < 3; k++) {
                    float off = MathUtil.sin(spin + k * 2.09f) * 6;
                    gfx.line(10, off, 38, off, 3.5f, dark);
                }
                gfx.ring(28, 0, 8, 2, trim);
                gfx.strokeRoundRect(-8, -10, 12, 10, 5, 2, trim);
                break;
            }
            case WeaponType.G_NOZZLE:
                gfx.circle(-2, 6, 9, 0xFFBF360C);
                gfx.ring(-2, 6, 9, 2, trim);
                gfx.rect(4, -4, 30, 4, body);
                gfx.rect(28, -6, 34, 6, dark);
                if (recoil > 0.2f || w.timer < 0.1f) gfx.circle(36, 0, 5, glow);
                break;
            case WeaponType.G_DISC:
                gfx.roundRect(-6, -7, 20, 7, 3, body);
                gfx.circle(26, 0, 10, MathUtil.alpha(glow, 0.9f));
                gfx.circle(26, 0, 4, 0xFFFFFFFF);
                gfx.strokeRoundRect(-6, -7, 20, 7, 3, 2, trim);
                break;
            case WeaponType.G_EMITTER:
                gfx.roundRect(-6, -8, 22, 8, 4, body);
                tri[0] = 22; tri[1] = 0; tri[2] = 32; tri[3] = -9; tri[4] = 42; tri[5] = 0; tri[6] = 32; tri[7] = 9;
                gfx.poly(tri, 4, glow);
                gfx.strokeRoundRect(-6, -8, 22, 8, 4, 2, trim);
                break;
            case WeaponType.G_BOX:
                gfx.roundRect(-8, -10, 18, 10, 3, body);
                gfx.rect(-4, -2, 14, 2, 0xFFFFEB3B);
                gfx.circle(12, -6, 3, 0xFFFF1744);
                gfx.strokeRoundRect(-8, -10, 18, 10, 3, 2, trim);
                break;
            default: // pistol
                gfx.roundRect(-6, -6, 24, 5, 3, body);
                gfx.rect(-2, 2, 6, 12, dark);
                gfx.rect(20, -4, 27, 2, glow);
                gfx.strokeRoundRect(-6, -6, 24, 5, 3, 2, trim);
        }
        if (w.maker >= 0) drawParts(gfx, w, accent, dark, glow);
        gfx.restore();
    }

    /** Visible gun parts: barrel extension, grip, sight and accessory (each rolled on loot). */
    private void drawParts(Gfx gfx, Weapon w, int accent, int dark, int glow) {
        switch (w.barrel) {
            case 1: gfx.rect(28, -2.5f, 44, 2.5f, dark); gfx.rect(42, -3.5f, 46, 3.5f, accent); break;           // long
            case 2: gfx.roundRect(22, -6, 36, 6, 3, dark); gfx.rect(33, -6, 37, 6, accent); break;                // heavy
            case 3: gfx.rect(26, -7, 40, -3, dark); gfx.rect(26, 3, 40, 7, dark); break;                          // split
        }
        switch (w.grip) {
            case 1: tri[0] = 0; tri[1] = 4; tri[2] = 7; tri[3] = 4; tri[4] = 3; tri[5] = 15; gfx.poly(tri, 3, accent); break;
            case 2: gfx.roundRect(-1, 4, 7, 16, 3, dark); gfx.rect(-1, 8, 7, 10, accent); break;
            case 3: gfx.roundRect(-4, 4, 9, 17, 4, dark); break;
        }
        switch (w.sight) {
            case 1: gfx.rect(6, -11, 10, -6, dark); gfx.circle(8, -12, 2.5f, 0xFFFF1744); break;                // red dot
            case 2: gfx.roundRect(2, -15, 22, -8, 3, dark); gfx.circle(22, -11.5f, 3, 0xFF40C4FF); gfx.rect(10, -8, 13, -6, dark); break; // scope
            case 3: gfx.strokeRoundRect(4, -15, 16, -7, 2, 2, accent); break;                                     // holo
        }
        switch (w.accessory) {
            case 1: tri[0] = 26; tri[1] = 5; tri[2] = 42; tri[3] = 8; tri[4] = 26; tri[5] = 9; gfx.poly(tri, 3, 0xFFCFD8DC); break; // bayonet
            case 2: gfx.circle(10, 9, 6, dark); gfx.ring(10, 9, 6, 1.5f, accent); break;                         // drum mag
            case 3: gfx.rect(-12, -3, -6, 3, accent); break;                                                     // stock / comp
            case 4: for (int k = 0; k < 2; k++) gfx.ring(14 + k * 6, 0, 5, 1.5f, glow); break;                   // elemental coil
        }
    }

    /** Arrow at the screen edge pointing towards an off-screen boss. */
    private void drawOffscreenBossArrow(Gfx gfx, float hw, float hh, float cx, float cy, float z) {
        Enemy b = g.bosses.current;
        if (b == null || !b.alive) return;
        float dx = (b.x - cx) * z, dy = (b.y - cy) * z;
        if (Math.abs(dx) < hw && Math.abs(dy) < hh) return;
        float a = MathUtil.atan2(dy, dx);
        float k = Math.min((hw - 40) / Math.max(1e-3f, Math.abs(MathUtil.cos(a))), (hh - 260) / Math.max(1e-3f, Math.abs(MathUtil.sin(a))));
        float x = hw + MathUtil.cos(a) * k, y = hh + MathUtil.sin(a) * k;
        int col = g.bosses.color(b);
        tri[0] = x + MathUtil.cos(a) * 26; tri[1] = y + MathUtil.sin(a) * 26;
        tri[2] = x + MathUtil.cos(a + 2.4f) * 18; tri[3] = y + MathUtil.sin(a + 2.4f) * 18;
        tri[4] = x + MathUtil.cos(a - 2.4f) * 18; tri[5] = y + MathUtil.sin(a - 2.4f) * 18;
        gfx.poly(tri, 3, MathUtil.alpha(col, 0.6f + 0.4f * MathUtil.sin(g.realTime * 8)));
    }
}
