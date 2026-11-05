package com.blockkillers.core;

import java.util.ArrayList;

/** All screens (menu, HUD, pause, level-up, game over, settings, statistics), buttons, sliders and notifications. */
public final class UIManager {
    // actions
    public static final int A_PLAY = 1, A_SETTINGS = 2, A_STATS = 3, A_EXIT = 4, A_RESUME = 5, A_RESTART = 6, A_MENU = 7,
            A_BACK = 8, A_PAUSE = 9, A_ABILITY = 10, A_CARD0 = 11, A_CARD1 = 12, A_CARD2 = 13, A_VIB = 14, A_SHAKE = 15,
            A_QUALITY = 16, A_BATTERY = 17, A_LANG = 18, A_MUSIC = 19, A_SFX = 20, A_SENS = 21, A_DMGNUM = 22;

    static final int B_BUTTON = 0, B_SLIDER = 1, B_TOGGLE = 2, B_CYCLE = 3, B_CARD = 4, B_ICON = 5;

    public static final class Button {
        public float l, t, r, b;
        public int action, kind, color;
        public String key;
        public boolean enabled = true;
        Button(int action, int kind, String key, int color) { this.action = action; this.kind = kind; this.key = key; this.color = color; }
        void set(float l, float t, float r, float b) { this.l = l; this.t = t; this.r = r; this.b = b; }
        public boolean hit(float x, float y) { return enabled && x >= l - 6 && x <= r + 6 && y >= t - 6 && y <= b + 6; }
        public float cx() { return (l + r) / 2; }
        public float cy() { return (t + b) / 2; }
    }

    private static final int NEON = 0xFF18FFFF, PINK = 0xFFFF4081, GOLD = 0xFFFFD740, PANEL = 0xE60B1022;

    private final Game g;
    private final ArrayList<Button> menuB = new ArrayList<>(), hudB = new ArrayList<>(), pauseB = new ArrayList<>(),
            levelB = new ArrayList<>(), overB = new ArrayList<>(), setB = new ArrayList<>(), statB = new ArrayList<>();
    private Button pressed;
    private int pressedPointer = -1;
    private Button slider;
    private int sliderPointer = -1;
    private float levelOpenTime;

    // announcements
    private String annTitle, annSub;
    private float annTimer;

    // menu background
    private final float[][] bg = new float[26][6];

    public UIManager(Game g) {
        this.g = g;
        menuB.add(new Button(A_PLAY, B_BUTTON, "play", NEON));
        menuB.add(new Button(A_SETTINGS, B_BUTTON, "settings", 0xFFB388FF));
        menuB.add(new Button(A_STATS, B_BUTTON, "statistics", 0xFFB388FF));
        menuB.add(new Button(A_EXIT, B_BUTTON, "exit", PINK));
        hudB.add(new Button(A_PAUSE, B_ICON, "pause", 0xFFFFFFFF));
        hudB.add(new Button(A_ABILITY, B_ICON, "nova", 0xFF40C4FF));
        pauseB.add(new Button(A_RESUME, B_BUTTON, "resume", NEON));
        pauseB.add(new Button(A_RESTART, B_BUTTON, "restart", 0xFFB388FF));
        pauseB.add(new Button(A_SETTINGS, B_BUTTON, "settings", 0xFFB388FF));
        pauseB.add(new Button(A_MENU, B_BUTTON, "main_menu", PINK));
        levelB.add(new Button(A_CARD0, B_CARD, "", 0));
        levelB.add(new Button(A_CARD1, B_CARD, "", 0));
        levelB.add(new Button(A_CARD2, B_CARD, "", 0));
        overB.add(new Button(A_RESTART, B_BUTTON, "restart", NEON));
        overB.add(new Button(A_MENU, B_BUTTON, "main_menu", PINK));
        setB.add(new Button(A_MUSIC, B_SLIDER, "music_volume", NEON));
        setB.add(new Button(A_SFX, B_SLIDER, "sfx_volume", NEON));
        setB.add(new Button(A_SENS, B_SLIDER, "sensitivity", NEON));
        setB.add(new Button(A_VIB, B_TOGGLE, "vibration", 0xFFB388FF));
        setB.add(new Button(A_SHAKE, B_TOGGLE, "screen_shake", 0xFFB388FF));
        setB.add(new Button(A_DMGNUM, B_TOGGLE, "damage_numbers", 0xFFB388FF));
        setB.add(new Button(A_QUALITY, B_CYCLE, "graphics", GOLD));
        setB.add(new Button(A_BATTERY, B_CYCLE, "performance", GOLD));
        setB.add(new Button(A_LANG, B_CYCLE, "language", GOLD));
        setB.add(new Button(A_BACK, B_BUTTON, "back", PINK));
        statB.add(new Button(A_BACK, B_BUTTON, "back", PINK));
        for (float[] s : bg) resetBg(s, true);
    }

    private void resetBg(float[] s, boolean anywhere) {
        s[0] = MathUtil.rand(0, Config.VIRTUAL_WIDTH);
        s[1] = anywhere ? MathUtil.rand(0, 1800) : -80;
        s[2] = MathUtil.rand(14, 46);          // size
        s[3] = MathUtil.rand(25, 90);          // speed
        s[4] = MathUtil.rand(0, MathUtil.TAU); // rotation
        s[5] = MathUtil.randInt(9);            // enemy type for colour/shape
    }

