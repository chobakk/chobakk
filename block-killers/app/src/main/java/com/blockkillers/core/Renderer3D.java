package com.blockkillers.core;

/**
 * First-person 3D view of the same simulation (ray-cast walls + depth-sorted billboards) with an animated
 * weapon view-model: idle sway, walk bob, turn sway, recoil kick, muzzle flash and a lower/raise swap animation.
 */
public final class Renderer3D {
    public static final float FOV = 1.15f;           // ~66 degrees
    private static final float EYE = 46f, WALL = 150f, MAX_D = 2600f, FOG_D = 1500f;
    private final Game g;
    private final int cols = 180;
    private final float[] zbuf = new float[cols];
    // sprites
    private static final int MAXS = 900;
    private final float[] sd = new float[MAXS], sx = new float[MAXS];
    private final int[] skind = new int[MAXS], sidx = new int[MAXS], order = new int[MAXS];
    private int sn;
    private final float[] pts = new float[8];
    private float bobPhase, lastHeading, turnSway;

    public Renderer3D(Game g) { this.g = g; }

    public void render(Gfx gfx) {
        Player me = g.player;
        float W = g.viewW, H = g.viewH;
        float heading = me.view;
        float horizon = H * 0.46f + me.viewPitch;
        float proj = (W / 2f) / (float) Math.tan(FOV / 2f);
        float camX = me.x, camY = me.y;
        WorldMap m = g.map;
        int region = m.regionAt(camX, camY);
        // sky + floor
        gfx.clear(0xFF04060C);
        for (int i = 0; i < 6; i++) {
            float t0 = horizon * i / 6f, t1 = horizon * (i + 1) / 6f;
            gfx.rect(0, t0, W, t1 + 1, MathUtil.mix(0xFF070A14, MathUtil.mix(WorldMap.FLOOR[region], 0xFF20283A, 0.5f), i / 6f));
        }
        int floorC = MathUtil.mix(WorldMap.FLOOR[region], 0xFF263238, 0.35f);
        for (int i = 0; i < 8; i++) {
            float t0 = horizon + (H - horizon) * i / 8f, t1 = horizon + (H - horizon) * (i + 1) / 8f;
            gfx.rect(0, t0, W, t1 + 1, MathUtil.mix(0xFF05070C, floorC, (i + 1) / 8f));
        }
        // ground grid lines receding (sense of motion)
        for (int k = 1; k < 14; k++) {
            float dist = k * 80f - ((camX * MathUtil.cos(heading) + camY * MathUtil.sin(heading)) % 80f + 80f) % 80f;
            if (dist < 20) continue;
            float y = horizon + EYE * proj / dist;
            if (y > H) continue;
            gfx.line(0, y, W, y, 1, MathUtil.alpha(WorldMap.GRID[region], Math.min(1f, 1.6f - dist / 1000f)));
        }
        // walls
        float colW = W / cols;
        for (int c = 0; c < cols; c++) {
            float cx = (c + 0.5f) / cols * 2f - 1f;
            float a = heading + (float) Math.atan(cx * Math.tan(FOV / 2f));
            float dx = MathUtil.cos(a), dy = MathUtil.sin(a);
            float d = m.raycast(camX, camY, dx, dy, MAX_D);
            float perp = Math.max(1f, d * MathUtil.cos(a - heading));
            zbuf[c] = perp;
            if (d >= MAX_D - 1) continue;
            float top = horizon - (WALL - EYE) * proj / perp, bot = horizon + EYE * proj / perp;
            int base;
            if (m.rayHit >= 0) base = WorldMap.OBST[m.regionAt(m.ox[m.rayHit], m.oy[m.rayHit])];
            else base = 0xFF5A1A20;
            float fog = MathUtil.clamp(perp / FOG_D, 0, 0.92f);
            int col = MathUtil.mix(MathUtil.mix(base, 0xFF000000, 0.55f - 0.45f * m.rayShade), 0xFF04060C, fog);
            gfx.rect(c * colW, top, (c + 1) * colW + 0.6f, bot, col);
            if (m.rayHit >= 0) gfx.rect(c * colW, top, (c + 1) * colW + 0.6f, top + Math.max(2, (bot - top) * 0.05f), MathUtil.mix(base, 0xFF04060C, fog));
            else if (((int) ((camX + dx * d + camY + dy * d) / 60f) & 1) == 0) gfx.rect(c * colW, bot - (bot - top) * 0.12f, (c + 1) * colW + 0.6f, bot, MathUtil.mix(0xFFFF1744, 0xFF04060C, fog * 0.9f + 0.05f));
        }
        // sprites
        collect(camX, camY, heading, proj, W);
        for (int i = 0; i < sn; i++) order[i] = i;
        sortFarToNear();
        for (int oi = 0; oi < sn; oi++) {
            int i = order[oi];
            float depth = sd[i], x = sx[i];
            int col = (int) MathUtil.clamp(x / colW, 0, cols - 1);
            float fog = MathUtil.clamp(depth / FOG_D, 0, 0.9f);
            float foot = horizon + EYE * proj / depth;
            switch (skind[i]) {
                case 0: { // zombie / boss
                    Enemy e = g.enemies.list.get(sidx[i]);
                    float h = (e.boss ? e.radius * 3.2f : e.radius * 4.4f) * proj / depth;
                    if (e.type.id == 2 || e.type.id == 10) h *= 0.9f;
                    if (zbuf[col] < depth && !visibleEdge(x, h * 0.3f, depth, colW)) continue;
                    if (!zombieBillboard(gfx, e, x, foot, h, fog, camX, camY)) Zombies.drawFront(gfx, e, x, foot, h, e.t * (e.type.id == 1 ? 12f : 6f), fog);
                    if (e.shield > 0) gfx.ring(x, foot - h * 0.5f, h * 0.45f, Math.max(2, h * 0.02f), MathUtil.alpha(0xFF82B1FF, 0.5f * (1 - fog)));
                    if (e.boss || e.tier > 0 || e.maxHp >= 80) {
                        float bw = h * 0.5f;
                        gfx.rect(x - bw / 2, foot - h * 1.06f, x + bw / 2, foot - h * 1.02f, 0xAA000000);
                        gfx.rect(x - bw / 2, foot - h * 1.06f, x - bw / 2 + bw * MathUtil.clamp(e.hp / e.maxHp, 0, 1), foot - h * 1.02f, e.boss ? g.bosses.color(e) : 0xFFE53935);
                    }
                    break;
                }
                case 1: { // enemy bullet (acid glob)
                    EnemyBullet b = g.combat.bullets.get(sidx[i]);
                    if (zbuf[col] < depth) continue;
                    float r = Math.max(2, b.radius * 1.6f * proj / depth), y = horizon + (EYE - 30) * proj / depth;
                    gfx.circle(x, y, r * 1.8f, MathUtil.alpha(b.color, 0.3f));
                    gfx.circle(x, y, r, b.color);
                    break;
                }
                case 2: { // player projectile
                    Projectile p = g.combat.projectiles.get(sidx[i]);
                    if (zbuf[col] < depth) continue;
                    float r = MathUtil.clamp(p.radius * 1.3f * proj / depth, 1.5f, 26f), y = horizon + (EYE - 34) * proj / depth;
                    gfx.circle(x, y, r * 2f, MathUtil.alpha(p.color, 0.3f));
                    gfx.circle(x, y, r, MathUtil.mix(p.color, 0xFFFFFFFF, 0.4f));
                    break;
                }
                case 3: { // ground loot: light beam + gun
                    GroundItem it = g.loot.items.get(sidx[i]);
                    if (zbuf[col] < depth) continue;
                    int c = Loot.COLORS[it.weapon.rarity];
                    float bh = (100 + it.weapon.rarity * 40) * proj / depth, bw = Math.max(2, 10 * proj / depth);
                    gfx.rect(x - bw, foot - bh, x + bw, foot, MathUtil.alpha(c, 0.25f));
                    gfx.rect(x - bw * 0.4f, foot - bh, x + bw * 0.4f, foot, MathUtil.alpha(c, 0.45f));
                    gfx.save(); gfx.translate(x - 20 * proj / depth, foot - 20 * proj / depth + MathUtil.sin(it.t * 3) * 4 * proj / depth);
                    g.world.drawGun(gfx, it.weapon, 1.4f * proj / depth, 0); gfx.restore();
                    break;
                }
                case 4: { // chest
                    WorldMap mm = g.map;
                    int ci = sidx[i];
                    if (zbuf[col] < depth) continue;
                    int c = mm.golden[ci] ? 0xFFFFC400 : 0xFF26C6DA;
                    float w2 = 30 * proj / depth, hh2 = 40 * proj / depth;
                    gfx.roundRect(x - w2, foot - hh2, x + w2, foot, 4, MathUtil.mix(MathUtil.mix(c, 0xFF000000, 0.5f), 0xFF04060C, fog));
                    gfx.rect(x - w2, foot - hh2, x + w2, foot - hh2 * 0.7f, MathUtil.mix(c, 0xFF04060C, fog));
                    gfx.rect(x - w2 * 0.15f, foot - hh2 * 0.75f, x + w2 * 0.15f, foot - hh2 * 0.45f, 0xFFFFFFFF);
                    break;
                }
                case 5: { // companion pet
                    Hero h = g.heroes[sidx[i] >> 4];
                    PetManager.Pet pt = h.pets.pets[sidx[i] & 15];
                    if (zbuf[col] < depth) continue;
                    float sc = 1.6f * proj / depth;
                    gfx.save(); gfx.translate(x, horizon + (EYE - 60) * proj / depth); gfx.scale(sc);
                    PetManager.draw(gfx, pt.type, pt.level, 0, 0, 0, pt.anim);
                    gfx.restore();
                    break;
                }
                default: { // other player
                    Hero h = g.heroes[sidx[i]];
                    if (zbuf[col] < depth) continue;
                    float hgt = h.player.radius * 4.2f * proj / depth;
                    if (!survivorBillboard(gfx, h, x, foot, hgt, fog, camX, camY)) drawOtherPlayer(gfx, h, x, foot, hgt, fog);
                    else gfx.text(h.name, x, foot - hgt * 1.12f, Math.max(12, hgt * 0.12f), h.color, Gfx.ALIGN_CENTER, true);
                }
            }
        }
        drawViewModel(gfx, W, H);
        // crosshair
        float cx = W / 2, cy = horizon;
        Weapon w = g.weapons.activeWeapon();
        float spread = 10 + (w != null ? w.recoil * 14 : 0);
        int cc = 0xDDFFFFFF;
        gfx.line(cx - spread - 12, cy, cx - spread, cy, 3, cc); gfx.line(cx + spread, cy, cx + spread + 12, cy, 3, cc);
        gfx.line(cx, cy - spread - 12, cx, cy - spread, 3, cc); gfx.line(cx, cy + spread, cx, cy + spread + 12, 3, cc);
        gfx.circle(cx, cy, 2, cc);
        if (me.hurtFlash > 0) gfx.rect(0, 0, W, H, MathUtil.alpha(0x66FF1744, me.hurtFlash / 0.25f));
        if (me.alive && me.hp < me.maxHp * 0.25f) gfx.ring(W / 2, H / 2, Math.max(W, H) * 0.75f, Math.max(W, H) * 0.35f, MathUtil.alpha(0xFFFF1744, 0.3f + 0.2f * MathUtil.sin(g.realTime * 6f)));
    }

