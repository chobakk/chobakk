package com.blockkillers.core;

/**
 * Root of the game: owns every manager, runs the state machine, steps the simulation and dispatches input.
 * Contains no platform code, so the exact same game runs on Android and in the desktop test harness.
 */
public final class Game {
    public static final int S_MENU = 0, S_PLAYING = 1, S_PAUSED = 2, S_LEVELUP = 3, S_GAMEOVER = 4, S_SETTINGS = 5, S_STATS = 6, S_LOOT = 7, S_MAP = 8, S_SHOP = 9, S_DEV = 10, S_MAPSELECT = 11, S_MPMENU = 12, S_LOBBY = 13, S_MPRESULT = 14, S_LAYOUT = 15;
    /** Match modes. */
    public static final int M_SOLO = 0, M_COOP = 1, M_PVP = 2, M_PVPVE = 3;
    public static final String[] MODE_KEYS = {"md_solo", "md_coop", "md_pvp", "md_pvpve"};
    public static final String[] MODE_DESC = {"mdd_solo", "mdd_coop", "mdd_pvp", "mdd_pvpve"};

    public final Platform platform;
    public final SettingsManager settings;
    public final SaveManager save;
    public final Strings strings;
    public final AudioManager audio;
    public final AchievementManager achievements;
    /** All player slots (1 in solo; up to 4 in multiplayer). Systems use the currently bound hero's fields below. */
    public final Hero[] heroes = new Hero[4];
    public int heroCount = 1;
    /** Index of the hero controlled by this device's (first) user. */
    public int localHero;
    public Hero hero;
    public Player player;
    public PlayerController playerCtl;
    public Joystick joystick;
    public WeaponManager weapons;
    public XpManager xp;
    public UpgradeManager upgrades;
    public PetManager pets;
    public final Player.PermanentBonus bonus = new Player.PermanentBonus();
    public final Camera camera = new Camera();
    public final SpatialGrid grid = new SpatialGrid(Config.WORLD_HALF, Config.GRID_CELL, EnemyManager.CAPACITY);
    public final EnemyManager enemies;
    public final EnemyAI ai;
    public final CombatManager combat;
    public final WaveManager waves;
    public final BossManager bosses;
    public final ParticleSystem fx;
    public final UIManager ui;
    public final WorldRenderer world;
    public Renderer3D world3D;
    public HudLayout layout;
    public Sprites sprites;
    /** Pointer used for first-person look (3D). */
    private int lookPointer = -1;
    private float lookX, lookY;
    public boolean view3D() { return settings.view3D && !splitScreen; }
    public final WorldMap map = new WorldMap();
    public final LootManager loot;
    public final ShopManager shop;
    public final MemeSounds meme;
    public final DevOptions dev = new DevOptions();
    public final NetEvents netEvents = new NetEvents();
    public final MultiplayerManager mpm;

    public int state = S_MENU;
    public int settingsReturn = S_MENU;
    public float viewW = Config.VIRTUAL_WIDTH, viewH = 1280, scale = 1f;
    public float realTime;

    // ---- current run ----
    public float time;
    public int kills, killScore, bossScore, bossesKilled, achievementsThisRun, score;
    public final int[] killsByType = new int[Config.ENEMIES.length];
    public boolean newBest, mapBest, pendingBossReward, runActive, reviveUsed;
    public float coinsF, deathSlowmo;
    public int coinsThisRun, bulletsDestroyed;
    public float deathTimer, gameOverTimer;
    /** Current match mode and match clock (PvP modes end at matchLength). */
    public int mode = M_SOLO;
    public float matchLength, pvpDropTimer;
    /** Two players on one device (left/right screen halves). */
    public boolean splitScreen;
    /** Network role (null = offline). */
    public com.blockkillers.core.net.NetSession net;
    public int netHostState;
    public boolean mp() { return mode != M_SOLO; }
    public boolean pvp() { return mode == M_PVP || mode == M_PVPVE; }
    public boolean isClient() { return net != null && net.isClient(); }
    /** Testing aid: player cannot take damage. */
    public boolean godMode;