    // ------------------------------------------------------------------ layout
    public void layout() {
        float W = g.viewW, H = g.viewH;
        float bw = 460, bh = 92, y = Math.max(H * 0.47f, 560);
        for (int i = 0; i < menuB.size(); i++) {
            Button b = menuB.get(i);
            float h = i == 0 ? bh + 18 : bh;
            b.set(W / 2 - bw / 2, y, W / 2 + bw / 2, y + h);
            y += h + 22;
        }
        hudB.get(0).set(W - 96, 46, W - 22, 120);
        hudB.get(1).set(W - 190, H - 210, W - 40, H - 60);
        y = H * 0.42f;
        for (Button b : pauseB) { b.set(W / 2 - bw / 2, y, W / 2 + bw / 2, y + bh); y += bh + 20; }
        float ch = Math.min(200, (H * 0.62f) / 3f - 20);
        y = H * 0.25f;
        for (Button b : levelB) { b.set(40, y, W - 40, y + ch); y += ch + 22; }
        y = H * 0.70f;
        for (Button b : overB) { b.set(W / 2 - bw / 2, y, W / 2 + bw / 2, y + bh); y += bh + 20; }
        float row = Math.min(92, (H - 330) / 10f);
        y = 150;
        for (int i = 0; i < setB.size() - 1; i++) { setB.get(i).set(40, y, W - 40, y + row - 12); y += row; }
        setB.get(setB.size() - 1).set(W / 2 - 180, H - 130, W / 2 + 180, H - 50);
        statB.get(0).set(W / 2 - 180, H - 120, W / 2 + 180, H - 44);
    }

    private ArrayList<Button> buttons() {
        switch (g.state) {
            case Game.S_MENU: return menuB;
            case Game.S_PLAYING: return hudB;
            case Game.S_PAUSED: return pauseB;
            case Game.S_LEVELUP: return levelB;
            case Game.S_GAMEOVER: return overB;
            case Game.S_SETTINGS: return setB;
            case Game.S_STATS: return statB;
            default: return null;
        }
    }

    // Accessors (used by the automated test harness to tap real button positions)
    public Button menuButton(int i) { return menuB.get(i); }
    public Button hudButton(int i) { return hudB.get(i); }
    public Button pauseButton(int i) { return pauseB.get(i); }
    public Button levelButton(int i) { return levelB.get(i); }
    public Button overButton(int i) { return overB.get(i); }
    public Button settingsButton(int i) { return setB.get(i); }
    public Button statsButton(int i) { return statB.get(i); }

    public void openLevelUp() {
        levelOpenTime = g.realTime;
        pressed = null; pressedPointer = -1;
        for (int i = 0; i < 3; i++) levelB.get(i).enabled = i < g.upgrades.cardCount;
    }

    public void announce(String title, String sub) { annTitle = title; annSub = sub; annTimer = 2.6f; }

    // ------------------------------------------------------------------ input
    public boolean touchDown(int id, float x, float y) {
        ArrayList<Button> bs = buttons();
        if (bs == null) return false;
        menuB.get(3).enabled = g.platform.canExit();
        if (g.state == Game.S_LEVELUP && g.realTime - levelOpenTime < 0.45f) return true;
        if (g.state == Game.S_GAMEOVER && g.gameOverTimer < 0.8f) return true;
        for (Button b : bs) {
            if (!b.hit(x, y)) continue;
            if (b.kind == B_SLIDER) { slider = b; sliderPointer = id; setSlider(b, x); return true; }
            if (b.action == A_ABILITY) { g.playerCtl.useAbility(); return true; }
            pressed = b; pressedPointer = id;
            return true;
        }
        // in gameplay, touches that miss the HUD buttons control the joystick
        return g.state != Game.S_PLAYING;
    }

    public void touchMove(int id, float x, float y) {
        if (slider != null && id == sliderPointer) setSlider(slider, x);
    }

    public void touchUp(int id, float x, float y) {
        if (slider != null && id == sliderPointer) {
            slider = null; sliderPointer = -1;
            g.settings.save();
            g.audio.play(Sfx.CLICK);
            return;
        }
        if (pressed != null && id == pressedPointer) {
            Button b = pressed;
            pressed = null; pressedPointer = -1;
            if (b.hit(x, y) && buttons() != null && buttons().contains(b)) action(b.action);
        }
    }

    /** Slider track bounds; mirrored to the left side for right-to-left languages. */
    private float sliderL(Button b) { return g.strings.rtl() ? b.l + 30 : b.l + (b.r - b.l) * 0.45f; }
    private float sliderR(Button b) { return sliderL(b) + (b.r - b.l) * 0.5f; }

    private void setSlider(Button b, float x) {
        float v = MathUtil.clamp((x - sliderL(b)) / (sliderR(b) - sliderL(b)), 0f, 1f);
        SettingsManager s = g.settings;
        switch (b.action) {
            case A_MUSIC: s.musicVolume = v; g.audio.refreshVolume(); break;
            case A_SFX: s.sfxVolume = v; break;
            case A_SENS: s.sensitivity = 0.5f + v * 1.5f; break;
        }
    }