    private boolean visibleEdge(float x, float halfW, float depth, float colW) {
        int l = (int) MathUtil.clamp((x - halfW) / colW, 0, cols - 1), r = (int) MathUtil.clamp((x + halfW) / colW, 0, cols - 1);
        return zbuf[l] > depth || zbuf[r] > depth;
    }

    private void add(int kind, int idx, float wx, float wy, float camX, float camY, float c, float s, float proj, float W) {
        if (sn >= MAXS) return;
        float dx = wx - camX, dy = wy - camY;
        float depth = dx * c + dy * s;
        if (depth < 12 || depth > MAX_D) return;
        float lat = -dx * s + dy * c;
        float x = W / 2 + lat / depth * proj;
        if (x < -W * 0.5f || x > W * 1.5f) return;
        sd[sn] = depth; sx[sn] = x; skind[sn] = kind; sidx[sn] = idx; sn++;
    }

    private void collect(float camX, float camY, float heading, float proj, float W) {
        sn = 0;
        float c = MathUtil.cos(heading), s = MathUtil.sin(heading);
        EntityList<Enemy> en = g.enemies.list;
        for (int i = 0; i < en.size; i++) { Enemy e = en.get(i); if (e.alive) add(0, i, e.x, e.y, camX, camY, c, s, proj, W); }
        for (int i = 0; i < g.combat.bullets.size; i++) { EnemyBullet b = g.combat.bullets.get(i); add(1, i, b.x, b.y, camX, camY, c, s, proj, W); }
        for (int i = 0; i < g.combat.projectiles.size; i++) {
            Projectile p = g.combat.projectiles.get(i);
            // skip your own shots right at the muzzle (the view-model flash covers them)
            if (p.owner == g.hero && MathUtil.dist2(p.x, p.y, camX, camY) < 140f * 140f) continue;
            add(2, i, p.x, p.y, camX, camY, c, s, proj, W);
        }
        for (int i = 0; i < g.loot.items.size; i++) { GroundItem it = g.loot.items.get(i); add(3, i, it.x, it.y, camX, camY, c, s, proj, W); }
        WorldMap m = g.map;
        for (int i = 0; i < m.chestCount; i++) if (!m.opened[i]) add(4, i, m.cx[i], m.cy[i], camX, camY, c, s, proj, W);
        for (int h = 0; h < g.heroCount; h++) {
            Hero hero = g.heroes[h];
            if (!hero.active) continue;
            for (int k = 0; k < hero.pets.count; k++) add(5, (h << 4) | k, hero.pets.pets[k].x, hero.pets.pets[k].y, camX, camY, c, s, proj, W);
            if (hero != g.hero && !hero.downed) add(6, h, hero.player.x, hero.player.y, camX, camY, c, s, proj, W);
        }
    }