    // adaptive performance
    private float slowFrameTime;

    public Game(Platform platform) {
        this.platform = platform;
        settings = new SettingsManager(platform.storage());
        save = new SaveManager(platform.storage());
        strings = new Strings(this);
        audio = new AudioManager(platform.audio(), settings);
        audio.recorder = netEvents;
        achievements = new AchievementManager(this);
        enemies = new EnemyManager(this);
        ai = new EnemyAI(this);
        combat = new CombatManager(this);
        waves = new WaveManager(this);
        bosses = new BossManager(this);
        fx = new ParticleSystem(this);
        loot = new LootManager(this);
        shop = new ShopManager(platform.storage(), save);
        meme = new MemeSounds(this);
        for (int i = 0; i < heroes.length; i++) heroes[i] = new Hero(this, i);
        heroes[0].active = true; heroes[0].local = true;
        bind(heroes[0]);
        mpm = new MultiplayerManager(this);
        ui = new UIManager(this);
        world = new WorldRenderer(this);
        world3D = new Renderer3D(this);
        layout = new HudLayout(platform.storage());
        sprites = new Sprites(platform);
        player.reset(bonus);
        map.generate(1, WorldMap.OPEN);
    }

    /** Points the per-player fields at a hero so every system acts on that player. */
    public void bind(Hero h) {
        hero = h; player = h.player; playerCtl = h.ctl; joystick = h.joystick;
        weapons = h.weapons; xp = h.xp; upgrades = h.upgrades; pets = h.pets;
    }
    public void bindLocal() { bind(heroes[localHero]); }