    private float sliderValue(int action) {
        SettingsManager s = g.settings;
        switch (action) {
            case A_MUSIC: return s.musicVolume;
            case A_SFX: return s.sfxVolume;
            default: return (s.sensitivity - 0.5f) / 1.5f;
        }
    }

    /** Executes a UI action (public so automated tests can drive the UI). */
    public void action(int a) {
        if (a != A_PAUSE) g.audio.play(Sfx.CLICK);
        SettingsManager s = g.settings;
        switch (a) {
            case A_PLAY: g.startRun(); break;
            case A_SETTINGS: g.openSettings(); break;
            case A_STATS: g.state = Game.S_STATS; break;
            case A_EXIT: g.platform.exitApp(); break;
            case A_RESUME: g.resume(); break;
            case A_RESTART: g.restart(); break;
            case A_MENU: g.toMenu(); break;
            case A_BACK: if (g.state == Game.S_SETTINGS) g.closeSettings(); else g.state = Game.S_MENU; break;
            case A_PAUSE: g.pause(); break;
            case A_CARD0: g.chooseUpgrade(0); break;
            case A_CARD1: g.chooseUpgrade(1); break;
            case A_CARD2: g.chooseUpgrade(2); break;
            case A_VIB: s.vibration = !s.vibration; if (s.vibration) g.platformVibrate(40); s.save(); break;
            case A_SHAKE: s.screenShake = !s.screenShake; s.save(); break;
            case A_DMGNUM: s.showDamageNumbers = !s.showDamageNumbers; s.save(); break;
            case A_QUALITY: s.quality = (s.quality + 1) % 3; g.fx.adaptive = 1f; s.save(); break;
            case A_BATTERY: s.batterySaver = !s.batterySaver; s.save(); break;
            case A_LANG: s.language = (s.language + 1) % 2; s.save(); break;
        }
    }

    // ------------------------------------------------------------------ update
    public void update(float dt) { if (annTimer > 0) annTimer -= dt; }

    public void updateMenuBackground(float dt) {
        for (float[] s : bg) {
            s[1] += s[3] * dt;
            s[4] += dt * 0.6f;
            if (s[1] > g.viewH + 80) resetBg(s, false);
        }
    }

    // ------------------------------------------------------------------ render
    public void renderMenuBackground(Gfx gfx) {
        gfx.clear(0xFF070B16);
        float W = g.viewW, H = g.viewH;
        for (float y = 0; y < H; y += 80) gfx.line(0, y, W, y, 1, 0x1418FFFF);
        for (float x = 0; x < W; x += 80) gfx.line(x, 0, x, H, 1, 0x1418FFFF);
        for (float[] s : bg) {
            EnemyType et = Config.ENEMIES[(int) s[5]];
            Shapes.draw(gfx, et.shape, s[0], s[1], s[2] * 1.4f, s[4], MathUtil.alpha(et.color, 0.10f));
            Shapes.draw(gfx, et.shape, s[0], s[1], s[2], s[4], MathUtil.alpha(et.color, 0.45f));
        }
    }

    public void render(Gfx gfx) {
        switch (g.state) {
            case Game.S_MENU: renderMenu(gfx); break;
            case Game.S_PLAYING: renderHud(gfx); break;
            case Game.S_PAUSED: renderHud(gfx); renderPause(gfx); break;
            case Game.S_LEVELUP: renderHud(gfx); renderLevelUp(gfx); break;
            case Game.S_GAMEOVER: renderGameOver(gfx); break;
            case Game.S_SETTINGS: dim(gfx, g.settingsReturn != Game.S_MENU ? 0xEE070B16 : 0xB3070B16); renderSettings(gfx); break;
            case Game.S_STATS: dim(gfx, 0xB3070B16); renderStats(gfx); break;
        }
        renderToast(gfx);
    }

    private String s(String key) { return g.strings.get(key); }

    private void dim(Gfx gfx, int c) { gfx.rect(0, 0, g.viewW, g.viewH, c); }

    private void neonText(Gfx gfx, String t, float x, float y, float size, int color) {
        gfx.text(t, x, y + 4, size, MathUtil.alpha(color, 0.25f), Gfx.ALIGN_CENTER, true);
        gfx.text(t, x + 2, y + 2, size, 0xFF000000, Gfx.ALIGN_CENTER, true);
        gfx.text(t, x, y, size, color, Gfx.ALIGN_CENTER, true);
    }