    private void sortFarToNear() {
        // insertion sort is fine for mostly-sorted frames; fall back to shell sort for big lists
        for (int gap = sn / 2; gap > 0; gap /= 2)
            for (int i = gap; i < sn; i++) {
                int v = order[i]; float dv = sd[v]; int j = i;
                while (j >= gap && sd[order[j - gap]] < dv) { order[j] = order[j - gap]; j -= gap; }
                order[j] = v;
            }
    }

    /** Which of the 8 pre-rendered facing rows to use (row 0 = facing the camera, clockwise in 45 deg steps). */
    private static int facingRow(float dir, float wx, float wy, float camX, float camY) {
        float rel = dir - MathUtil.atan2(camY - wy, camX - wx);
        int r = Math.round(rel / (MathUtil.PI / 4f));
        return ((r % 8) + 8) % 8;
    }

    private static int fogTint(int tint, float fog) { return MathUtil.mix(tint, 0xFF05070C, fog); }

    /** Pre-rendered CC0 zombie / boss model as a directional billboard. */
    private boolean zombieBillboard(Gfx gfx, Enemy e, float x, float foot, float h, float fog, float camX, float camY) {
        Sprites sp = g.sprites;
        if (sp == null || !sp.ok) return false;
        Object img; int cw, ch;
        if (e.boss) { img = sp.bosses[Math.min(e.bossKind, sp.bosses.length - 1)]; cw = sp.bcw; ch = sp.bch; }
        else { img = sp.zombies[Math.min(e.type.id, sp.zombies.length - 1)]; cw = sp.zcw; ch = sp.zch; }
        if (img == null) return false;
        float dir = MathUtil.atan2(e.vy, e.vx);
        if (e.vx * e.vx + e.vy * e.vy < 1f) dir = MathUtil.atan2(camY - e.y, camX - e.x);
        if (!e.boss && e.type.behavior == EnemyType.B_CHARGE && e.state == 1) dir = MathUtil.atan2(e.dirY, e.dirX);
        int row = facingRow(dir, e.x, e.y, camX, camY);
        float fps = e.boss ? 6f : (e.type.id == 1 || e.type.id == 4 || e.type.id == 10 ? 14f : 8f);
        int frame = ((int) (e.t * fps)) & 7;
        float dh = h * 1.12f, dw = dh * cw / ch;
        float top = foot - dh * 0.965f;
        int tint = e.boss ? 0xFFFFFFFF : WorldRenderer.statusTint(e);
        tint = fogTint(tint, fog);
        if (e.flash > 0) gfx.image(img, frame * cw, row * ch, cw, ch, x - dw / 2, top, dw, dh, 1f, Gfx.TINT_FLASH, 0xCCFFFFFF);
        else gfx.image(img, frame * cw, row * ch, cw, ch, x - dw / 2, top, dw, dh, 1f, tint != 0xFFFFFFFF ? Gfx.TINT_MULTIPLY : Gfx.TINT_NONE, tint);
        return true;
    }

