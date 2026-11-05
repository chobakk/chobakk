package com.blockkillers.core;

/**
 * Root of the game: owns every manager, runs the state machine, steps the simulation and dispatches input.
 * Contains no platform code, so the exact same game runs on Android and in the desktop test harness.
 */
public final class Game {
    public static final int S_MENU = 0, S_PLAYING = 1, S_PAUSED = 2, S_LEVELUP = 3, S_GAMEOVER = 4, S_SETTINGS = 5, S_STATS = 6;

    public final Platform platform;
    public final SettingsManager settings;
    public final SaveManager save;
    public final Strings strings;
    public final AudioManager audio;
    public final AchievementManager achievements;
    public final Player player = new Player();
    public final Player.PermanentBonus bonus = new Player.PermanentBonus();
    public final PlayerController playerCtl;
    public final Joystick joystick = new Joystick();
    public final Camera camera = new Camera();
    public final SpatialGrid grid = new SpatialGrid(Config.WORLD_HALF, Config.GRID_CELL, EnemyManager.CAPACITY);
    public final EnemyManager enemies;
    public final EnemyAI ai;
    public final CombatManager combat;
    public final WeaponManager weapons;
    public final XpManager xp;
    public final UpgradeManager upgrades;
    public final WaveManager waves;
    public final BossManager bosses;
    public final ParticleSystem fx;
    public final UIManager ui;
    public final WorldRenderer world;

    public int state = S_MENU;
    public int settingsReturn = S_MENU;
    public float viewW = Config.VIRTUAL_WIDTH, viewH = 1280, scale = 1f;
    public float realTime;

    // ---- current run ----
    public float time;
    public int kills, killScore, bossScore, bossesKilled, achievementsThisRun, score;
    public final int[] killsByType = new int[Config.ENEMIES.length];
    public boolean newBest, pendingBossReward, runActive;
    public float deathTimer, gameOverTimer;
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
        achievements = new AchievementManager(this);
        playerCtl = new PlayerController(this);
        enemies = new EnemyManager(this);
        ai = new EnemyAI(this);
        combat = new CombatManager(this);
        weapons = new WeaponManager(this);
        xp = new XpManager(this);
        upgrades = new UpgradeManager(this);
        waves = new WaveManager(this);
        bosses = new BossManager(this);
        fx = new ParticleSystem(this);
        ui = new UIManager(this);
        world = new WorldRenderer(this);
        player.reset(bonus);
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
    public void startRun() {
        achievements.fillBonus(bonus);
        player.reset(bonus);
        enemies.clear(); combat.clear(); fx.clear();
        weapons.reset(); xp.reset(); waves.reset(); bosses.reset(); playerCtl.reset();
        joystick.reset();
        achievements.clearToasts();
        time = 0; kills = killScore = bossScore = bossesKilled = achievementsThisRun = score = 0;
        for (int i = 0; i < killsByType.length; i++) killsByType[i] = 0;
        newBest = false; pendingBossReward = false; deathTimer = 0; gameOverTimer = 0;
        camera.snap(0, 0);
        runActive = true;
        state = S_PLAYING;
        audio.musicOn();
    }

    public void restart() {
        if (runActive && state != S_GAMEOVER) finishRun(false);
        startRun();
    }

    public void pause() {
        if (state == S_PLAYING) { state = S_PAUSED; joystick.reset(); }
    }
    public void resume() { if (state == S_PAUSED) { state = S_PLAYING; joystick.reset(); } }

    public void toMenu() {
        if (runActive && state != S_GAMEOVER) finishRun(false);
        runActive = false;
        state = S_MENU;
        joystick.reset();
        audio.musicOn();
    }

    public void openSettings() { settingsReturn = state; state = S_SETTINGS; }
    public void closeSettings() { settings.save(); audio.refreshVolume(); state = settingsReturn; }

    public void onPlayerDeath() {
        if (!player.alive) return;
        player.alive = false;
        deathTimer = 1.2f;
        fx.burst(player.x, player.y, 0xFF40C4FF, 50, 500, 9);
        fx.ring(player.x, player.y, 260, 0xFFFFFFFF, 0.8f);
        camera.addShake(1f);
        platformVibrate(400);
        audio.play(Sfx.GAME_OVER);
    }

    private void finishRun(boolean died) {
        computeScore();
        newBest = save.recordRun(score, kills, time, player.level, bossesKilled);
        runActive = false;
    }

    private void computeScore() {
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
        if (state == S_PLAYING) {
            // sub-step large frames for stable collisions
            int steps = (int) Math.ceil(dt / (1f / 60f));
            steps = Math.max(1, Math.min(steps, 4));
            float h = dt / steps;
            for (int i = 0; i < steps && state == S_PLAYING; i++) step(h);
        } else if (state == S_MENU) {
            ui.updateMenuBackground(dt);
        } else if (state == S_GAMEOVER) {
            gameOverTimer += dt;
            fx.update(dt);
        }
    }

    private void step(float dt) {
        if (!player.alive) {
            deathTimer -= dt;
            fx.update(dt);
            enemies.update(dt);
            enemies.cleanup();
            camera.update(dt, player.x, player.y, settings.screenShake);
            if (deathTimer <= 0) {
                finishRun(true);
                state = S_GAMEOVER;
                gameOverTimer = 0;
                joystick.reset();
            }
            return;
        }
        time += dt;
        grid.build(enemies.list);
        waves.update(dt);
        bosses.update(dt);
        playerCtl.update(dt);
        weapons.update(dt);
        enemies.update(dt);
        combat.update(dt);
        enemies.cleanup();
        fx.update(dt);
        camera.update(dt, player.x, player.y, settings.screenShake);
        computeScore();
        achievements.check();

        if (player.alive && (xp.pendingLevelUps > 0 || pendingBossReward)) openLevelUp();
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
            case S_STATS: state = S_MENU; return true;
            case S_GAMEOVER: toMenu(); return true;
            case S_LEVELUP: return true;
            default: return false;
        }
    }

    // ------------------------------------------------------------------ input (virtual coordinates)
    public void touchDown(int id, float px, float py) {
        float x = px / scale, y = py / scale;
        if (ui.touchDown(id, x, y)) return;
        if (state == S_PLAYING && player.alive) joystick.down(id, x, y, settings.sensitivity);
    }
    public void touchMove(int id, float px, float py) {
        float x = px / scale, y = py / scale;
        ui.touchMove(id, x, y);
        joystick.move(id, x, y);
    }
    public void touchUp(int id, float px, float py) {
        float x = px / scale, y = py / scale;
        ui.touchUp(id, x, y);
        joystick.up(id);
    }

    // ------------------------------------------------------------------ render
    public void render(Gfx g) {
        g.save();
        g.scale(scale);
        if (state == S_MENU || state == S_STATS || (state == S_SETTINGS && settingsReturn == S_MENU)) {
            ui.renderMenuBackground(g);
        } else {
            world.render(g);
        }
        ui.render(g);
        g.restore();
    }
}