    /** Nearest hero that can be attacked (alive, not downed); falls back to the bound hero. */
    public Hero nearestHero(float x, float y) {
        Hero best = null; float bd = Float.MAX_VALUE;
        for (int i = 0; i < heroCount; i++) {
            Hero h = heroes[i];
            if (!h.canAct()) continue;
            float d = MathUtil.dist2(x, y, h.player.x, h.player.y);
            if (d < bd) { bd = d; best = h; }
        }
        return best != null ? best : hero;
    }
    /** Distance squared from a point to the closest active hero. */
    public float nearestHeroDist2(float x, float y) {
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < heroCount; i++) { Hero h = heroes[i]; if (h.active) bd = Math.min(bd, MathUtil.dist2(x, y, h.player.x, h.player.y)); }
        return bd;
    }

    /** Must be called whenever the surface size changes (pixels). */
    public void resize(int pxW, int pxH) {
        scale = pxW / Config.VIRTUAL_WIDTH;
        viewW = Config.VIRTUAL_WIDTH;
        viewH = pxH / scale;
        ui.layout();
    }

    public void platformVibrate(int ms) { if (settings.vibration) platform.vibrate(ms); }

    // ------------------------------------------------------------------ state changes
    /** Starts a run on the given map (also remembered as the default). */
    public void startRun(int mapMode) {
        save.selectedMap = MathUtil.clamp(mapMode, 0, WorldMap.MODES - 1);
        save.save();
        startRun();
    }

    public void startRun() {
        achievements.fillBonus(bonus);
        shop.apply(bonus);
        Player.PermanentBonus none = new Player.PermanentBonus();
        map.generate(net != null ? net.mapSeed() : MathUtil.RNG.nextLong(), save.selectedMap);
        loot.reset();
        enemies.clear(); combat.clear(); fx.clear();
        waves.reset(); bosses.reset();
        achievements.clearToasts();
        time = 0; kills = killScore = bossScore = bossesKilled = achievementsThisRun = score = 0;
        for (int i = 0; i < killsByType.length; i++) killsByType[i] = 0;
        newBest = false; pendingBossReward = false; deathTimer = 0; gameOverTimer = 0;
        coinsF = 0; coinsThisRun = 0; bulletsDestroyed = 0; reviveUsed = false; deathSlowmo = 0;
        matchLength = mode == M_PVP ? Config.PVP_MATCH : (mode == M_PVPVE ? Config.PVPVE_MATCH : 0);
        pvpDropTimer = 8f;
        meme.reset();
        for (int i = 0; i < heroCount; i++) {
            Hero h = heroes[i];
            if (!h.active) continue;
            bind(h);
            // shop/achievement bonuses: always in solo & co-op, never in PvP (fair duels)
            player.reset(pvp() ? none : bonus);
            weapons.reset(); xp.reset(); playerCtl.reset(); joystick.reset(); pets.reset();
            h.resetMatchStats();
            spawnPoint(h, i);
            if (!pvp()) for (int k = 0; k < shop.arsenal(); k++) weapons.addInstance(loot.rollRandom(Loot.UNCOMMON, 1f));
            if (h.local && i == localHero) dev.applyToRun(this);
        }
        bindLocal();
        camera.snap(player.x, player.y);
        runActive = true;
        state = S_PLAYING;
        audio.musicOn();
    }

    /** Spread players out at the start / on respawn. */
    private void spawnPoint(Hero h, int i) {
        Player p = h.player;
        if (pvp() && time > 0) {
            float[] pt = new float[2];
            for (int tries = 0; tries < 20; tries++) {
                pt[0] = MathUtil.rand(-Math.min(map.hw, 1400), Math.min(map.hw, 1400));
                pt[1] = map.corridor() ? 0 : MathUtil.rand(-Math.min(map.hh, 1400), Math.min(map.hh, 1400));
                if (!map.blocked(pt[0], pt[1], 60)) break;
            }
            p.x = pt[0]; p.y = pt[1];
            return;
        }
        float spread = pvp() ? 420f : 70f;
        if (map.corridor()) { p.x = (i - (heroCount - 1) / 2f) * spread; p.y = 0; return; }
        float a = i * MathUtil.TAU / Math.max(1, heroCount);
        p.x = heroCount == 1 ? 0 : MathUtil.cos(a) * spread; p.y = heroCount == 1 ? 0 : MathUtil.sin(a) * spread;
    }

    /** A random player to spawn enemies around. */
    public Player spawnFocus() {
        if (heroCount == 1) return heroes[0].player;
        int n = MathUtil.randInt(heroCount);
        for (int k = 0; k < heroCount; k++) { Hero h = heroes[(n + k) % heroCount]; if (h.canAct()) return h.player; }
        return heroes[localHero].player;
    }

    /** Coins: run coins belong to the hero; the local hero's coins are banked at the end. */
    public void addCoins(Hero h, float c) {
        h.coinsF += c * shop.coinMult();
        int whole = (int) h.coinsF;
        if (whole > 0) { h.coinsF -= whole; h.coins += whole; }
    }

    /** XP: shared by everyone in solo/co-op, only to the killer in PvP modes. */
    public void awardXp(float amount, Hero killer) {
        Hero prev = hero;
        for (int i = 0; i < heroCount; i++) {
            Hero h = heroes[i];
            if (!h.active || (pvp() && killer != null && h != killer)) continue;
            bind(h);
            xp.addXp(amount);
        }
        bind(prev);
    }

    public void restart() {
        if (runActive && state != S_GAMEOVER) finishRun(false);
        startRun();
    }

    public void pause() {
        if (state == S_PLAYING && mp()) { ui.mp.pauseOpen = true; return; }   // multiplayer never freezes the match
        if (state == S_PLAYING) { state = S_PAUSED; joystick.reset(); }
    }
    public void resume() { if (state == S_PAUSED) { state = S_PLAYING; joystick.reset(); } }

    public void toMenu() {
        if (mp() || net != null) { if (runActive && !isClient()) finishRun(false); mpm.leave(); audio.musicOn(); return; }
        if (runActive && state != S_GAMEOVER) finishRun(false);
        runActive = false;
        state = S_MENU;
        joystick.reset();
        audio.musicOn();
    }

    public void openSettings() { settingsReturn = state; state = S_SETTINGS; }
    public void closeSettings() { settings.save(); audio.refreshVolume(); state = settingsReturn; }

    /** Coins for everyone (bosses, chests). */
    public void addCoins(float c) { for (int i = 0; i < heroCount; i++) if (heroes[i].active) addCoins(heroes[i], c); }

    public void onPlayerDeath() {
        if (!player.alive || hero.downed) return;
        if (mp()) { onHeroDown(hero); return; }
        if (shop.secondWind() && !reviveUsed) {
            reviveUsed = true;
            player.hp = player.maxHp * 0.5f;
            player.invuln = 2.5f;
            combat.nova();
            ui.announce(strings.get("second_wind"), null);
            return;
        }
        player.alive = false;
        deathTimer = 2.8f;
        deathSlowmo = 0;
        meme.onDeath();
        fx.burst(player.x, player.y, 0xFF40C4FF, 50, 500, 9);
        fx.ring(player.x, player.y, 260, 0xFFFFFFFF, 0.8f);
        camera.addShake(1f);
        platformVibrate(400);
    }

    /** Multiplayer: co-op players go down (revivable); PvP players respawn. */
    private void onHeroDown(Hero h) {
        h.downed = true; h.downTime = 0; h.reviveProgress = 0; h.deaths++;
        h.joystick.reset(); h.cardsOpen = false;
        fx.burst(h.player.x, h.player.y, h.color, 40, 420, 8);
        fx.ring(h.player.x, h.player.y, 180, 0xFFFFFFFF, 0.6f);
        if (h.local) { platformVibrate(300); camera.addShake(0.7f); }
        meme.onDeath();
        if (pvp()) {
            h.respawnTimer = mode == M_PVP ? Config.PVP_RESPAWN : Config.PVPVE_RESPAWN;
            Hero k = h.lastAttacker;
            if (k != null && k != h) {
                k.pvpKills++; k.score += Config.PVP_KILL_SCORE;
                ui.announce(k.name + " ⚔ " + h.name, null);
            }
        } else {
            ui.announce(h.name + " " + strings.get("is_down"), strings.get("revive_hint"));
        }
    }

    private void updateHeroesMp(float dt) {
        for (int i = 0; i < heroCount; i++) {
            Hero h = heroes[i];
            if (!h.active || !h.downed) continue;
            h.downTime += dt;
            if (pvp()) {
                h.respawnTimer -= dt;
                if (h.respawnTimer <= 0) {
                    h.downed = false;
                    Player p = h.player;
                    p.hp = p.maxHp; p.invuln = 2f;
                    spawnPoint(h, i);
                    fx.ring(p.x, p.y, 120, h.color, 0.5f);
                }
                continue;
            }
            // co-op revive: stand next to a downed teammate
            boolean helper = false;
            for (int j = 0; j < heroCount; j++) {
                Hero o = heroes[j];
                if (o == h || !o.canAct()) continue;
                if (MathUtil.dist2(o.player.x, o.player.y, h.player.x, h.player.y) < Config.REVIVE_RANGE * Config.REVIVE_RANGE) helper = true;
            }
            h.reviveProgress = helper ? h.reviveProgress + dt : Math.max(0, h.reviveProgress - dt * 0.5f);
            if (h.reviveProgress >= Config.REVIVE_TIME) {
                h.downed = false; h.reviveProgress = 0;
                h.player.hp = h.player.maxHp * 0.4f; h.player.invuln = 2f;
                fx.ring(h.player.x, h.player.y, 140, 0xFF00E676, 0.6f);
                audio.play(Sfx.LEVEL_UP);
            }
        }
    }

    /** Co-op is lost when every player is down at the same time. */
    private boolean allDown() {
        for (int i = 0; i < heroCount; i++) if (heroes[i].active && !heroes[i].downed) return false;
        return true;
    }

    private void finishRun(boolean died) {
        computeScore();
        Hero me = heroes[localHero];
        coinsThisRun = me.coins;
        if (mp()) { save.addCoins(coinsThisRun); runActive = false; return; }   // MP: only coins count toward progression
        newBest = save.recordRun(score, kills, time, player.level, bossesKilled);
        mapBest = save.recordMapBest(map.mode, score);
        save.recordLoot(loot.legendariesThisRun, loot.chestsThisRun);
        save.addCoins(coinsThisRun);
        runActive = false;
    }

    private void computeScore() {
        if (mp()) {
            Hero me = heroes[localHero];
            score = me.score + (pvp() ? 0 : (int) time * Config.SCORE_PER_SECOND + bossScore);
            coinsThisRun = me.coins;
            return;
        }
        coinsThisRun = heroes[0].coins;
        score = killScore + bossScore + (int) time * Config.SCORE_PER_SECOND + achievementsThisRun * Config.SCORE_PER_ACHIEVEMENT;
    }

    // ------------------------------------------------------------------ loop
    public void update(float dt) {
        dt = Math.min(dt, 0.1f);
        realTime += dt;
        audio.tick(dt);
        ui.update(dt);
        achievements.update(dt);
        adapt(dt);
        if (state == S_LEVELUP && !player.alive) state = S_PLAYING;   // never get stuck on cards after dying
        mpm.update(dt);
        if (isClient()) { if (state == S_PLAYING) mpm.clientFrame(dt); else if (state == S_MPRESULT) { gameOverTimer += dt; fx.update(dt); } }
        else if (state == S_PLAYING) {
            // sub-step large frames for stable collisions
            int steps = (int) Math.ceil(dt / (1f / 60f));
            steps = Math.max(1, Math.min(steps, 4));
            float h = dt / steps;
            for (int i = 0; i < steps && state == S_PLAYING; i++) step(h);
        } else if (state == S_MENU || state == S_SHOP || state == S_DEV || state == S_MAPSELECT || state == S_MPMENU || state == S_LOBBY) {
            ui.updateMenuBackground(dt);
        } else if (state == S_GAMEOVER) {
            gameOverTimer += dt;
            fx.update(dt);
        }
    }

    private void step(float dt) {
        bindLocal();
        boolean lost = mp() ? (!pvp() && allDown()) : !player.alive;
        if (lost) {
            if (mp() && player.alive) { for (int i = 0; i < heroCount; i++) heroes[i].player.alive = false; deathTimer = 2.8f; deathSlowmo = 0; }
            // dramatic slow-motion death: the world crawls while the camera pushes in
            deathTimer -= dt;
            deathSlowmo += dt;
            float slow = dt * 0.25f;
            fx.update(slow);
            grid.build(enemies.list);
            enemies.update(slow);
            camera.targetZoom = Config.zoomFor(player.level, time) * 1.35f;
            camera.update(dt, player.x, player.y, settings.screenShake);
            enemies.cleanup();
            if (deathTimer <= 0) {
                finishRun(true);
                meme.onGameOverScreen();
                state = mp() ? S_MPRESULT : S_GAMEOVER;
                gameOverTimer = 0;
                joystick.reset();
            }
            return;
        }
        time += dt;
        grid.build(enemies.list);
        if (mode != M_PVP) { waves.update(dt); bosses.update(dt); }
        else pvpDrops(dt);
        for (int i = 0; i < heroCount; i++) {
            Hero h = heroes[i];
            if (!h.active) continue;
            bind(h);
            if (!h.local) { playerCtl.overrideInput = true; playerCtl.inputX = h.netX; playerCtl.inputY = h.netY;
                if (h.netAbilitySeq != h.lastAbilitySeq) { h.lastAbilitySeq = h.netAbilitySeq; playerCtl.useAbility(); } }
            if (h.canAct()) { playerCtl.update(dt); weapons.update(dt); pets.update(dt); }
        }
        bindLocal();
        if (splitScreen) tether();
        meme.update(dt);
        enemies.update(dt);
        combat.update(dt);
        enemies.cleanup();
        fx.update(dt);
        loot.update(dt);
        if (mp()) updateHeroesMp(dt);
        bindLocal();
        camera.targetZoom = Config.zoomFor(player.level, time) * (splitScreen ? 0.85f : 1f);
        float cx = player.x, cy = player.y;
        if (splitScreen) { cx = (heroes[0].player.x + heroes[1].player.x) / 2; cy = (heroes[0].player.y + heroes[1].player.y) / 2; }
        camera.update(dt, cx, cy, settings.screenShake);
        computeScore();
        if (!mp()) achievements.check();

        if (!mp()) {
            if (player.alive && (xp.pendingLevelUps > 0 || pendingBossReward)) openLevelUp();
        } else {
            pendingBossReward = false;
            for (int i = 0; i < heroCount; i++) {
                Hero h = heroes[i];
                if (!h.active || h.cardsOpen || !h.canAct() || h.xp.pendingLevelUps <= 0) continue;
                bind(h);
                upgrades.roll(false);
                h.cardsOpen = upgrades.cardCount > 0;
                if (!h.cardsOpen) h.xp.pendingLevelUps = 0;
                else if (h.local) audio.play(Sfx.LEVEL_UP);
            }
            bindLocal();
            if (matchLength > 0 && time >= matchLength) { finishRun(false); state = S_MPRESULT; gameOverTimer = 0; audio.play(Sfx.ACHIEVEMENT); }
        }
    }

    /** Split-screen: keep both local players within one screen. */
    private void tether() {
        Player a = heroes[0].player, b = heroes[1].player;
        float max = Math.min(viewW, viewH) * 0.9f / camera.zoom;
        float dx = b.x - a.x, dy = b.y - a.y, d = MathUtil.len(dx, dy);
        if (d > max) {
            float pull = (d - max) / 2f / d;
            a.x += dx * pull; a.y += dy * pull; b.x -= dx * pull; b.y -= dy * pull;
        }
    }

    /** PvP: guns rain in near the players (there are no enemies to drop them). */
    private void pvpDrops(float dt) {
        pvpDropTimer -= dt;
        if (pvpDropTimer > 0) return;
        pvpDropTimer = 9f;
        Player f = spawnFocus();
        float a = MathUtil.rand(0, MathUtil.TAU), r = MathUtil.rand(200, 500);
        loot.drop(map.clampX(f.x + MathUtil.cos(a) * r, 60), map.clampY(f.y + MathUtil.sin(a) * r, 60), loot.rollRandom(Loot.UNCOMMON, 1.5f));
    }

    /** Multiplayer (non-pausing) card choice for a hero. */
    public void chooseCard(Hero h, int index) {
        if (!h.cardsOpen) return;
        Hero prev = hero;
        bind(h);
        upgrades.apply(index);
        if (xp.pendingLevelUps > 0) xp.pendingLevelUps--;
        h.cardsOpen = false;
        if (h.local) audio.play(Sfx.SELECT);
        bind(prev);
    }

    private void openLevelUp() {
        boolean boss = pendingBossReward;
        upgrades.roll(boss);
        if (upgrades.cardCount == 0) { pendingBossReward = false; xp.pendingLevelUps = 0; return; }
        state = S_LEVELUP;
        joystick.reset();
        ui.openLevelUp();
        audio.play(Sfx.LEVEL_UP);
    }

    public void chooseUpgrade(int index) {
        if (state != S_LEVELUP) return;
        upgrades.apply(index);
        audio.play(Sfx.SELECT);
        if (upgrades.bossReward) pendingBossReward = false; else if (xp.pendingLevelUps > 0) xp.pendingLevelUps--;
        state = S_PLAYING;
        if (xp.pendingLevelUps > 0 || pendingBossReward) openLevelUp();
    }

    // ------------------------------------------------------------------ loot & map screens
    public void openLoot() {
        if (state != S_PLAYING || loot.nearby == null) return;
        loot.viewing = loot.nearby;
        state = S_LOOT;
        joystick.reset();
        ui.openLoot();
        audio.play(Sfx.CLICK);
    }
    public void closeLoot() { if (state == S_LOOT) { loot.viewing = null; state = S_PLAYING; joystick.reset(); } }
    public void openMap() { if (state == S_PLAYING) { state = S_MAP; joystick.reset(); audio.play(Sfx.CLICK); } }
    public void closeMap() { if (state == S_MAP) { state = S_PLAYING; joystick.reset(); } }

    /** Lowers particle budgets if frames are consistently slow. */
    private void adapt(float dt) {
        if (state != S_PLAYING) return;
        if (dt > 1f / 40f) slowFrameTime += dt; else slowFrameTime = Math.max(0, slowFrameTime - dt * 0.5f);
        if (slowFrameTime > 2f && fx.adaptive > 0.3f) { fx.adaptive -= 0.15f; slowFrameTime = 0; }
    }

    // ------------------------------------------------------------------ host lifecycle
    /** App sent to background. */
    public void onHostPause() {
        pause();
        settings.save();
        save.save();
        audio.musicOff();
    }
    public void onHostResume() {
        audio.markMusicStopped();
        audio.musicOn();
    }
    /** Hardware back button. Returns false if the app should close. */
    public boolean onBack() {
        switch (state) {
            case S_PLAYING: pause(); return true;
            case S_PAUSED: resume(); return true;
            case S_SETTINGS: closeSettings(); return true;
            case S_STATS: case S_SHOP: case S_DEV: case S_MAPSELECT: state = S_MENU; return true;
            case S_MPMENU: mpm.discovery.stop(); state = S_MENU; return true;
            case S_LAYOUT: layout.save(); state = S_SETTINGS; return true;
            case S_LOBBY: case S_MPRESULT: toMenu(); return true;
            case S_GAMEOVER: toMenu(); return true;
            case S_LEVELUP: return true;
            case S_LOOT: closeLoot(); return true;
            case S_MAP: closeMap(); return true;
            default: return false;
        }
    }

    // ------------------------------------------------------------------ input (virtual coordinates)
    public void touchDown(int id, float px, float py) {
        float x = px / scale, y = py / scale;
        if (ui.touchDown(id, x, y)) return;
        if (state != S_PLAYING) return;
        // 3D: the right half of the screen is the look pad
        if (view3D() && x > viewW * 0.5f) { lookPointer = id; lookX = x; lookY = y; return; }
        // split screen: the left half steers player 1, the right half player 2
        Hero h = splitScreen ? heroes[x < viewW / 2 ? 0 : 1] : heroes[localHero];
        if (h.canAct()) h.joystick.down(id, x, y, settings.sensitivity);
    }
    public void touchMove(int id, float px, float py) {
        float x = px / scale, y = py / scale;
        ui.touchMove(id, x, y);
        if (id == lookPointer) {
            Player p = heroes[localHero].player;
            p.view += (x - lookX) * 0.0085f * settings.sensitivity;
            p.viewPitch = MathUtil.clamp(p.viewPitch - (y - lookY) * 0.4f, -120, 120);
            lookX = x; lookY = y;
            return;
        }
        for (int i = 0; i < heroCount; i++) if (heroes[i].local) heroes[i].joystick.move(id, x, y);
    }
    public void touchUp(int id, float px, float py) {
        float x = px / scale, y = py / scale;
        ui.touchUp(id, x, y);
        if (id == lookPointer) { lookPointer = -1; return; }
        for (int i = 0; i < heroCount; i++) if (heroes[i].local) heroes[i].joystick.up(id);
    }

    // ------------------------------------------------------------------ render
    public void render(Gfx g) {
        g.save();
        g.scale(scale);
        if (state == S_MENU || state == S_STATS || state == S_SHOP || state == S_DEV || state == S_MAPSELECT || state == S_MPMENU || state == S_LOBBY || state == S_LAYOUT || (state == S_SETTINGS && settingsReturn == S_MENU)) {
            ui.renderMenuBackground(g);
        } else {
            if (view3D()) world3D.render(g); else world.render(g);
        }
        ui.render(g);
        g.restore();
    }
}