    /** Another player's survivor model as a directional billboard. */
    private boolean survivorBillboard(Gfx gfx, Hero h, float x, float foot, float hgt, float fog, float camX, float camY) {
        Sprites sp = g.sprites;
        if (sp == null || !sp.ok) return false;
        Object img = sp.survivors[h.id % sp.survivors.length];
        if (img == null) return false;
        Player p = h.player;
        int row = facingRow(p.facing, p.x, p.y, camX, camY);
        boolean moving = p.vx * p.vx + p.vy * p.vy > 100f;
        int frame = moving ? ((int) (g.realTime * 10f)) & 7 : 0;
        float dh = hgt * 1.12f, dw = dh * sp.zcw / sp.zch;
        int tint = fogTint(0xFFFFFFFF, fog);
        if (p.hurtFlash > 0) gfx.image(img, frame * sp.zcw, row * sp.zch, sp.zcw, sp.zch, x - dw / 2, foot - dh * 0.965f, dw, dh, 1f, Gfx.TINT_FLASH, 0xAAFF5252);
        else gfx.image(img, frame * sp.zcw, row * sp.zch, sp.zcw, sp.zch, x - dw / 2, foot - dh * 0.965f, dw, dh, 1f, tint != 0xFFFFFFFF ? Gfx.TINT_MULTIPLY : Gfx.TINT_NONE, tint);
        return true;
    }

