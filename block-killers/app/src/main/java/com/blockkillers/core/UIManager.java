package com.blockkillers.core;

import java.util.ArrayList;

/** All screens (menu, HUD, pause, level-up, game over, settings, statistics), buttons, sliders and notifications. */
public final class UIManager {
    // actions
    public static final int A_PLAY = 1, A_SETTINGS = 2, A_STATS = 3, A_EXIT = 4, A_RESUME = 5, A_RESTART = 6, A_MENU = 7,
            A_BACK = 8, A_PAUSE = 9, A_ABILITY = 10, A_CARD0 = 11, A_CARD1 = 12, A_CARD2 = 13, A_VIB = 14, A_SHAKE = 15,
            A_QUALITY = 16, A_BATTERY = 17, A_LANG = 18, A_MUSIC = 19, A_SFX = 20, A_SENS = 21, A_DMGNUM = 22,
            A_MAP = 23, A_PICKUP = 24, A_SLOT0 = 25, A_LOOT_SLOT0 = 30, A_LOOT_SCRAP = 35, A_LOOT_LEAVE = 36, A_MAP_BACK = 37,
            A_SHOP = 40, A_DEV = 41, A_MAPSEL0 = 42, A_MEME = 45, A_SHOP_ROW0 = 50, A_DEV_ROW0 = 70;

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
            levelB = new ArrayList<>(), lootB = new ArrayList<>(), mapB = new ArrayList<>(), mapSelB = new ArrayList<>(), shopB = new ArrayList<>(), devB = new ArrayList<>(), overB = new ArrayList<>(), setB = new ArrayList<>(), statB = new ArrayList<>();
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
        menuB.add(new Button(A_SHOP, B_BUTTON, "shop", GOLD));
        menuB.add(new Button(A_DEV, B_BUTTON, "dev_menu", 0xFF69F0AE));
        for (int i = 0; i < WorldMap.MODES; i++) mapSelB.add(new Button(A_MAPSEL0 + i, B_CARD, "", NEON));
        mapSelB.add(new Button(A_BACK, B_BUTTON, "back", PINK));
        for (int i = 0; i < ShopManager.COUNT; i++) shopB.add(new Button(A_SHOP_ROW0 + i, B_CARD, "", GOLD));
        shopB.add(new Button(A_BACK, B_BUTTON, "back", PINK));
        for (int i = 0; i < DEV_ROWS; i++) devB.add(new Button(A_DEV_ROW0 + i, i == DEV_ROWS - 1 ? B_BUTTON : B_CYCLE, DEV_KEYS[i], i == DEV_ROWS - 1 ? NEON : 0xFF69F0AE));
        devB.add(new Button(A_BACK, B_BUTTON, "back", PINK));
        hudB.add(new Button(A_PAUSE, B_ICON, "pause", 0xFFFFFFFF));
        hudB.add(new Button(A_ABILITY, B_ICON, "nova", 0xFF40C4FF));
        hudB.add(new Button(A_MAP, B_ICON, "map", 0xFFFFFFFF));
        hudB.add(new Button(A_PICKUP, B_BUTTON, "pick_up", 0xFFFFFFFF));
        for (int i = 0; i < Config.HOTBAR; i++) hudB.add(new Button(A_SLOT0 + i, B_ICON, "", 0xFFFFFFFF));
        for (int i = 0; i < Config.HOTBAR; i++) lootB.add(new Button(A_LOOT_SLOT0 + i, B_ICON, "", 0xFFFFFFFF));
        lootB.add(new Button(A_LOOT_SCRAP, B_BUTTON, "scrap", GOLD));
        lootB.add(new Button(A_LOOT_LEAVE, B_BUTTON, "leave", PINK));
        mapB.add(new Button(A_MAP_BACK, B_BUTTON, "back", PINK));
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
        setB.add(new Button(A_MEME, B_TOGGLE, "meme_sounds", 0xFFFF4081));
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
        float bw = 460, bh = 76, y = Math.max(H * 0.44f, 520);
        int[] order = {0, 4, 1, 2, 5, 3};   // PLAY, SHOP, SETTINGS, STATISTICS, DEV, EXIT
        for (int i : order) {
            Button b = menuB.get(i);
            float h = i == 0 ? bh + 20 : bh;
            b.set(W / 2 - bw / 2, y, W / 2 + bw / 2, y + h);
            y += h + 14;
        }
        float mh = Math.min(230, (H - 330) / 3f - 16);
        for (int i = 0; i < WorldMap.MODES; i++) mapSelB.get(i).set(40, 160 + i * (mh + 16), W - 40, 160 + i * (mh + 16) + mh);
        mapSelB.get(WorldMap.MODES).set(W / 2 - 180, H - 120, W / 2 + 180, H - 44);
        float sh = Math.min(76, (H - 330) / ShopManager.COUNT);
        for (int i = 0; i < ShopManager.COUNT; i++) shopB.get(i).set(30, 170 + i * sh, W - 30, 170 + i * sh + sh - 6);
        shopB.get(ShopManager.COUNT).set(W / 2 - 180, H - 120, W / 2 + 180, H - 44);
        float dh = Math.min(70, (H - 300) / DEV_ROWS);
        for (int i = 0; i < DEV_ROWS; i++) devB.get(i).set(40, 150 + i * dh, W - 40, 150 + i * dh + dh - 8);
        devB.get(DEV_ROWS).set(W / 2 - 180, H - 120, W / 2 + 180, H - 44);
        hudB.get(0).set(W - 96, 46, W - 22, 120);
        hudB.get(1).set(W - 148, H - 300, W - 32, H - 184);
        hudB.get(2).set(W - 180, 132, W - 20, 292);
        hudB.get(3).set(W / 2 - 160, H - 255, W / 2 + 160, H - 165);
        float hs = 100, hg = 10, hx = (W - (hs * Config.HOTBAR + hg * (Config.HOTBAR - 1))) / 2f;
        for (int i = 0; i < Config.HOTBAR; i++) hudB.get(4 + i).set(hx + i * (hs + hg), H - 140, hx + i * (hs + hg) + hs, H - 40);
        float lrow = Math.min(80, (H - 170 - 548) / 5f);
        for (int i = 0; i < Config.HOTBAR; i++) lootB.get(i).set(40, 548 + i * lrow, W - 40, 548 + i * lrow + lrow - 8);
        lootB.get(5).set(40, H - 140, W / 2 - 10, H - 50);
        lootB.get(6).set(W / 2 + 10, H - 140, W - 40, H - 50);
        mapB.get(0).set(W / 2 - 180, H - 120, W / 2 + 180, H - 44);
        y = H * 0.42f;
        for (Button b : pauseB) { b.set(W / 2 - bw / 2, y, W / 2 + bw / 2, y + bh); y += bh + 20; }
        float ch = Math.min(200, (H * 0.62f) / 3f - 20);
        y = H * 0.25f;
        for (Button b : levelB) { b.set(40, y, W - 40, y + ch); y += ch + 22; }
        y = H * 0.70f;
        for (Button b : overB) { b.set(W / 2 - bw / 2, y, W / 2 + bw / 2, y + bh); y += bh + 20; }
        float row = Math.min(88, (H - 360) / 11f);
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
            case Game.S_LOOT: return lootB;
            case Game.S_MAP: return mapB;
            case Game.S_MAPSELECT: return mapSelB;
            case Game.S_SHOP: return shopB;
            case Game.S_DEV: return devB;
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
    public Button lootButton(int i) { return lootB.get(i); }
    public Button mapButton(int i) { return mapB.get(i); }
    public Button mapSelectButton(int i) { return mapSelB.get(i); }
    public Button shopButton(int i) { return shopB.get(i); }
    public Button devButton(int i) { return devB.get(i); }

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
        hudB.get(3).enabled = g.loot.nearby != null;
        if (g.state == Game.S_LEVELUP && g.realTime - levelOpenTime < 0.45f) return true;
        if (g.state == Game.S_GAMEOVER && g.gameOverTimer < 0.8f) return true;
        for (Button b : bs) {
            if (!b.hit(x, y)) continue;
            if (b.kind == B_SLIDER) { slider = b; sliderPointer = id; setSlider(b, x); return true; }
            if (b.action == A_ABILITY) { g.playerCtl.useAbility(); return true; }
            if (b.action >= A_SLOT0 && b.action < A_SLOT0 + Config.HOTBAR) { g.weapons.select(b.action - A_SLOT0); return true; }
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
            case A_PLAY: g.state = Game.S_MAPSELECT; break;
            case A_SHOP: g.state = Game.S_SHOP; break;
            case A_DEV: g.state = Game.S_DEV; break;
            case A_MEME: s.memeSounds = !s.memeSounds; s.save(); if (s.memeSounds) g.audio.play(Sfx.AIRHORN); break;
            case A_SETTINGS: g.openSettings(); break;
            case A_STATS: g.state = Game.S_STATS; break;
            case A_EXIT: g.platform.exitApp(); break;
            case A_RESUME: g.resume(); break;
            case A_RESTART: g.restart(); break;
            case A_MENU: g.toMenu(); break;
            case A_BACK: if (g.state == Game.S_SETTINGS) g.closeSettings(); else g.state = Game.S_MENU; break;
            case A_MAPSEL0: case A_MAPSEL0 + 1: case A_MAPSEL0 + 2: g.startRun(a - A_MAPSEL0); break;
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
            case A_MAP: g.openMap(); break;
            case A_MAP_BACK: g.closeMap(); break;
            case A_PICKUP: g.openLoot(); break;
            case A_LOOT_SCRAP: g.loot.scrap(); g.closeLoot(); break;
            case A_LOOT_LEAVE: g.closeLoot(); break;
            default:
                if (a >= A_LOOT_SLOT0 && a < A_LOOT_SLOT0 + Config.HOTBAR) { g.loot.equipInto(a - A_LOOT_SLOT0); g.closeLoot(); }
                else if (a >= A_SHOP_ROW0 && a < A_SHOP_ROW0 + ShopManager.COUNT) {
                    int i = a - A_SHOP_ROW0;
                    if (g.shop.buy(i)) { g.audio.play(Sfx.SELECT); shopFlash = i; shopFlashT = 0.4f; }
                    else { g.audio.play(Sfx.HURT); shopFlash = -1 - i; shopFlashT = 0.4f; }
                } else if (a >= A_DEV_ROW0 && a < A_DEV_ROW0 + DEV_ROWS) devAction(a - A_DEV_ROW0);
        }
    }

    // ------------------------------------------------------------------ update
    public void update(float dt) { if (annTimer > 0) annTimer -= dt; if (shopFlashT > 0) shopFlashT -= dt; }

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
            case Game.S_LOOT: renderHud(gfx); renderLoot(gfx); break;
            case Game.S_MAP: renderMap(gfx); break;
            case Game.S_MAPSELECT: dim(gfx, 0xB3070B16); renderMapSelect(gfx); break;
            case Game.S_SHOP: dim(gfx, 0xC0070B16); renderShop(gfx); break;
            case Game.S_DEV: dim(gfx, 0xD0070B16); renderDev(gfx); break;
        }
        renderToast(gfx);
    }

    private String s(String key) { return g.strings.get(key); }

    /** Largest text size (<= size) at which t fits in maxW. */
    private float fit(Gfx gfx, String t, float maxW, float size, boolean bold) {
        float w = gfx.textWidth(t, size, bold);
        return w <= maxW ? size : Math.max(12f, size * maxW / w);
    }

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
        coinIcon(gfx, 30, 98, 10);
        gfx.text(Integer.toString(g.coinsThisRun), 46, 106, 22, 0xFFFFC400, Gfx.ALIGN_LEFT, true);
        renderHotbar(gfx);
        renderMinimap(gfx);
        Button pk = hudB.get(3);
        pk.enabled = g.state == Game.S_PLAYING && g.loot.nearby != null;
        if (pk.enabled) {
            Weapon lw = g.loot.nearby.weapon;
            int rc = Loot.COLORS[lw.rarity];
            float pulse = 0.5f + 0.5f * MathUtil.sin(g.realTime * 6);
            gfx.roundRect(pk.l, pk.t, pk.r, pk.b, 18, pk == pressed ? MathUtil.alpha(rc, 0.45f) : 0xE60E1530);
            gfx.strokeRoundRect(pk.l, pk.t, pk.r, pk.b, 18, 3 + pulse * 2, rc);
            gfx.text(s("pick_up"), pk.cx(), pk.t + 36, 26, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
            String ln = lw.name(g.strings);
            gfx.text(ln, pk.cx(), pk.t + 70, fit(gfx, ln, pk.r - pk.l - 20, 21, true), rc, Gfx.ALIGN_CENTER, true);
        }
        // boss bar
        Enemy boss = g.bosses.current;
        if (boss != null && boss.alive) {
            int c = g.bosses.color(boss);
            gfx.text(g.bosses.name(boss), (W - 200) / 2 + 20, 168, 26, c, Gfx.ALIGN_CENTER, true);
            bar(gfx, 30, 178, W - 200, 200, boss.hp / boss.maxHp, c, 0xAA000000);
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
        if (!p.alive && g.state == Game.S_PLAYING) { renderDeath(gfx); return; }
        // joystick
        Joystick j = g.joystick;
        if (j.active) {
            gfx.circle(j.baseX, j.baseY, j.radius, 0x22FFFFFF);
            gfx.ring(j.baseX, j.baseY, j.radius, 3, 0x66FFFFFF);
            float kx = j.baseX + j.outX * j.radius, ky = j.baseY + j.outY * j.radius;
            gfx.circle(kx, ky, 34, 0x9940C4FF);
            gfx.ring(kx, ky, 34, 3, 0xFFFFFFFF);
        } else if (g.state == Game.S_PLAYING && g.time < 8) {
            float x = 170, y = H - 340;
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

    /** Slow-motion death: desaturating overlay, cinematic bars and a big title. */
    private void renderDeath(Gfx gfx) {
        float W = g.viewW, H = g.viewH, t = g.deathSlowmo;
        float k = MathUtil.clamp(t / 0.8f, 0, 1);
        gfx.rect(0, 0, W, H, MathUtil.alpha(0xFF2A2A2A, 0.55f * k));
        gfx.rect(0, 0, W, H, MathUtil.alpha(0xFF400010, 0.25f * k));
        float bar = H * 0.12f * k;
        gfx.rect(0, 0, W, bar, 0xFF000000);
        gfx.rect(0, H - bar, W, H, 0xFF000000);
        if (t > 0.5f) {
            float a = MathUtil.clamp((t - 0.5f) * 3f, 0, 1);
            float sc = 1.6f - 0.6f * a;
            gfx.text(s("eliminated"), W / 2 + 4, H * 0.48f + 4, 92 * sc, MathUtil.alpha(0xFF000000, a), Gfx.ALIGN_CENTER, true);
            gfx.text(s("eliminated"), W / 2, H * 0.48f, 92 * sc, MathUtil.alpha(0xFFFF1744, a), Gfx.ALIGN_CENTER, true);
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
            Weapon cw = c.type == UpgradeManager.T_NEW_WEAPON ? c.rolled : (c.type == UpgradeManager.T_WEAPON_LEVEL ? g.weapons.slots[c.weapon] : null);
            int rc = c.type == UpgradeManager.T_STAT ? Config.RARITY_COLORS[c.rarity] : (cw != null ? Loot.COLORS[cw.rarity] : 0xFF64FFDA);
            float off = (1 - appear) * (300 + i * 120);
            float l = b.l + off, r = b.r + off;
            gfx.roundRect(l, b.t, r, b.b, 20, b == pressed ? MathUtil.alpha(rc, 0.3f) : 0xF0101830);
            gfx.strokeRoundRect(l, b.t, r, b.b, 20, c.rarity >= 2 ? 5 : 3, rc);
            if (c.rarity == 3 || (cw != null && cw.rarity == Loot.LEGENDARY)) gfx.strokeRoundRect(l - 6, b.t - 6, r + 6, b.b + 6, 24, 2, MathUtil.alpha(rc, 0.4f + 0.3f * MathUtil.sin(g.realTime * 6)));
            float h = b.b - b.t;
            float iconX = rtl ? r - 70 : l + 70;
            gfx.circle(iconX, b.t + h / 2, 44, MathUtil.alpha(rc, 0.18f));
            if (c.type == UpgradeManager.T_PET_NEW || c.type == UpgradeManager.T_PET_LEVEL) {
                int pt = c.type == UpgradeManager.T_PET_NEW ? c.weapon : g.pets.pets[c.weapon].type;
                PetManager.draw(gfx, pt, c.type == UpgradeManager.T_PET_NEW ? 1 : g.pets.pets[c.weapon].level, iconX, b.t + h / 2 - 6, 0, g.realTime);
            } else if (c.type == UpgradeManager.T_STAT) statIcon(gfx, c.stat, iconX, b.t + h / 2, 26, rc);
            else if (cw != null) { gfx.save(); gfx.translate(iconX - 22, b.t + h / 2); g.world.drawGun(gfx, cw, 1.4f, 0); gfx.restore(); }
            float tx = rtl ? r - 140 : l + 140;
            int al = rtl ? Gfx.ALIGN_RIGHT : Gfx.ALIGN_LEFT;
            if (c.type == UpgradeManager.T_PET_NEW || c.type == UpgradeManager.T_PET_LEVEL) rc = PetManager.COLORS[c.type == UpgradeManager.T_PET_NEW ? c.weapon : g.pets.pets[c.weapon].type];
            String rarity = c.type == UpgradeManager.T_PET_NEW ? s("pet_new") : c.type == UpgradeManager.T_PET_LEVEL ? s("pet_up") : c.type == UpgradeManager.T_STAT ? s(Config.RARITY_KEYS[c.rarity])
                    : s(c.type == UpgradeManager.T_NEW_WEAPON ? "weapon_new" : "weapon_up") + (cw != null ? " · " + s(Loot.RARITY_KEYS[cw.rarity]) : "");
            gfx.text(rarity.toUpperCase(), tx, b.t + h * 0.30f, 18, rc, al, true);
            String ttl = um.title(c);
            gfx.text(ttl, tx, b.t + h * 0.56f, fit(gfx, ttl, (r - l) - 170, 32, true), 0xFFFFFFFF, al, true);
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
        coinIcon(gfx, W / 2 - 90, top - 22, 13);
        gfx.text("+" + g.coinsThisRun + " " + s("coins"), W / 2 - 70, top - 13, 26, 0xFFFFC400, Gfx.ALIGN_LEFT, true);
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
        gfx.text(s("custom_sounds_hint"), W / 2, g.viewH - 150, 16, 0x99FFFFFF, Gfx.ALIGN_CENTER, false);
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
                boolean on = b.action == A_VIB ? st.vibration : (b.action == A_SHAKE ? st.screenShake : (b.action == A_MEME ? st.memeSounds : st.showDamageNumbers));
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
        String[] labels = {s("best_score"), s("games_played"), s("total_kills"), s("total_time"), s("best_time"), s("highest_level"), s("bosses_defeated"), s("legendaries_found"), s("chests_opened")};
        String[] vals = {Integer.toString(sv.bestScore), Integer.toString(sv.gamesPlayed), Long.toString(sv.totalKills),
                MathUtil.time(sv.totalTime), MathUtil.time(sv.bestTime), Integer.toString(sv.highestLevel), Integer.toString(sv.bossesDefeated),
                Integer.toString(sv.legendariesFound), Integer.toString(sv.chestsOpened)};
        float y = 140;
        float rowH = Math.min(38, (g.viewH - 800) / 9f);
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

    // ------------------------------------------------------------------ hotbar / minimap / loot / map
    private void renderHotbar(Gfx gfx) {
        WeaponManager wm = g.weapons;
        for (int i = 0; i < Config.HOTBAR; i++) {
            Button b = hudB.get(4 + i);
            Weapon w = wm.slots[i];
            boolean act = i == wm.active;
            float lift = act ? 10 : 0;
            int rc = w == null ? 0x55FFFFFF : Loot.COLORS[w.rarity];
            gfx.roundRect(b.l, b.t - lift, b.r, b.b - lift, 16, act ? 0xEE16213E : 0xAA0B1022);
            if (act) gfx.roundRect(b.l - 4, b.t - lift - 4, b.r + 4, b.b - lift + 4, 20, MathUtil.alpha(rc, 0.18f));
            gfx.strokeRoundRect(b.l, b.t - lift, b.r, b.b - lift, 16, act ? 4 : 2, act ? rc : MathUtil.alpha(rc, 0.6f));
            gfx.text(Integer.toString(i + 1), b.l + 12, b.t - lift + 22, 16, 0x88FFFFFF, Gfx.ALIGN_LEFT, true);
            if (w == null) { gfx.text("+", b.cx(), b.cy() - lift + 12, 34, 0x33FFFFFF, Gfx.ALIGN_CENTER, true); continue; }
            gfx.save();
            gfx.translate(b.cx() - 16, b.cy() - lift - 4);
            gfx.rotate(-0.25f);
            g.world.drawGun(gfx, w, 1.25f, 0);
            gfx.restore();
            if (w.element != Loot.E_NONE) gfx.circle(b.r - 14, b.t - lift + 15, 7, Loot.ELEMENT_COLORS[w.element]);
            gfx.text(s("lv") + w.level, b.cx(), b.b - lift - 10, 16, act ? 0xFFFFFFFF : 0xAAFFFFFF, Gfx.ALIGN_CENTER, true);
            if (act && wm.switchTimer > 0) gfx.rect(b.l + 8, b.b - lift - 6, b.l + 8 + (b.r - b.l - 16) * (1 - wm.switchTimer / Config.SWITCH_DELAY), b.b - lift - 3, rc);
        }
    }

    private final float[] rr4 = new float[4];

    private void renderMinimap(Gfx gfx) {
        Button mb = hudB.get(2);
        float R = 1800f;                                   // world radius shown
        float half = (mb.r - mb.l) / 2f, sc = half / R;
        float cx = mb.cx(), cy = mb.cy();
        Player p = g.player;
        WorldMap m = g.map;
        float W = Config.WORLD_HALF;
        gfx.roundRect(mb.l - 3, mb.t - 3, mb.r + 3, mb.b + 3, 14, 0xCC000000);
        // region tint
        for (int q = 0; q < 4; q++) {
            m.regionRect(q, rr4);
            float ql = rr4[0], qr = rr4[2], qt = rr4[1], qb = rr4[3];
            float il = Math.max(ql, p.x - R), ir = Math.min(qr, p.x + R), it = Math.max(qt, p.y - R), ib = Math.min(qb, p.y + R);
            if (il >= ir || it >= ib) continue;
            gfx.rect(cx + (il - p.x) * sc, cy + (it - p.y) * sc, cx + (ir - p.x) * sc, cy + (ib - p.y) * sc, WorldMap.MAP_COLOR[q]);
        }
        for (int i = 0; i < m.count; i++) {
            float dx = m.ox[i] - p.x, dy = m.oy[i] - p.y;
            if (Math.abs(dx) > R - m.or[i] || Math.abs(dy) > R - m.or[i]) continue;
            gfx.circle(cx + dx * sc, cy + dy * sc, Math.max(1.5f, m.or[i] * sc), 0xFF607D8B);
        }
        for (int i = 0; i < m.chestCount; i++) {
            if (m.opened[i]) continue;
            float dx = m.cx[i] - p.x, dy = m.cy[i] - p.y;
            if (Math.abs(dx) > R || Math.abs(dy) > R) continue;
            gfx.rect(cx + dx * sc - 3.5f, cy + dy * sc - 3.5f, cx + dx * sc + 3.5f, cy + dy * sc + 3.5f, m.golden[i] ? 0xFFFFC400 : 0xFF26C6DA);
        }
        EntityList<GroundItem> items = g.loot.items;
        for (int i = 0; i < items.size; i++) {
            GroundItem it = items.get(i);
            float dx = it.x - p.x, dy = it.y - p.y;
            if (Math.abs(dx) > R || Math.abs(dy) > R) continue;
            gfx.circle(cx + dx * sc, cy + dy * sc, 3.5f, Loot.COLORS[it.weapon.rarity]);
        }
        EntityList<Enemy> en = g.enemies.list;
        int shown = 0;
        for (int i = 0; i < en.size && shown < 160; i++) {
            Enemy e = en.get(i);
            float dx = e.x - p.x, dy = e.y - p.y;
            if (!e.alive || Math.abs(dx) > R || Math.abs(dy) > R) continue;
            shown++;
            if (e.boss) gfx.circle(cx + dx * sc, cy + dy * sc, 6, g.bosses.color(e));
            else gfx.rect(cx + dx * sc - 1.2f, cy + dy * sc - 1.2f, cx + dx * sc + 1.2f, cy + dy * sc + 1.2f, e.tier > 0 ? 0xFFFF9100 : 0xFFFF5252);
        }
        float a = p.facing;
        float[] t = {cx + MathUtil.cos(a) * 8, cy + MathUtil.sin(a) * 8, cx + MathUtil.cos(a + 2.5f) * 6, cy + MathUtil.sin(a + 2.5f) * 6,
                cx + MathUtil.cos(a - 2.5f) * 6, cy + MathUtil.sin(a - 2.5f) * 6};
        gfx.poly(t, 3, 0xFFFFFFFF);
        gfx.strokeRoundRect(mb.l - 3, mb.t - 3, mb.r + 3, mb.b + 3, 14, 2, mb == pressed ? 0xFFFFFFFF : 0x8818FFFF);
        gfx.text(s(WorldMap.REGION_KEYS[m.regionAt(p.x, p.y)]), cx, mb.b + 22, 16, 0xCCFFFFFF, Gfx.ALIGN_CENTER, true);
    }

    /** Stat lines describing a gun (used on the loot screen). */
    private int weaponLines(Weapon w, String[] out) {
        Player p = g.player;
        int n = 0;
        int cnt = w.count(p);
        out[n++] = s("st_dps") + "  " + Math.round(w.dps(p));
        out[n++] = s("st_damage") + "  " + Math.round(w.damage(p)) + (cnt > 1 && w.type.usesProjectiles() ? " ×" + cnt : "");
        if (w.type.kind != WeaponType.K_ORBIT) out[n++] = s("st_rate") + "  " + String.format(java.util.Locale.US, "%.1f/s", 1f / w.cooldown(p));
        if (w.critBonus() > 0) out[n++] = s("st_crit") + "  +" + Math.round(w.critBonus() * 100) + "%";
        if (w.pierce() > 0 && w.type.usesProjectiles() && w.pierce() < 50) out[n++] = s("st_pierce") + "  " + w.pierce();
        if (w.element != Loot.E_NONE) out[n++] = s(Loot.ELEMENT_KEYS[w.element]) + ": " + s("eld" + Loot.ELEMENT_KEYS[w.element].substring(2));
        if (w.trait != Loot.T_NONE) out[n++] = "★ " + s(Loot.TRAIT_DESC[w.trait]);
        return n;
    }
    private final String[] lines = new String[10];

    public void openLoot() { pressed = null; pressedPointer = -1; levelOpenTime = g.realTime; }

    private void renderLoot(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        dim(gfx, 0xE0070B16);
        GroundItem it = g.loot.viewing;
        if (it == null) return;
        Weapon w = it.weapon;
        int rc = Loot.COLORS[w.rarity];
        neonText(gfx, s("weapon_found"), W / 2, 80, 50, rc);
        gfx.roundRect(40, 105, W - 40, 505, 22, 0xF0101830);
        gfx.strokeRoundRect(40, 105, W - 40, 505, 22, w.rarity >= Loot.EPIC ? 5 : 3, rc);
        gfx.circle(140, 210, 70, MathUtil.alpha(rc, 0.15f));
        gfx.save(); gfx.translate(105, 210); gfx.rotate(-0.3f); g.world.drawGun(gfx, w, 2.4f, 0); gfx.restore();
        String nm = w.name(g.strings);
        gfx.text(nm, 240, 165, fit(gfx, nm, W - 300, 32, true), rc, Gfx.ALIGN_LEFT, true);
        gfx.text(s(Loot.RARITY_KEYS[w.rarity]).toUpperCase() + "  ·  " + s("st_ilvl") + " " + w.itemLevel, 240, 200, 20, MathUtil.alpha(rc, 0.85f), Gfx.ALIGN_LEFT, true);
        gfx.text(s(w.type.descKey), 240, 232, 19, 0xFFB0BEC5, Gfx.ALIGN_LEFT, false);
        if (w.maker >= 0) {
            gfx.text(s(Loot.MAKER_KEYS[w.maker]) + " — " + s(Loot.MAKER_DESC[w.maker]), 240, 260, 18, Loot.MAKER_ACCENT[w.maker], Gfx.ALIGN_LEFT, true);
            gfx.text(s(Loot.BARREL_KEYS[w.barrel]) + " · " + s(Loot.GRIP_KEYS[w.grip]) + " · " + s(Loot.SIGHT_KEYS[w.sight]) + (w.accessory > 0 ? " · " + s(Loot.ACC_KEYS[w.accessory]) : ""),
                    70, 292, 17, 0xFFB0BEC5, Gfx.ALIGN_LEFT, false);
        }
        int n = weaponLines(w, lines);
        float y = 330;
        for (int i = 0; i < n; i++) {
            gfx.text(lines[i], 70, y, i == 0 ? 26 : 21, i == 0 ? GOLD : (lines[i].startsWith("★") ? Loot.COLORS[Loot.LEGENDARY] : 0xFFFFFFFF), Gfx.ALIGN_LEFT, i == 0);
            y += i == 0 ? 32 : 27;
        }
        gfx.text(s("equip_into"), W / 2, 532, 22, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
        float newDps = w.dps(g.player);
        for (int i = 0; i < Config.HOTBAR; i++) {
            Button b = lootB.get(i);
            Weapon cur = g.weapons.slots[i];
            int cc = cur == null ? 0x6618FFFF : Loot.COLORS[cur.rarity];
            gfx.roundRect(b.l, b.t, b.r, b.b, 14, b == pressed ? MathUtil.alpha(cc, 0.35f) : 0xCC0E1530);
            gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 14, i == g.weapons.active ? 3 : 2, cc);
            float my = (b.t + b.b) / 2;
            gfx.text(Integer.toString(i + 1), b.l + 20, my + 8, 22, 0x88FFFFFF, Gfx.ALIGN_LEFT, true);
            if (cur == null) { gfx.text(s("empty_slot"), b.l + 60, my + 8, 22, 0xFF18FFFF, Gfx.ALIGN_LEFT, true); continue; }
            gfx.save(); gfx.translate(b.l + 70, my); g.world.drawGun(gfx, cur, 1.0f, 0); gfx.restore();
            String cn = cur.name(g.strings);
            gfx.text(cn, b.l + 125, my + 8, fit(gfx, cn, b.r - b.l - 330, 21, true), cc, Gfx.ALIGN_LEFT, true);
            float d = newDps - cur.dps(g.player);
            String ds = (d >= 0 ? "▲ +" : "▼ ") + Math.round(d) + " " + s("st_dps");
            gfx.text(ds, b.r - 20, my + 8, 20, d >= 0 ? 0xFF69F0AE : 0xFFFF5252, Gfx.ALIGN_RIGHT, true);
        }
        Button sc = lootB.get(5);
        sc.key = "scrap";
        drawButton(gfx, sc);
        gfx.text("+" + Loot.SCRAP_XP[w.rarity] * (1 + g.loot.itemLevel() / 4) + " XP", sc.cx(), sc.b + 26, 18, GOLD, Gfx.ALIGN_CENTER, false);
        drawButton(gfx, lootB.get(6));
    }

    private void renderMap(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        gfx.clear(0xFF05070F);
        WorldMap m = g.map;
        neonText(gfx, s("map"), W / 2, 80, 56, NEON);
        float boxW = W - 40, boxH = H - 330;
        float sc = Math.min(boxW / (2 * m.hw), boxH / (2 * m.hh));
        float mw = 2 * m.hw * sc, mhh = 2 * m.hh * sc;
        float size = mw;
        float x0 = (W - mw) / 2, y0 = 120 + (boxH - mhh) / 2;
        float WH = m.hw, HH = m.hh;
        for (int q = 0; q < 4; q++) {
            m.regionRect(q, rr4);
            gfx.rect(x0 + (rr4[0] + WH) * sc, y0 + (rr4[1] + HH) * sc, x0 + (rr4[2] + WH) * sc, y0 + (rr4[3] + HH) * sc, WorldMap.MAP_COLOR[q]);
        }
        for (int i = 0; i < m.count; i++) gfx.circle(x0 + (m.ox[i] + WH) * sc, y0 + (m.oy[i] + HH) * sc, Math.max(1.2f, m.or[i] * sc), 0xFF78909C);
        // fog of war (cells cover the whole 8000-unit square; draw the part inside this map)
        float fc = WorldMap.FOG_CELL * sc, FH = Config.WORLD_HALF;
        for (int ry = 0; ry < WorldMap.FOG_COLS; ry++) for (int rx = 0; rx < WorldMap.FOG_COLS; rx++) {
            if (m.explored[ry * WorldMap.FOG_COLS + rx]) continue;
            float wx = -FH + rx * WorldMap.FOG_CELL, wy = -FH + ry * WorldMap.FOG_CELL;
            if (wx + WorldMap.FOG_CELL < -WH || wx > WH || wy + WorldMap.FOG_CELL < -HH || wy > HH) continue;
            float l = Math.max(x0, x0 + (wx + WH) * sc), t = Math.max(y0, y0 + (wy + HH) * sc);
            float r = Math.min(x0 + mw, x0 + (wx + WorldMap.FOG_CELL + WH) * sc + 0.5f), bb = Math.min(y0 + mhh, y0 + (wy + WorldMap.FOG_CELL + HH) * sc + 0.5f);
            gfx.rect(l, t, r, bb, 0xE6070B16);
        }
        for (int i = 0; i < m.chestCount; i++) {
            if (!m.isExplored(m.cx[i], m.cy[i])) continue;
            float cx = x0 + (m.cx[i] + WH) * sc, cy = y0 + (m.cy[i] + HH) * sc;
            gfx.rect(cx - 4, cy - 4, cx + 4, cy + 4, m.opened[i] ? 0x55FFFFFF : (m.golden[i] ? 0xFFFFC400 : 0xFF26C6DA));
        }
        EntityList<GroundItem> items = g.loot.items;
        for (int i = 0; i < items.size; i++) {
            GroundItem it = items.get(i);
            gfx.circle(x0 + (it.x + WH) * sc, y0 + (it.y + HH) * sc, 4, Loot.COLORS[it.weapon.rarity]);
        }
        Enemy boss = g.bosses.current;
        if (boss != null && boss.alive) gfx.circle(x0 + (boss.x + WH) * sc, y0 + (boss.y + HH) * sc, 8, g.bosses.color(boss));
        for (int q = 0; q < 4; q++) {
            m.regionRect(q, rr4);
            float lx = x0 + ((rr4[0] + rr4[2]) / 2 + WH) * sc, ly2 = y0 + ((rr4[1] + rr4[3]) / 2 + HH) * sc;
            gfx.text(s(WorldMap.REGION_KEYS[q]), lx, m.corridor() ? y0 - 12 : ly2, m.corridor() ? 15 : 22, 0x99FFFFFF, Gfx.ALIGN_CENTER, true);
        }
        Player p = g.player;
        float px = x0 + (p.x + WH) * sc, py = y0 + (p.y + HH) * sc;
        float pulse = 6 + 3 * MathUtil.sin(g.realTime * 5);
        gfx.ring(px, py, pulse + 6, 2, 0xFFFFFFFF);
        gfx.circle(px, py, 6, 0xFF40C4FF);
        gfx.strokeRoundRect(x0, y0, x0 + mw, y0 + mhh, 4, 3, 0xFFFF1744);
        float ly = 120 + boxH + 34;
        gfx.text(s("explored") + " " + Math.round(m.exploredFraction() * 100) + "%", W / 2, ly, 22, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
        gfx.rect(W / 2 - 230, ly + 16, W / 2 - 220, ly + 26, 0xFFFFC400);
        gfx.text(s("chest"), W / 2 - 210, ly + 26, 17, 0xCCFFFFFF, Gfx.ALIGN_LEFT, false);
        gfx.circle(W / 2 - 50, ly + 21, 5, Loot.COLORS[Loot.EPIC]);
        gfx.text(s("loot"), W / 2 - 40, ly + 26, 17, 0xCCFFFFFF, Gfx.ALIGN_LEFT, false);
        gfx.circle(W / 2 + 90, ly + 21, 6, 0xFF40C4FF);
        gfx.text(s("you"), W / 2 + 102, ly + 26, 17, 0xCCFFFFFF, Gfx.ALIGN_LEFT, false);
        drawButton(gfx, mapB.get(0));
    }

    // ------------------------------------------------------------------ map select / shop / developer menu
    private int shopFlash; private float shopFlashT;
    public static final int DEV_ROWS = 13;
    public static final String[] DEV_KEYS_PUBLIC = {"dv_coins_add", "dv_coins_big", "dv_coins_zero", "dv_god", "dv_oneshot", "dv_level", "dv_minute", "dv_legendary", "dv_pets", "dv_shop_max", "dv_shop_reset", "dv_reset_all", "dv_start", "dv_confirm", "dev_hint", "choose_map", "shop", "dev_menu", "meme_sounds", "custom_sounds_hint", "eliminated", "second_wind", "multi_kill", "pet_new", "pet_up", "card_pet_level", "maker_fmt", "coins", "max"};
    static final String[] DEV_KEYS = {"dv_coins_add", "dv_coins_big", "dv_coins_zero", "dv_god", "dv_oneshot", "dv_level", "dv_minute",
            "dv_legendary", "dv_pets", "dv_shop_max", "dv_shop_reset", "dv_reset_all", "dv_start"};
    private static final int[] DEV_LEVELS = {1, 5, 10, 20, 30, 50};
    private static final int[] DEV_MINUTES = {0, 2, 4, 5, 9, 14, 19};
    private int devConfirmReset;

    private void devAction(int row) {
        DevOptions d = g.dev;
        SaveManager sv = g.save;
        switch (row) {
            case 0: sv.coins += 1000; sv.save(); break;
            case 1: sv.coins += 100000; sv.save(); break;
            case 2: sv.coins = 0; sv.save(); break;
            case 3: d.godMode = !d.godMode; break;
            case 4: d.oneShot = !d.oneShot; break;
            case 5: d.startLevel = next(DEV_LEVELS, d.startLevel); break;
            case 6: d.startMinute = next(DEV_MINUTES, d.startMinute); break;
            case 7: d.legendaryLoadout = !d.legendaryLoadout; break;
            case 8: d.allPets = !d.allPets; break;
            case 9: for (int i = 0; i < ShopManager.COUNT; i++) { while (!g.shop.maxed(i)) { sv.coins += g.shop.cost(i); g.shop.buy(i); } } break;
            case 10: for (int i = 0; i < ShopManager.COUNT; i++) { g.shop.levels[i] = 0; g.platform.storage().putInt("shop_" + i, 0); } sv.save(); break;
            case 11:
                if (devConfirmReset == 0) { devConfirmReset = 1; return; }
                sv.resetAll();
                for (int i = 0; i < ShopManager.COUNT; i++) { g.shop.levels[i] = 0; g.platform.storage().putInt("shop_" + i, 0); }
                sv.save();
                break;
            case 12: g.startRun(sv.selectedMap); break;
        }
        devConfirmReset = 0;
    }
    private static int next(int[] opts, int cur) { for (int i = 0; i < opts.length; i++) if (opts[i] == cur) return opts[(i + 1) % opts.length]; return opts[0]; }

    private String devValue(int row) {
        DevOptions d = g.dev;
        switch (row) {
            case 0: case 1: case 2: return "● " + g.save.coins;
            case 3: return s(d.godMode ? "on" : "off");
            case 4: return s(d.oneShot ? "on" : "off");
            case 5: return Integer.toString(d.startLevel);
            case 6: return d.startMinute + ":00";
            case 7: return s(d.legendaryLoadout ? "on" : "off");
            case 8: return s(d.allPets ? "on" : "off");
            case 11: return devConfirmReset == 1 ? s("dv_confirm") : "";
            case 12: return s(WorldMap.MODE_KEYS[g.save.selectedMap]);
            default: return "";
        }
    }

    private void coinIcon(Gfx gfx, float x, float y, float r) {
        gfx.circle(x, y, r, 0xFFFFC400);
        gfx.ring(x, y, r * 0.7f, Math.max(1.5f, r * 0.18f), 0xFFFF8F00);
        gfx.circle(x - r * 0.3f, y - r * 0.3f, r * 0.2f, 0xCCFFFFFF);
    }

    private void renderMapSelect(Gfx gfx) {
        float W = g.viewW;
        neonText(gfx, s("choose_map"), W / 2, 110, 54, NEON);
        for (int i = 0; i < WorldMap.MODES; i++) {
            Button b = mapSelB.get(i);
            boolean sel = g.save.selectedMap == i;
            gfx.roundRect(b.l, b.t, b.r, b.b, 22, b == pressed ? 0x6618FFFF : 0xE60E1530);
            gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 22, sel ? 4 : 2, sel ? NEON : 0x6618FFFF);
            // preview
            float pl = b.l + 20, pt = b.t + 20, pr = b.l + 220, pb = b.b - 20;
            gfx.roundRect(pl, pt, pr, pb, 10, 0xFF05070F);
            float pcx = (pl + pr) / 2, pcy = (pt + pb) / 2;
            if (i == WorldMap.OPEN) {
                gfx.rect(pl + 4, pt + 4, pcx, pcy, WorldMap.MAP_COLOR[WorldMap.TOXIC]); gfx.rect(pcx, pt + 4, pr - 4, pcy, WorldMap.MAP_COLOR[WorldMap.FROST]);
                gfx.rect(pl + 4, pcy, pcx, pb - 4, WorldMap.MAP_COLOR[WorldMap.NEON]); gfx.rect(pcx, pcy, pr - 4, pb - 4, WorldMap.MAP_COLOR[WorldMap.CRIMSON]);
                for (int k = 0; k < 14; k++) gfx.circle(pl + 15 + (k * 37 % 170), pt + 12 + (k * 53 % (pb - pt - 24)), 4, 0xFF78909C);
            } else if (i == WorldMap.ARENA) {
                gfx.circle(pcx, pcy, (pb - pt) / 2 - 6, WorldMap.MAP_COLOR[WorldMap.CRIMSON]);
                for (int k = 0; k < 10; k++) { float a = k * MathUtil.TAU / 10; gfx.circle(pcx + MathUtil.cos(a) * (pb - pt) * 0.3f, pcy + MathUtil.sin(a) * (pb - pt) * 0.3f, 5, 0xFF78909C); }
            } else {
                gfx.rect(pl + 4, pcy - 9, pr - 4, pcy + 9, WorldMap.MAP_COLOR[WorldMap.NEON]);
                gfx.line(pl + 4, pcy - 10, pr - 4, pcy - 10, 3, 0xFFFF1744); gfx.line(pl + 4, pcy + 10, pr - 4, pcy + 10, 3, 0xFFFF1744);
                gfx.text("◀  ▶", pcx, pcy + 40, 22, 0xAAFFFFFF, Gfx.ALIGN_CENTER, true);
            }
            gfx.circle(pcx, pcy, 6, 0xFF40C4FF);
            float tx = b.l + 245;
            gfx.text(s(WorldMap.MODE_KEYS[i]), tx, b.t + 52, 34, sel ? NEON : 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
            gfx.text(s(WorldMap.MODE_DESC[i]), tx, b.t + 88, 19, 0xFFB0BEC5, Gfx.ALIGN_LEFT, false);
            gfx.text(s("best_score") + ": " + g.save.bestByMap[i], tx, b.b - 28, 22, GOLD, Gfx.ALIGN_LEFT, true);
        }
        drawButton(gfx, mapSelB.get(WorldMap.MODES));
    }

    private void renderShop(Gfx gfx) {
        float W = g.viewW;
        neonText(gfx, s("shop"), W / 2, 90, 60, GOLD);
        coinIcon(gfx, W / 2 - 70, 135, 14);
        gfx.text(Integer.toString(g.save.coins), W / 2 - 48, 145, 30, GOLD, Gfx.ALIGN_LEFT, true);
        ShopManager sh = g.shop;
        for (int i = 0; i < ShopManager.COUNT; i++) {
            Button b = shopB.get(i);
            boolean max = sh.maxed(i), can = sh.canBuy(i);
            int flashCol = shopFlashT > 0 && (shopFlash == i) ? 0x5500E676 : (shopFlashT > 0 && shopFlash == -1 - i ? 0x55FF1744 : 0);
            gfx.roundRect(b.l, b.t, b.r, b.b, 12, b == pressed ? 0x55FFD740 : 0xCC0E1530);
            if (flashCol != 0) gfx.roundRect(b.l, b.t, b.r, b.b, 12, flashCol);
            gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 12, 2, max ? 0xFF00E676 : (can ? GOLD : 0x44FFFFFF));
            float h = b.b - b.t, my = b.t + h / 2;
            gfx.text(s(ShopManager.KEYS[i]), b.l + 16, b.t + h * 0.42f, Math.min(24, h * 0.34f), 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
            gfx.text(sh.effect(i, g.strings), b.l + 16, b.t + h * 0.82f, Math.min(18, h * 0.26f), 0xFFB0BEC5, Gfx.ALIGN_LEFT, false);
            for (int k = 0; k < ShopManager.MAX[i]; k++) {
                float px = b.l + 330 + k * 15;
                if (ShopManager.MAX[i] > 8) px = b.l + 330 + k * 12;
                gfx.rect(px, my - 6, px + (ShopManager.MAX[i] > 8 ? 9 : 11), my + 6, k < sh.levels[i] ? GOLD : 0x33FFFFFF);
            }
            if (max) gfx.text(s("max"), b.r - 20, my + 8, 22, 0xFF00E676, Gfx.ALIGN_RIGHT, true);
            else {
                gfx.text(Integer.toString(sh.cost(i)), b.r - 20, my + 8, 22, can ? GOLD : 0x88FFFFFF, Gfx.ALIGN_RIGHT, true);
                coinIcon(gfx, b.r - 32 - gfx.textWidth(Integer.toString(sh.cost(i)), 22, true), my, 9);
            }
        }
        drawButton(gfx, shopB.get(ShopManager.COUNT));
    }

    private void renderDev(Gfx gfx) {
        float W = g.viewW;
        neonText(gfx, s("dev_menu"), W / 2, 90, 56, 0xFF69F0AE);
        gfx.text(s("dev_hint"), W / 2, 128, 19, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
        for (int i = 0; i < DEV_ROWS; i++) {
            Button b = devB.get(i);
            if (i == DEV_ROWS - 1) { drawButton(gfx, b); gfx.text(devValue(i), b.r - 18, (b.t + b.b) / 2 + 7, 18, 0xCCFFFFFF, Gfx.ALIGN_RIGHT, false); continue; }
            gfx.roundRect(b.l, b.t, b.r, b.b, 12, b == pressed ? 0x5569F0AE : 0xCC0E1530);
            gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 12, 2, i == 11 ? PINK : 0x6669F0AE);
            float my = (b.t + b.b) / 2;
            gfx.text(s(DEV_KEYS[i]), b.l + 18, my + 8, 22, 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
            gfx.text(devValue(i), b.r - 18, my + 8, 22, i == 11 ? PINK : 0xFF69F0AE, Gfx.ALIGN_RIGHT, true);
        }
        drawButton(gfx, devB.get(DEV_ROWS));
    }

    private void renderToast(Gfx gfx) {
        AchievementManager am = g.achievements;
        if (am.toastId < 0) return;
        if (g.state != Game.S_PLAYING && g.state != Game.S_GAMEOVER && g.state != Game.S_MENU) return;
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