    private void drawButton(Gfx gfx, Button b) {
        boolean down = b == pressed;
        int fill = down ? MathUtil.alpha(b.color, 0.35f) : 0xCC0E1530;
        if (!b.enabled) fill = 0x660E1530;
        gfx.roundRect(b.l, b.t, b.r, b.b, 18, fill);
        gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 18, 3, b.enabled ? b.color : 0x55FFFFFF);
        float size = Math.min(38, (b.b - b.t) * 0.42f);
        gfx.text(s(b.key), b.cx(), b.cy() + size * 0.36f, size, b.enabled ? 0xFFFFFFFF : 0x77FFFFFF, Gfx.ALIGN_CENTER, true);
    }

    private void renderMenu(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        float ly = Math.max(H * 0.2f, 230);
        float bob = MathUtil.sin(g.realTime * 2f) * 6f;
        // logo emblem
        Shapes.draw(gfx, Shapes.SQUARE, W / 2 - 150, ly - 120 + bob, 26, g.realTime, 0xFFFF3D6E);
        Shapes.draw(gfx, Shapes.TRIANGLE, W / 2 + 150, ly - 120 - bob, 26, -g.realTime, 0xFFFFB300);
        gfx.circle(W / 2, ly - 125, 34, 0x3340C4FF);
        gfx.circle(W / 2, ly - 125, 24, 0xFF40C4FF);
        gfx.circle(W / 2, ly - 125, 15, 0xFFFFFFFF);
        neonText(gfx, "BLOCK", W / 2, ly, 104, NEON);
        neonText(gfx, "KILLERS", W / 2, ly + 100, 104, PINK);
        gfx.text(s("tagline"), W / 2, ly + 172, 26, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
        gfx.text(s("best_score") + ": " + g.save.bestScore, W / 2, ly + 232, 34, GOLD, Gfx.ALIGN_CENTER, true);
        menuB.get(3).enabled = g.platform.canExit();
        for (Button b : menuB) if (b.action != A_EXIT || b.enabled) drawButton(gfx, b);
        gfx.text("v1.0", W / 2, H - 30, 20, 0x66FFFFFF, Gfx.ALIGN_CENTER, false);
    }

    private void bar(Gfx gfx, float l, float t, float r, float b, float k, int col, int back) {
        gfx.roundRect(l, t, r, b, (b - t) / 2, back);
        if (k > 0) gfx.roundRect(l, t, l + Math.max(b - t, (r - l) * MathUtil.clamp(k, 0, 1)), b, (b - t) / 2, col);
    }

    private void renderHud(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        Player p = g.player;
        // XP bar (top)
        bar(gfx, 20, 14, W - 20, 34, g.xp.progress(), 0xFF7C4DFF, 0x66000000);
        gfx.text(s("lv") + " " + p.level, 32, 31, 17, 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
        // health
        float hpK = p.hp / p.maxHp;
        int hpCol = hpK > 0.5f ? 0xFF00E676 : (hpK > 0.25f ? 0xFFFFC400 : 0xFFFF1744);
        bar(gfx, 20, 48, 330, 76, hpK, hpCol, 0x88000000);
        gfx.text(Math.round(p.hp) + " / " + Math.round(p.maxHp), 175, 70, 20, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
        // timer, kills, score
        gfx.text(MathUtil.time(g.time), W / 2 + 40, 112, 40, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
        gfx.text(s("kills") + " " + g.kills, W - 110, 72, 22, 0xFFFF80AB, Gfx.ALIGN_RIGHT, true);
        gfx.text(s("score") + " " + g.score, W - 110, 102, 22, GOLD, Gfx.ALIGN_RIGHT, true);
        // weapons
        float wx = 24;
        for (int i = 0; i < g.weapons.owned.size(); i++) {
            Weapon w = g.weapons.owned.get(i);
            gfx.roundRect(wx, 88, wx + 44, 132, 10, 0x99000000);
            gfx.strokeRoundRect(wx, 88, wx + 44, 132, 10, 2, w.type.color);
            weaponIcon(gfx, w.type.id, wx + 22, 106, 13, w.type.color);
            gfx.text(Integer.toString(w.level), wx + 22, 130, 14, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
            wx += 50;
        }
        // boss bar
        Enemy boss = g.bosses.current;
        if (boss != null && boss.alive) {
            int c = g.bosses.color(boss);
            gfx.text(g.bosses.name(boss), W / 2, 168, 26, c, Gfx.ALIGN_CENTER, true);
            bar(gfx, 60, 178, W - 60, 200, boss.hp / boss.maxHp, c, 0xAA000000);
        } else if (g.bosses.warning > 0) {
            float a = 0.5f + 0.5f * MathUtil.sin(g.realTime * 14);
            gfx.text(s("boss_incoming"), W / 2, 190, 34, MathUtil.alpha(0xFFFF1744, a), Gfx.ALIGN_CENTER, true);
        }
        // pause button
        Button pb = hudB.get(0);
        gfx.roundRect(pb.l, pb.t, pb.r, pb.b, 14, pb == pressed ? 0x66FFFFFF : 0x55000000);
        gfx.rect(pb.cx() - 14, pb.t + 20, pb.cx() - 5, pb.b - 20, 0xFFFFFFFF);
        gfx.rect(pb.cx() + 5, pb.t + 20, pb.cx() + 14, pb.b - 20, 0xFFFFFFFF);
        // ability button
        Button ab = hudB.get(1);
        float r = (ab.r - ab.l) / 2f;
        float cd = g.playerCtl.abilityCooldown;
        boolean ready = cd <= 0;
        gfx.circle(ab.cx(), ab.cy(), r, ready ? 0x5540C4FF : 0x66000000);
        if (!ready) gfx.arc(ab.cx(), ab.cy(), r, -MathUtil.PI / 2, MathUtil.TAU * (1 - cd / Config.NOVA_COOLDOWN), 0x4440C4FF);
        gfx.ring(ab.cx(), ab.cy(), r, 4, ready ? 0xFF40C4FF : 0x8840C4FF);
        if (ready) gfx.ring(ab.cx(), ab.cy(), r + 6 + 4 * MathUtil.sin(g.realTime * 5), 2, 0x6640C4FF);
        for (int i = 0; i < 8; i++) {
            float a = i * MathUtil.TAU / 8 + g.realTime * (ready ? 1.5f : 0);
            gfx.line(ab.cx() + MathUtil.cos(a) * 14, ab.cy() + MathUtil.sin(a) * 14, ab.cx() + MathUtil.cos(a) * 36, ab.cy() + MathUtil.sin(a) * 36, 4, ready ? 0xFFFFFFFF : 0x66FFFFFF);
        }
        gfx.text(ready ? s("nova") : Integer.toString((int) Math.ceil(cd)), ab.cx(), ab.b + 30, 22, ready ? 0xFF40C4FF : 0x99FFFFFF, Gfx.ALIGN_CENTER, true);
        // joystick
        Joystick j = g.joystick;
        if (j.active) {
            gfx.circle(j.baseX, j.baseY, j.radius, 0x22FFFFFF);
            gfx.ring(j.baseX, j.baseY, j.radius, 3, 0x66FFFFFF);
            float kx = j.baseX + j.outX * j.radius, ky = j.baseY + j.outY * j.radius;
            gfx.circle(kx, ky, 34, 0x9940C4FF);
            gfx.ring(kx, ky, 34, 3, 0xFFFFFFFF);
        } else if (g.state == Game.S_PLAYING && g.time < 8) {
            float x = 170, y = H - 200;
            gfx.ring(x, y, 85, 3, 0x33FFFFFF);
            gfx.circle(x, y, 34, 0x2240C4FF);
            gfx.text(s("drag_to_move"), x, y + 130, 22, 0x99FFFFFF, Gfx.ALIGN_CENTER, true);
        }
        // announcement
        if (annTimer > 0 && annTitle != null) {
            float k = Math.min(1f, Math.min(annTimer, 2.6f - annTimer) * 3f);
            neonText(gfx, annTitle, W / 2, H * 0.36f, 54, MathUtil.alpha(0xFFFFFFFF, k));
            if (annSub != null) gfx.text(annSub, W / 2, H * 0.36f + 50, 28, MathUtil.alpha(GOLD, k), Gfx.ALIGN_CENTER, true);
        }
    }

    /** Small symbolic icon for each weapon. */
    public void weaponIcon(Gfx gfx, int id, float x, float y, float r, int c) {
        switch (id) {
            case Config.W_BLASTER: gfx.circle(x, y, r * 0.5f, c); break;
            case Config.W_TWIN: gfx.circle(x - r * 0.45f, y, r * 0.38f, c); gfx.circle(x + r * 0.45f, y, r * 0.38f, c); break;
            case Config.W_TRI: for (int i = -1; i <= 1; i++) gfx.line(x, y + r * 0.7f, x + i * r * 0.7f, y - r * 0.7f, 3, c); break;
            case Config.W_PIERCER: gfx.line(x - r, y + r * 0.5f, x + r, y - r * 0.5f, 4, c); break;
            case Config.W_ROCKET: gfx.circle(x, y, r * 0.65f, c); gfx.circle(x, y, r * 0.3f, 0xFFFF6D00); break;
            case Config.W_BLADES: gfx.ring(x, y, r * 0.7f, 2, c); gfx.circle(x + r * 0.7f, y, r * 0.3f, c); gfx.circle(x - r * 0.7f, y, r * 0.3f, c); break;
            case Config.W_LIGHTNING: gfx.line(x + r * 0.3f, y - r, x - r * 0.3f, y, 3, c); gfx.line(x - r * 0.3f, y, x + r * 0.3f, y, 3, c); gfx.line(x + r * 0.3f, y, x - r * 0.3f, y + r, 3, c); break;
            default: gfx.ring(x, y, r * 0.5f, 2, c); gfx.ring(x, y, r * 0.9f, 2, c);
        }
    }

    private void renderPause(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        dim(gfx, 0xCC070B16);
        neonText(gfx, s("paused"), W / 2, H * 0.24f, 80, NEON);
        gfx.text(MathUtil.time(g.time) + "   " + s("kills") + " " + g.kills + "   " + s("lv") + " " + g.player.level, W / 2, H * 0.24f + 60, 26, 0xFFB0BEC5, Gfx.ALIGN_CENTER, true);
        gfx.text(s("score") + " " + g.score, W / 2, H * 0.24f + 100, 28, GOLD, Gfx.ALIGN_CENTER, true);
        for (Button b : pauseB) drawButton(gfx, b);
    }

    private void renderLevelUp(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        dim(gfx, 0xD9070B16);
        UpgradeManager um = g.upgrades;
        neonText(gfx, s(um.bossReward ? "boss_reward" : "level_up"), W / 2, H * 0.14f, 72, um.bossReward ? GOLD : NEON);
        gfx.text(s("choose_upgrade"), W / 2, H * 0.14f + 52, 26, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
        float appear = MathUtil.clamp((g.realTime - levelOpenTime) * 4f, 0, 1);
        boolean rtl = g.strings.rtl();
        for (int i = 0; i < um.cardCount; i++) {
            Button b = levelB.get(i);
            UpgradeManager.Card c = um.cards[i];
            int rc = c.type == UpgradeManager.T_STAT ? Config.RARITY_COLORS[c.rarity] : 0xFF64FFDA;
            float off = (1 - appear) * (300 + i * 120);
            float l = b.l + off, r = b.r + off;
            gfx.roundRect(l, b.t, r, b.b, 20, b == pressed ? MathUtil.alpha(rc, 0.3f) : 0xF0101830);
            gfx.strokeRoundRect(l, b.t, r, b.b, 20, c.rarity >= 2 ? 5 : 3, rc);
            if (c.rarity == 3) gfx.strokeRoundRect(l - 6, b.t - 6, r + 6, b.b + 6, 24, 2, MathUtil.alpha(rc, 0.4f + 0.3f * MathUtil.sin(g.realTime * 6)));
            float h = b.b - b.t;
            float iconX = rtl ? r - 70 : l + 70;
            gfx.circle(iconX, b.t + h / 2, 44, MathUtil.alpha(rc, 0.18f));
            if (c.type == UpgradeManager.T_STAT) statIcon(gfx, c.stat, iconX, b.t + h / 2, 26, rc);
            else weaponIcon(gfx, c.weapon, iconX, b.t + h / 2, 26, Config.WEAPONS[c.weapon].color);
            float tx = rtl ? r - 140 : l + 140;
            int al = rtl ? Gfx.ALIGN_RIGHT : Gfx.ALIGN_LEFT;
            String rarity = c.type == UpgradeManager.T_STAT ? s(Config.RARITY_KEYS[c.rarity]) : s(c.type == UpgradeManager.T_NEW_WEAPON ? "weapon_new" : "weapon_up");
            gfx.text(rarity.toUpperCase(), tx, b.t + h * 0.30f, 18, rc, al, true);
            gfx.text(um.title(c), tx, b.t + h * 0.56f, 32, 0xFFFFFFFF, al, true);
            gfx.text(um.describe(c), tx, b.t + h * 0.82f, 22, 0xFFCFD8DC, al, false);
        }
    }

    /** Icons for stat upgrades. */
    private void statIcon(Gfx gfx, int stat, float x, float y, float r, int c) {
        switch (stat) {
            case UpgradeManager.DAMAGE: Shapes.draw(gfx, Shapes.DIAMOND, x, y, r * 0.8f, 0.785f, c); break;
            case UpgradeManager.ATTACK_SPEED: for (int i = 0; i < 3; i++) gfx.line(x - r * 0.6f + i * r * 0.5f, y - r * 0.6f, x - r * 0.2f + i * r * 0.5f, y, 4, c); for (int i = 0; i < 3; i++) gfx.line(x - r * 0.2f + i * r * 0.5f, y, x - r * 0.6f + i * r * 0.5f, y + r * 0.6f, 4, c); break;
            case UpgradeManager.MOVE_SPEED: gfx.circle(x + r * 0.3f, y, r * 0.4f, c); for (int i = -1; i <= 1; i++) gfx.line(x - r, y + i * r * 0.35f, x - r * 0.2f, y + i * r * 0.35f, 3, c); break;
            case UpgradeManager.MAX_HP: case UpgradeManager.RECOVERY:
                gfx.rect(x - r * 0.2f, y - r * 0.7f, x + r * 0.2f, y + r * 0.7f, c); gfx.rect(x - r * 0.7f, y - r * 0.2f, x + r * 0.7f, y + r * 0.2f, c); break;
            case UpgradeManager.CRIT: gfx.ring(x, y, r * 0.6f, 3, c); gfx.line(x - r, y, x + r, y, 3, c); gfx.line(x, y - r, x, y + r, 3, c); break;
            case UpgradeManager.ARMOR: Shapes.draw(gfx, Shapes.PENTAGON, x, y, r * 0.8f, MathUtil.PI, c); break;
            case UpgradeManager.RANGE: gfx.ring(x, y, r * 0.35f, 3, c); gfx.ring(x, y, r * 0.75f, 3, c); break;
            case UpgradeManager.XP_BOOST: Shapes.draw(gfx, Shapes.HEXAGON, x, y, r * 0.7f, 0, c); break;
            case UpgradeManager.PROJ_SPEED: gfx.line(x - r, y, x + r * 0.6f, y, 4, c); gfx.circle(x + r * 0.6f, y, r * 0.3f, c); break;
            case UpgradeManager.EXTRA_PROJ: gfx.circle(x - r * 0.5f, y, r * 0.28f, c); gfx.circle(x, y, r * 0.28f, c); gfx.circle(x + r * 0.5f, y, r * 0.28f, c); break;
            default: Shapes.draw(gfx, Shapes.TRIANGLE, x, y, r * 0.8f, 0, c);
        }
    }

    private void renderGameOver(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        dim(gfx, 0xD9070B16);
        float k = MathUtil.clamp(g.gameOverTimer * 2.5f, 0, 1);
        neonText(gfx, s("game_over"), W / 2, H * 0.14f, 84, MathUtil.alpha(PINK, k));
        if (g.newBest) {
            float a = 0.6f + 0.4f * MathUtil.sin(g.realTime * 6);
            gfx.text(s("new_best"), W / 2, H * 0.14f + 56, 34, MathUtil.alpha(GOLD, a * k), Gfx.ALIGN_CENTER, true);
        }
        float top = H * 0.22f, bottom = H * 0.66f;
        gfx.roundRect(50, top, W - 50, bottom, 24, PANEL);
        gfx.strokeRoundRect(50, top, W - 50, bottom, 24, 2, 0x5518FFFF);
        String[] labels = {s("final_score"), s("survival_time"), s("total_kills"), s("level_reached"), s("bosses_defeated"), s("best_score")};
        String[] vals = {Integer.toString(g.score), MathUtil.time(g.time), Integer.toString(g.kills), Integer.toString(g.player.level),
                Integer.toString(g.bossesKilled), Integer.toString(g.save.bestScore)};
        boolean rtl = g.strings.rtl();
        float rowH = (bottom - top - 120) / labels.length;
        float y = top + 50;
        for (int i = 0; i < labels.length; i++) {
            int col = i == 0 ? GOLD : 0xFFFFFFFF;
            gfx.text(labels[i], rtl ? W - 90 : 90, y + 10, i == 0 ? 32 : 27, 0xFFB0BEC5, rtl ? Gfx.ALIGN_RIGHT : Gfx.ALIGN_LEFT, false);
            gfx.text(vals[i], rtl ? 90 : W - 90, y + 10, i == 0 ? 38 : 29, col, rtl ? Gfx.ALIGN_LEFT : Gfx.ALIGN_RIGHT, true);
            y += rowH;
        }
        // enemies defeated breakdown (as coloured shapes with counts)
        gfx.text(s("enemies_defeated"), W / 2, bottom - 70, 22, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
        int shown = 0;
        for (int i = 0; i < g.killsByType.length; i++) if (g.killsByType[i] > 0) shown++;
        float x = W / 2 - (shown - 1) * 29;
        for (int i = 0; i < g.killsByType.length; i++) {
            if (g.killsByType[i] <= 0) continue;
            EnemyType et = Config.ENEMIES[i];
            Shapes.draw(gfx, et.shape, x, bottom - 42, 11, 0, et.color);
            gfx.text(Integer.toString(g.killsByType[i]), x, bottom - 12, 16, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
            x += 58;
        }
        for (Button b : overB) { b.enabled = g.gameOverTimer >= 0.8f; drawButton(gfx, b); }
    }

    private void renderSettings(Gfx gfx) {
        float W = g.viewW;
        neonText(gfx, s("settings"), W / 2, 100, 64, NEON);
        SettingsManager st = g.settings;
        boolean rtl = g.strings.rtl();
        for (Button b : setB) {
            if (b.kind == B_BUTTON) { drawButton(gfx, b); continue; }
            gfx.roundRect(b.l, b.t, b.r, b.b, 14, 0x990E1530);
            float cy = b.cy();
            float lx = rtl ? b.r - 20 : b.l + 20;
            gfx.text(s(b.key), lx, cy + 10, 27, 0xFFFFFFFF, rtl ? Gfx.ALIGN_RIGHT : Gfx.ALIGN_LEFT, true);
            float cl = b.l + (b.r - b.l) * 0.45f, cr = b.r - 20;
            if (rtl) { cl = b.l + 20; cr = b.l + (b.r - b.l) * 0.55f; }
            if (b.kind == B_SLIDER) {
                float sl = sliderL(b), sr = sliderR(b);
                float v = sliderValue(b.action);
                bar(gfx, sl, cy - 6, sr, cy + 6, v, b.color, 0x55FFFFFF);
                gfx.circle(sl + (sr - sl) * v, cy, 16, 0xFFFFFFFF);
                String val = b.action == A_SENS ? String.format(java.util.Locale.US, "%.1fx", st.sensitivity) : Math.round(v * 100) + "%";
                gfx.text(val, sl + (sr - sl) * v, cy - 22, 17, 0xCCFFFFFF, Gfx.ALIGN_CENTER, true);
            } else if (b.kind == B_TOGGLE) {
                boolean on = b.action == A_VIB ? st.vibration : (b.action == A_SHAKE ? st.screenShake : st.showDamageNumbers);
                float tx = rtl ? cl + 50 : cr - 50;
                gfx.roundRect(tx - 46, cy - 22, tx + 46, cy + 22, 22, on ? MathUtil.alpha(b.color, 0.7f) : 0x55FFFFFF);
                gfx.circle(on ? tx + 24 : tx - 24, cy, 18, 0xFFFFFFFF);
                gfx.text(s(on ? "on" : "off"), rtl ? tx + 70 : tx - 70, cy + 8, 21, 0xCCFFFFFF, rtl ? Gfx.ALIGN_LEFT : Gfx.ALIGN_RIGHT, true);
            } else if (b.kind == B_CYCLE) {
                String v;
                if (b.action == A_QUALITY) v = s(st.quality == 0 ? "low" : (st.quality == 1 ? "medium" : "high"));
                else if (b.action == A_BATTERY) v = s(st.batterySaver ? "battery_saver" : "fps60");
                else v = st.language == SettingsManager.LANG_FA ? "فارسی" : "English";
                float tx = rtl ? cl + 110 : cr - 110;
                gfx.roundRect(tx - 115, cy - 24, tx + 115, cy + 24, 12, b == pressed ? 0x66FFFFFF : 0x33FFD740);
                gfx.text(v, tx, cy + 8, 23, b.color, Gfx.ALIGN_CENTER, true);
                Shapes.draw(gfx, Shapes.TRIANGLE, tx - 100, cy, 7, -MathUtil.PI / 2, b.color);
                Shapes.draw(gfx, Shapes.TRIANGLE, tx + 100, cy, 7, MathUtil.PI / 2, b.color);
            }
        }
    }

    private void renderStats(Gfx gfx) {
        float W = g.viewW;
        neonText(gfx, s("statistics"), W / 2, 90, 60, NEON);
        SaveManager sv = g.save;
        boolean rtl = g.strings.rtl();
        String[] labels = {s("best_score"), s("games_played"), s("total_kills"), s("total_time"), s("best_time"), s("highest_level"), s("bosses_defeated")};
        String[] vals = {Integer.toString(sv.bestScore), Integer.toString(sv.gamesPlayed), Long.toString(sv.totalKills),
                MathUtil.time(sv.totalTime), MathUtil.time(sv.bestTime), Integer.toString(sv.highestLevel), Integer.toString(sv.bossesDefeated)};
        float y = 140;
        float rowH = Math.min(40, (g.viewH - 760) / 7f);
        for (int i = 0; i < labels.length; i++) {
            gfx.text(labels[i], rtl ? W - 60 : 60, y + 26, 25, 0xFFB0BEC5, rtl ? Gfx.ALIGN_RIGHT : Gfx.ALIGN_LEFT, false);
            gfx.text(vals[i], rtl ? 60 : W - 60, y + 26, 26, 0xFFFFFFFF, rtl ? Gfx.ALIGN_LEFT : Gfx.ALIGN_RIGHT, true);
            y += rowH;
        }
        y += 18;
        gfx.text(s("achievements") + "  " + g.achievements.unlockedCount() + "/" + AchievementManager.COUNT, W / 2, y + 20, 30, GOLD, Gfx.ALIGN_CENTER, true);
        y += 40;
        float ah = Math.min(46, (g.viewH - 160 - y) / AchievementManager.COUNT);
        for (int i = 0; i < AchievementManager.COUNT; i++) {
            boolean un = g.achievements.unlocked(i);
            gfx.roundRect(40, y + 2, W - 40, y + ah - 4, 10, un ? 0x4400E676 : 0x33FFFFFF);
            float ix = rtl ? W - 66 : 66;
            if (un) gfx.circle(ix, y + ah / 2, 11, 0xFF00E676); else gfx.ring(ix, y + ah / 2, 11, 2, 0x88FFFFFF);
            gfx.text(s(AchievementManager.KEYS[i]), rtl ? W - 92 : 92, y + ah / 2 + 8, 21, un ? 0xFFFFFFFF : 0x99FFFFFF, rtl ? Gfx.ALIGN_RIGHT : Gfx.ALIGN_LEFT, true);
            gfx.text(s(AchievementManager.BONUS_KEYS[i]), rtl ? 60 : W - 60, y + ah / 2 + 7, 18, un ? 0xFF69F0AE : 0x66FFFFFF, rtl ? Gfx.ALIGN_LEFT : Gfx.ALIGN_RIGHT, false);
            y += ah;
        }
        for (Button b : statB) drawButton(gfx, b);
    }

    private void renderToast(Gfx gfx) {
        AchievementManager am = g.achievements;
        if (am.toastId < 0) return;
        float t = am.toastTimer[0];
        float k = Math.min(1f, Math.min(t, 3f - t) * 4f);
        float W = g.viewW;
        float y = 220 - (1 - k) * 260;
        gfx.roundRect(70, y, W - 70, y + 92, 18, 0xF0102018);
        gfx.strokeRoundRect(70, y, W - 70, y + 92, 18, 3, 0xFF00E676);
        gfx.circle(122, y + 46, 26, 0xFF00E676);
        gfx.text("★", 122, y + 57, 30, 0xFF0B1022, Gfx.ALIGN_CENTER, true);
        gfx.text(s("achievement_unlocked"), 165, y + 36, 19, 0xFF69F0AE, Gfx.ALIGN_LEFT, true);
        gfx.text(s(AchievementManager.KEYS[am.toastId]), 165, y + 72, 28, 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
    }
}