    private void drawOtherPlayer(Gfx gfx, Hero h, float x, float foot, float hgt, float fog) {
        int suit = MathUtil.mix(MathUtil.mix(h.color, 0xFF263238, 0.4f), 0xFF04060C, fog);
        float w = hgt * 0.36f;
        gfx.line(x - w * 0.2f, foot - hgt * 0.45f, x - w * 0.22f, foot, w * 0.22f, 0xFF263238);
        gfx.line(x + w * 0.2f, foot - hgt * 0.45f, x + w * 0.22f, foot, w * 0.22f, 0xFF263238);
        gfx.roundRect(x - w * 0.5f, foot - hgt * 0.8f, x + w * 0.5f, foot - hgt * 0.42f, w * 0.15f, suit);
        gfx.circle(x, foot - hgt * 0.88f, hgt * 0.1f, MathUtil.mix(0xFFE0B48C, 0xFF04060C, fog));
        gfx.text(h.name, x, foot - hgt * 1.05f, Math.max(12, hgt * 0.12f), h.color, Gfx.ALIGN_CENTER, true);
    }

    /** First-person view-model from the pre-rendered CC0 gun model (Quaternius / Kenney). */
    private boolean fpSprite(Gfx gfx, Weapon w, float W, float H, float offX, float offY, float sw, float kick, Player p) {
        Sprites sp = g.sprites;
        if (sp == null || !sp.ok) return false;
        int gi = sp.gunIndex(w.type.id);
        Object img = sp.fp[gi];
        if (img == null) return false;
        float S = Math.min(H * 1.15f, W * 0.62f);
        float k = S / 384f;
        float[] mz = sp.fpMuzzle[gi];
        // muzzle lands just below-right of the crosshair; recoil kicks the gun back (down-right) and up
        float mxs = W * 0.575f + offX + kick * 26f, mys = H * 0.6f + offY + sw * H * 0.8f + kick * 40f;
        float left = mxs - mz[0] * k, top = mys - mz[1] * k;
        float grow = 1f + kick * 0.05f;
        gfx.save();
        gfx.translate(mxs, mys);
        gfx.rotate(kick * 0.06f - sw * 0.25f);
        gfx.scale(grow);
        gfx.translate(-mxs, -mys);
        // element glow under the gun
        if (w.element != Loot.E_NONE) gfx.circle(left + S * 0.72f, top + S * 0.72f, S * 0.3f, MathUtil.alpha(Loot.ELEMENT_COLORS[w.element], 0.12f));
        gfx.image(img, 0, 0, 384, 384, left, top, S, S, 1f, Gfx.TINT_NONE, 0);
        gfx.restore();
        // rarity tag in the corner of the gun
        gfx.circle(left + S * 0.93f, top + S * 0.9f, Math.max(4, S * 0.012f), Loot.COLORS[w.rarity]);
        if (kick > 0.65f && w.type.kind != WeaponType.K_ORBIT && w.type.kind != WeaponType.K_MINE) {
            float fl = (0.7f + MathUtil.rand() * 0.6f) * S * 0.16f * (kick - 0.5f) * 2f;
            int fc = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : 0xFFFFD54F;
            gfx.circle(mxs, mys, fl * 0.9f, MathUtil.alpha(fc, 0.35f));
            gfx.circle(mxs, mys, fl * 0.45f, MathUtil.alpha(0xFFFFF8E1, 0.9f));
            for (int q = 0; q < 6; q++) { float a = q * MathUtil.TAU / 6 + g.realTime * 20; gfx.line(mxs, mys, mxs + MathUtil.cos(a) * fl, mys + MathUtil.sin(a) * fl * 0.7f, Math.max(3, S * 0.012f), MathUtil.alpha(fc, 0.7f)); }
        }
        if (g.weapons.beamActive()) {
            int bc = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color;
            gfx.line(mxs, mys, W / 2, H * 0.46f + p.viewPitch, 30, MathUtil.alpha(bc, 0.3f));
            gfx.line(mxs, mys, W / 2, H * 0.46f + p.viewPitch, 10, MathUtil.alpha(0xFFFFFFFF, 0.9f));
        }
        return true;
    }

    /** The gun in your hands. */
    private void drawViewModel(Gfx gfx, float W, float H) {
        Player p = g.player;
        Weapon w = g.weapons.activeWeapon();
        if (w == null || !p.alive) return;
        float speed = MathUtil.len(p.vx, p.vy) / Math.max(1f, p.speed());
        bobPhase += 0.016f * (4f + 9f * speed);
        float dHead = p.view - lastHeading;
        while (dHead > MathUtil.PI) dHead -= MathUtil.TAU;
        while (dHead < -MathUtil.PI) dHead += MathUtil.TAU;
        lastHeading = p.view;
        turnSway += (MathUtil.clamp(-dHead * 600f, -40, 40) - turnSway) * 0.15f;
        float idle = MathUtil.sin(g.realTime * 1.8f) * 4f;
        float bobX = MathUtil.sin(bobPhase) * 14f * speed, bobY = Math.abs(MathUtil.cos(bobPhase)) * 12f * speed;
        float sw = g.weapons.switchTimer > 0 ? g.weapons.switchTimer / Config.SWITCH_DELAY : 0;
        float kick = w.recoil;
        float ox = W * 0.56f + bobX + turnSway + kick * 12f, oy = H * 0.84f + bobY + idle + sw * 300f + kick * 30f;
        float rot = -0.42f - kick * 0.18f + sw * 0.6f;
        if (fpSprite(gfx, w, W, H, bobX + turnSway, bobY + idle, sw, kick, p)) return;
        float sc = 6.6f;
        // forearm + glove holding the grip
        gfx.line(W * 0.98f, H + 40, ox + 6, oy + 30, 64, 0xFF33402A);
        gfx.line(W * 0.98f, H + 40, ox + 6, oy + 30, 46, 0xFF4B5A33);
        gfx.save();
        gfx.translate(ox, oy);
        gfx.rotate(rot);
        g.world.drawGun(gfx, w, sc, kick);
        // muzzle flash
        if (kick > 0.65f && w.type.kind != WeaponType.K_ORBIT && w.type.kind != WeaponType.K_MINE) {
            float mx = 46 * sc, fl = (0.7f + MathUtil.rand() * 0.6f) * 90f * (kick - 0.5f);
            int fc = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : 0xFFFFD54F;
            gfx.circle(mx, 0, fl * 0.9f, MathUtil.alpha(fc, 0.35f));
            gfx.circle(mx, 0, fl * 0.5f, MathUtil.alpha(0xFFFFF8E1, 0.85f));
            for (int k = 0; k < 5; k++) { float a = k * MathUtil.TAU / 5 + g.realTime * 20; gfx.line(mx, 0, mx + MathUtil.cos(a) * fl, MathUtil.sin(a) * fl * 0.6f, 6, MathUtil.alpha(fc, 0.7f)); }
        }
        gfx.restore();
        // support hand under the barrel
        float hx = ox + MathUtil.cos(rot) * 24 * sc - MathUtil.sin(rot) * 8 * sc, hy = oy + MathUtil.sin(rot) * 24 * sc + MathUtil.cos(rot) * 8 * sc;
        gfx.line(W * 0.25f, H + 60, hx, hy + 10, 58, 0xFF33402A);
        gfx.line(W * 0.25f, H + 60, hx, hy + 10, 42, 0xFF4B5A33);
        gfx.circle(hx, hy, 26, 0xFF3E2723);
        // laser beam towards the crosshair
        if (g.weapons.beamActive()) {
            int bc = w.element != Loot.E_NONE ? Loot.ELEMENT_COLORS[w.element] : w.type.color;
            float tx = ox + MathUtil.cos(rot) * 46 * sc, ty = oy + MathUtil.sin(rot) * 46 * sc;
            gfx.line(tx, ty, W / 2, H * 0.46f + p.viewPitch, 30, MathUtil.alpha(bc, 0.3f));
            gfx.line(tx, ty, W / 2, H * 0.46f + p.viewPitch, 10, MathUtil.alpha(0xFFFFFFFF, 0.9f));
        }
    }
}
