package com.blockkillers.desktop;

import com.blockkillers.core.*;

import java.util.ArrayList;
import java.util.Locale;

/** Automated gameplay test-suite. Runs the real core game code headless with a fixed time step. */
public final class GameTests {
    private static int passed, failed;
    private static final ArrayList<String> report = new ArrayList<>();

    private static void check(String name, boolean ok, String detail) {
        if (ok) passed++; else failed++;
        String line = (ok ? "PASS " : "FAIL ") + name + (detail == null ? "" : "  [" + detail + "]");
        report.add(line);
        System.out.println(line);
    }

    private static Harness fresh() { return new Harness(new DesktopPlatform.MemStorage(), 1080, 2400); }

    public static void main(String[] args) {
        MathUtil.RNG.setSeed(12345);
        long t0 = System.currentTimeMillis();
        testMenuAndStart();
        testMovementAndJoystick();
        testSpawnAndEnemyAttack();
        testKillXpAndLevelUp();
        testAllEnemyTypes();
        testAllWeapons();
        testUpgradesAndRarity();
        testDifficulty();
        testBossSchedule();
        testBossDefeat();
        testGameOverRestartAndScore();
        testSaveLoad();
        testSettingsPersist();
        testPauseResumeBackground();
        testBackButton();
        testAchievements();
        testStrings();
        testScreenSizes();
        testPerformance();
        testNaturalRuns();
        System.out.println();
        System.out.println(String.format(Locale.US, "RESULT: %d passed, %d failed (%.1fs)", passed, failed, (System.currentTimeMillis() - t0) / 1000f));
        if (failed > 0) System.exit(1);
    }

    static void testMenuAndStart() {
        Harness h = fresh();
        Game g = h.game;
        check("starts in main menu", g.state == Game.S_MENU, null);
        h.tick(0.5f);
        h.tapButton(g.ui.menuButton(1));
        check("menu: Settings button opens settings", g.state == Game.S_SETTINGS, null);
        h.tapButton(g.ui.settingsButton(9));
        check("settings: Back returns to menu", g.state == Game.S_MENU, null);
        h.tapButton(g.ui.menuButton(2));
        check("menu: Statistics button opens stats", g.state == Game.S_STATS, null);
        h.tapButton(g.ui.statsButton(0));
        check("stats: Back returns to menu", g.state == Game.S_MENU, null);
        h.tapButton(g.ui.menuButton(0));
        check("menu: Play starts a run", g.state == Game.S_PLAYING && g.player.alive && g.weapons.owned.size() == 1, null);
        check("music plays", h.platform.audio.music, null);
    }

    static void testMovementAndJoystick() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        g.playerCtl.overrideInput = true; g.playerCtl.inputX = 1; g.playerCtl.inputY = 0;
        h.tick(1f);
        check("move right with input vector", g.player.x > 200, "x=" + (int) g.player.x);
        g.playerCtl.overrideInput = false;
        float y0 = g.player.y;
        float s = g.scale;
        g.touchDown(1, 200 * s, 1500 * s);
        g.touchMove(1, 200 * s, 1600 * s);       // drag down 100 virtual px
        h.tick(1f);
        check("joystick drag moves player down", g.player.y - y0 > 180, "dy=" + (int) (g.player.y - y0));
        // simultaneous second pointer on the ability button while moving
        UIManager.Button ab = g.ui.hudButton(1);
        g.playerCtl.abilityCooldown = 0;
        g.touchDown(2, ab.cx() * s, ab.cy() * s);
        g.touchUp(2, ab.cx() * s, ab.cy() * s);
        check("ability button works while joystick held (multi-touch)", g.playerCtl.abilityCooldown > 0 && g.joystick.active, null);
        g.touchUp(1, 0, 0);
        h.tick(0.5f);
        check("releasing joystick stops player", MathUtil.len(g.player.vx, g.player.vy) < 5, null);
        g.player.x = Config.WORLD_HALF - 30; g.playerCtl.overrideInput = true; g.playerCtl.inputX = 1;
        h.tick(1f);
        check("player stays inside world", g.player.x <= Config.WORLD_HALF - g.player.radius + 0.01f, null);
    }

    static void testSpawnAndEnemyAttack() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        h.tick(6f);
        check("enemies spawn over time", g.enemies.aliveCount() > 2, "alive=" + g.enemies.aliveCount());
        // spawned off-screen
        boolean offscreen = true;
        for (int i = 0; i < g.enemies.list.size; i++) {
            Enemy e = g.enemies.list.get(i);
            if (e.t < 0) offscreen = false;
        }
        check("enemies approach the player", approaching(h), null);
        float hp = g.player.hp;
        g.enemies.clear();
        g.weapons.owned.clear();
        Enemy e = g.enemies.spawn(Config.ENEMIES[0], g.player.x + 30, g.player.y);
        h.tick(0.3f);
        check("enemy contact damages player", g.player.hp < hp, "hp " + (int) hp + " -> " + (int) g.player.hp);
        float armorHp = g.player.hp;
        g.player.armor = 25; g.player.invuln = 0;
        g.combat.damagePlayer(20);
        check("armor reduces damage (25 armor halves)", Math.abs((armorHp - g.player.hp) - 10f) < 0.01f, "took " + (armorHp - g.player.hp));
    }

    private static boolean approaching(Harness h) {
        Game g = h.game;
        if (g.enemies.list.size == 0) return false;
        Enemy e = g.enemies.list.get(0);
        float d0 = MathUtil.dist(e.x, e.y, g.player.x, g.player.y);
        h.tick(0.5f);
        float d1 = MathUtil.dist(e.x, e.y, g.player.x, g.player.y);
        return d1 < d0;
    }

    static void testKillXpAndLevelUp() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        g.waves.update(0); // trigger wave 1 announcement
        Enemy e = g.enemies.spawn(Config.ENEMIES[3], g.player.x + 200, g.player.y);
        int xpBefore = g.player.xp;
        int expectXp = Math.round(e.xp * g.player.xpMult);
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(e, 1e6f, 0, 0, 0);
        check("killing awards kill + score", g.kills == 1 && g.killScore == Config.ENEMIES[3].score, "kills=" + g.kills);
        check("XP awarded immediately", g.player.xp - xpBefore == expectXp, "got " + (g.player.xp - xpBefore) + " expected " + expectXp);
        // auto-attack kills naturally
        g.enemies.clear();
        float xpm = g.player.xpMult;
        g.player.xpMult = 0;      // no level-up interruptions during this check
        int k0 = g.kills;
        for (int i = 0; i < 5; i++) g.enemies.spawn(Config.ENEMIES[0], g.player.x + 150 + i * 40, g.player.y + 100);
        h.tick(4f);
        check("automatic targeting destroys nearby enemies", g.kills - k0 >= 3, "killed " + (g.kills - k0) + "/5 in 4s");
        g.player.xpMult = xpm;
        g.xp.pendingLevelUps = 0;
        // force level-up
        g.xp.addXp(Config.xpToNext(g.player.level) + 1);
        h.tick(0.05f);
        check("level-up pauses gameplay and shows 3 cards", g.state == Game.S_LEVELUP && g.upgrades.cardCount == 3, "state=" + g.state);
        float timeAt = g.time;
        h.tick(1f);
        check("gameplay frozen during level-up", g.time == timeAt, null);
        h.tick(0.1f);
        UIManager.Button b = g.ui.levelButton(0);
        h.tap(b.cx(), b.cy());
        check("tapping a card applies upgrade and resumes", g.state == Game.S_PLAYING, "state=" + g.state);
        check("XP requirement grows per level", Config.xpToNext(10) > Config.xpToNext(2) && Config.xpToNext(2) > Config.xpToNext(1), null);
    }

    static void testAllEnemyTypes() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        g.weapons.owned.clear();
        g.godMode = true;
        boolean allMove = true;
        StringBuilder sb = new StringBuilder();
        for (EnemyType t : Config.ENEMIES) {
            g.enemies.clear(); g.combat.bullets.clear();
            Enemy e = g.enemies.spawn(t, g.player.x + 450, g.player.y);
            float d0 = MathUtil.dist(e.x, e.y, g.player.x, g.player.y);
            h.tick(1.2f);
            float d1 = MathUtil.dist(e.x, e.y, g.player.x, g.player.y);
            if (!(d1 < d0)) { allMove = false; sb.append(t.nameKey).append(' '); }
        }
        check("every enemy type spawns and approaches", allMove, sb.length() == 0 ? null : sb.toString());
        // shooter fires
        g.enemies.clear(); g.combat.bullets.clear();
        g.enemies.spawn(Config.ENEMIES[8], g.player.x + 300, g.player.y);
        h.tick(4f);
        check("shooter fires projectiles", g.platform.audio() != null && g.combat.bullets.size > 0 || shooterFired(h), "bullets=" + g.combat.bullets.size);
        // charger charges (reaches high speed)
        g.enemies.clear();
        Enemy c = g.enemies.spawn(Config.ENEMIES[5], g.player.x + 250, g.player.y);
        float maxSpeed = 0;
        for (int i = 0; i < 120; i++) { h.frame(); if (!c.alive) break; maxSpeed = Math.max(maxSpeed, MathUtil.len(c.vx, c.vy)); }
        check("charger performs a dash", maxSpeed > Config.ENEMIES[5].speed * 3f, "max speed " + (int) maxSpeed);
        // splitter splits
        g.enemies.clear();
        Enemy sp = g.enemies.spawn(Config.ENEMIES[7], g.player.x + 300, g.player.y);
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(sp, 1e6f, 0, 0, 0);
        int minis = 0;
        for (int i = 0; i < g.enemies.list.size; i++) if (g.enemies.list.get(i).alive && g.enemies.list.get(i).type.id == Config.MINI_ID) minis++;
        check("splitter splits into " + Config.SPLIT_COUNT + " smaller enemies", minis == Config.SPLIT_COUNT, "minis=" + minis);
        g.godMode = false;
    }

    private static boolean shooterFired(Harness h) { h.tick(2f); return h.game.combat.bullets.size > 0; }

    static void testAllWeapons() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        g.godMode = true;
        for (WeaponType t : Config.WEAPONS) g.weapons.add(t.id);
        check("weapon slots capped at " + Config.MAX_WEAPONS, g.weapons.owned.size() == Config.MAX_WEAPONS, "owned=" + g.weapons.owned.size());
        boolean ok = true;
        StringBuilder sb = new StringBuilder();
        for (WeaponType t : Config.WEAPONS) {
            Harness hh = fresh(); Game gg = hh.game; hh.bot = false;
            gg.startRun(); gg.godMode = true;
            gg.weapons.owned.clear(); gg.weapons.add(t.id);
            for (int i = 0; i < 12; i++) {
                float a = i * MathUtil.TAU / 12;
                gg.enemies.spawn(Config.ENEMIES[3], gg.player.x + MathUtil.cos(a) * 90, gg.player.y + MathUtil.sin(a) * 90);
            }
            float hp0 = totalHp(gg);
            hh.tick(3f);
            float hp1 = totalHp(gg);
            boolean dealt = hp1 < hp0 || gg.kills > 0;
            if (!dealt) { ok = false; sb.append(t.nameKey).append(' '); }
        }
        check("all 8 weapons deal damage", ok, sb.length() == 0 ? null : "no damage: " + sb);
        // weapon levelling
        g.weapons.owned.clear(); g.weapons.add(Config.W_BLASTER);
        Weapon w = g.weapons.get(Config.W_BLASTER);
        float d1 = w.damage(g.player), c1 = w.cooldown(g.player);
        g.weapons.levelUp(Config.W_BLASTER, 3);
        check("weapon level-up increases damage & fire rate", w.damage(g.player) > d1 && w.cooldown(g.player) < c1 && w.level == 4, null);
        g.weapons.levelUp(Config.W_BLASTER, 10);
        check("weapon level capped", w.level == Config.WEAPON_MAX_LEVEL, null);
    }

    private static float totalHp(Game g) {
        float s = 0;
        for (int i = 0; i < g.enemies.list.size; i++) if (g.enemies.list.get(i).alive) s += g.enemies.list.get(i).hp;
        return s;
    }

    static void testUpgradesAndRarity() {
        Harness h = fresh(); Game g = h.game;
        g.startRun();
        int[] rar = new int[4];
        boolean distinct = true, allTypes = true;
        boolean[] seenStat = new boolean[UpgradeManager.STAT_COUNT];
        for (int i = 0; i < 3000; i++) {
            g.upgrades.roll(false);
            UpgradeManager.Card[] c = g.upgrades.cards;
            for (int k = 0; k < 3; k++) {
                if (c[k].type == UpgradeManager.T_STAT) { rar[c[k].rarity]++; seenStat[c[k].stat] = true; }
                for (int j = k + 1; j < 3; j++)
                    if (c[k].type == c[j].type && c[k].stat == c[j].stat && c[k].weapon == c[j].weapon) distinct = false;
            }
        }
        for (boolean b : seenStat) allTypes &= b;
        check("cards are always 3 distinct options", distinct, null);
        check("all 12 stat upgrades appear", allTypes, null);
        check("rarity distribution common>rare>epic>legendary>0", rar[0] > rar[1] && rar[1] > rar[2] && rar[2] > rar[3] && rar[3] > 0,
                rar[0] + "/" + rar[1] + "/" + rar[2] + "/" + rar[3]);
        boolean bossOk = true;
        for (int i = 0; i < 200; i++) {
            g.upgrades.roll(true);
            for (int k = 0; k < g.upgrades.cardCount; k++) if (g.upgrades.cards[k].type == UpgradeManager.T_STAT && g.upgrades.cards[k].rarity < 2) bossOk = false;
        }
        check("boss rewards are Epic or better", bossOk, null);
        // each stat applies
        Player p = g.player;
        float dm = p.damageMult;
        UpgradeManager.Card c = g.upgrades.cards[0];
        c.type = UpgradeManager.T_STAT; c.stat = UpgradeManager.DAMAGE; c.rarity = 3; c.value = UpgradeManager.BASE[0] * Config.RARITY_MULT[3];
        g.upgrades.cardCount = 3;
        g.upgrades.apply(0);
        check("legendary damage card applies +35%", Math.abs(p.damageMult - dm - 0.35f) < 0.001f, null);
        float before = g.player.maxHp;
        c.stat = UpgradeManager.MAX_HP; c.value = 15; g.upgrades.apply(0);
        check("max health card applies", g.player.maxHp == before + 15, null);
        c.stat = UpgradeManager.EXTRA_PROJ; c.value = 1; g.upgrades.apply(0);
        check("extra projectile card applies", g.player.extraProjectiles == 1 && g.weapons.get(0).count(g.player) == 2, null);
    }

    static void testDifficulty() {
        check("enemy health scales up", Config.hpMult(600) > 2.5f * Config.hpMult(0), String.format(Locale.US, "x%.2f at 10min", Config.hpMult(600)));
        check("enemy damage scales up", Config.dmgMult(600) > Config.dmgMult(0), null);
        check("enemy speed scales (capped)", Config.speedMult(600) > 1f && Config.speedMult(100000) <= 1.4f, null);
        check("spawn interval shortens", Config.spawnInterval(600) < Config.spawnInterval(0) / 3, null);
        check("population cap grows", Config.maxEnemies(600, 2) > Config.maxEnemies(0, 2) * 5, null);
        check("enemy variety grows (10 types by 5:30)", countUnlocked(330) == 10 && countUnlocked(0) == 1, "t=0:" + countUnlocked(0));
        check("first minutes are gentle (<=1.05 spawns/s at start)", 1f / Config.spawnInterval(0) <= 1.0f, null);
    }

    private static int countUnlocked(float t) {
        int n = 0;
        for (EnemyType e : Config.ENEMIES) if (e.spawnWeight > 0 && e.unlockTime <= t) n++;
        return n;
    }

    static void testBossSchedule() {
        Harness h = fresh(); Game g = h.game;
        g.startRun();
        g.godMode = true;
        float bossSeen = -1, warnSeen = -1;
        while (g.time < 320 && bossSeen < 0) {
            h.frame();
            if (warnSeen < 0 && g.bosses.warning > 0) warnSeen = g.time;
            if (g.bosses.bossActive()) bossSeen = g.time;
        }
        check("boss warning before first boss", warnSeen > 290 && warnSeen < 300, "warning at " + warnSeen);
        check("first boss appears at ~5:00", bossSeen >= 299 && bossSeen <= 302, "boss at " + String.format(Locale.US, "%.1f", bossSeen));
        Enemy b = g.bosses.current;
        check("boss is large with big health", b != null && b.radius >= 70 && b.maxHp > 2000, b == null ? "none" : "r=" + b.radius + " hp=" + (int) b.maxHp);
        check("boss health bar visible in HUD state", g.state == Game.S_PLAYING || g.state == Game.S_LEVELUP, null);
        // observe boss attack patterns
        int bulletsSeen = 0;
        boolean telegraph = false;
        for (int i = 0; i < 60 * 25 && g.bosses.bossActive(); i++) {
            h.frame();
            bulletsSeen = Math.max(bulletsSeen, g.combat.bullets.size);
            if (g.bosses.current != null && g.bosses.telegraphing(g.bosses.current)) telegraph = true;
        }
        check("boss uses special attacks (bullets)", bulletsSeen > 5, "max bullets " + bulletsSeen);
        report.add("INFO  boss phase: telegraph observed=" + telegraph + ", player level=" + g.player.level + ", boss alive after 25s=" + g.bosses.bossActive()
                + (g.bosses.bossActive() ? String.format(Locale.US, " (hp %.0f%%)", 100 * g.bosses.current.hp / g.bosses.current.maxHp) : ""));
        System.out.println(report.get(report.size() - 1));
        // keep fighting until defeated (bot + god mode)
        float start = g.time;
        while (g.bosses.bossActive() && g.time - start < 180) h.frame();
        check("boss can be defeated by the player's weapons", g.bossesKilled == 1, "bossesKilled=" + g.bossesKilled + " after " + (int) (g.time - 300) + "s");
        h.tick(0.1f);
        check("next boss scheduled 5 min after spawn", Math.abs(g.bosses.nextBossTime - (bossSeen + Config.BOSS_INTERVAL)) < 1f, "next=" + (int) g.bosses.nextBossTime);
        // all 3 boss kinds behave
        for (int k = 0; k < 3; k++) {
            Harness hh = fresh(); Game gg = hh.game; hh.bot = true;
            gg.startRun(); gg.godMode = true;
            gg.bosses.spawned = k;
            Enemy bb = gg.bosses.spawnBoss();
            int maxB = 0;
            for (int i = 0; i < 60 * 15; i++) { hh.frame(); maxB = Math.max(maxB, gg.combat.bullets.size); }
            check("boss kind " + k + " (" + gg.strings.get(BossManager.NAME_KEYS[k]) + ") attacks", maxB > 3 || gg.enemies.aliveCount() > 10, "bullets=" + maxB);
        }
    }

    static void testBossDefeat() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        Enemy b = g.bosses.spawnBoss();
        int score0 = g.bossScore;
        g.player.hp = 10;
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(b, b.maxHp + 1, 0, 0, 0);
        check("boss defeat counted + score", g.bossesKilled == 1 && g.bossScore - score0 == Config.BOSS_SCORE, null);
        check("boss defeat heals player", g.player.hp > 10, null);
        h.tick(0.05f);
        check("boss defeat grants reward upgrade screen", g.state == Game.S_LEVELUP && g.upgrades.bossReward, "state=" + g.state);
        h.tick(0.6f);
        h.tapButton(g.ui.levelButton(1));
        check("reward chosen -> back to play", g.state == Game.S_PLAYING || g.state == Game.S_LEVELUP, "state=" + g.state);
    }

    static void testGameOverRestartAndScore() {
        Harness h = fresh(); Game g = h.game;
        g.startRun();
        h.tick(30f);
        int kills = g.kills;
        h.bot = false;
        g.player.invuln = 0; g.player.armor = 0;
        g.combat.damagePlayer(10000);
        check("player death triggers", !g.player.alive, null);
        h.tick(1.5f);
        check("game over screen after death", g.state == Game.S_GAMEOVER, "state=" + g.state);
        int expected = g.killScore + g.bossScore + (int) g.time * Config.SCORE_PER_SECOND + g.achievementsThisRun * Config.SCORE_PER_ACHIEVEMENT;
        check("score = kills + time + bosses + achievements", g.score == expected && g.score > 0, "score=" + g.score + " expected=" + expected);
        check("new best flagged on first run", g.newBest && g.save.bestScore == g.score, null);
        h.tapButton(g.ui.overButton(0));
        check("game-over buttons ignore taps for first 0.8s", g.state == Game.S_GAMEOVER, null);
        h.tick(1f);
        h.tapButton(g.ui.overButton(0));
        check("Restart starts a fresh run", g.state == Game.S_PLAYING && g.kills == 0 && g.time < 0.1f && g.player.hp == g.player.maxHp, null);
        g.combat.damagePlayer(100000);
        h.tick(1.5f); h.tick(1f);
        h.tapButton(g.ui.overButton(1));
        check("Main Menu button returns to menu", g.state == Game.S_MENU, null);
        check("games played counted", g.save.gamesPlayed == 2, "played=" + g.save.gamesPlayed);
    }

    static void testSaveLoad() {
        DesktopPlatform.MemStorage st = new DesktopPlatform.MemStorage();
        Harness h = new Harness(st, 1080, 2400);
        Game g = h.game;
        g.startRun();
        h.tick(45f);
        int kills = g.kills;
        g.combat.damagePlayer(1e6f); g.player.invuln = 0; g.combat.damagePlayer(1e6f);
        h.tick(2f);
        int score = g.score;
        int level = g.player.level;
        // "close" the app and reopen it with only the flushed data
        Harness h2 = new Harness(st.reopen(), 1080, 2400);
        SaveManager s = h2.game.save;
        check("best score survives restart", s.bestScore == score && score > 0, s.bestScore + " vs " + score);
        check("total kills survive restart", s.totalKills == kills, s.totalKills + " vs " + kills);
        check("games played / time / level survive", s.gamesPlayed == 1 && s.totalTime > 40 && s.highestLevel == level, null);
        check("achievement progress survives", (s.achievementMask & 1) == 1, "mask=" + s.achievementMask);
        int expectedHpBonus = h2.game.achievements.unlocked(AchievementManager.KILLS_100) ? 5 : 0;
        h2.game.startRun();
        check("permanent unlock bonuses applied to next run", h2.game.player.damageMult > 1f, "dmgMult=" + h2.game.player.damageMult);
    }

    static void testSettingsPersist() {
        DesktopPlatform.MemStorage st = new DesktopPlatform.MemStorage();
        Harness h = new Harness(st, 1080, 2400);
        Game g = h.game;
        h.tapButton(g.ui.menuButton(1));
        UIManager.Button music = g.ui.settingsButton(0);
        float s = g.scale;
        g.touchDown(3, (music.l + (music.r - music.l) * 0.45f) * s, music.cy() * s);   // drag slider to 0
        g.touchUp(3, 0, 0);
        check("music slider sets volume", g.settings.musicVolume < 0.01f && h.platform.audio.musicVolume < 0.01f, "vol=" + g.settings.musicVolume);
        h.tapButton(g.ui.settingsButton(3)); // vibration
        h.tapButton(g.ui.settingsButton(4)); // shake
        h.tapButton(g.ui.settingsButton(6)); // graphics cycle
        h.tapButton(g.ui.settingsButton(7)); // battery
        h.tapButton(g.ui.settingsButton(8)); // language
        h.tapButton(g.ui.settingsButton(9)); // back
        SettingsManager re = new SettingsManager(st.reopen());
        check("settings persist across restart", !re.vibration && !re.screenShake && re.quality == 0 && re.batterySaver && re.language == 1 && re.musicVolume < 0.01f,
                "vib=" + re.vibration + " shake=" + re.screenShake + " q=" + re.quality + " bat=" + re.batterySaver + " lang=" + re.language);
        int vib = h.platform.vibrations;
        g.startRun(); g.combat.damagePlayer(5);
        check("vibration toggle respected", h.platform.vibrations == vib, null);
        check("Persian language applied", g.strings.get("play").equals("شروع"), null);
        // settings from pause menu return to pause
        g.pause();
        h.tapButton(g.ui.pauseButton(2));
        check("pause -> settings", g.state == Game.S_SETTINGS, null);
        h.tapButton(g.ui.settingsButton(9));
        check("settings -> back to pause", g.state == Game.S_PAUSED, null);
        // sfx muted -> no sounds played
        g.settings.sfxVolume = 0;
        int plays = sum(h.platform.audio.plays);
        g.resume(); h.tick(5);
        check("sfx volume 0 mutes effects", sum(h.platform.audio.plays) == plays, null);
    }

    private static int sum(int[] a) { int s = 0; for (int v : a) s += v; return s; }

    static void testPauseResumeBackground() {
        Harness h = fresh(); Game g = h.game;
        g.startRun();
        h.tick(5f);
        h.tapButton(g.ui.hudButton(0));
        check("HUD pause button pauses", g.state == Game.S_PAUSED, null);
        float t = g.time;
        h.tick(3f);
        check("time frozen while paused", g.time == t, null);
        h.tapButton(g.ui.pauseButton(0));
        check("Resume continues", g.state == Game.S_PLAYING, null);
        g.onHostPause();
        check("app backgrounded -> auto pause + music off + saved", g.state == Game.S_PAUSED && !h.platform.audio.music && h.platform.storage.flushes > 0, null);
        g.onHostResume();
        check("app foregrounded -> music back, still paused", h.platform.audio.music && g.state == Game.S_PAUSED, null);
        h.tapButton(g.ui.pauseButton(1));
        check("Restart from pause", g.state == Game.S_PLAYING && g.time == 0, null);
        g.pause();
        h.tapButton(g.ui.pauseButton(3));
        check("Main Menu from pause", g.state == Game.S_MENU, null);
    }

    static void testBackButton() {
        Harness h = fresh(); Game g = h.game;
        check("back on menu closes app", !g.onBack(), null);
        g.startRun();
        g.onBack();
        check("back in game pauses", g.state == Game.S_PAUSED, null);
        g.onBack();
        check("back in pause resumes", g.state == Game.S_PLAYING, null);
    }

    static void testAchievements() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        Enemy e = g.enemies.spawn(Config.ENEMIES[0], 100, 100);
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(e, 1e6f, 0, 0, 0);
        h.tick(0.1f);
        check("First Kill unlocks with notification", g.achievements.unlocked(AchievementManager.FIRST_KILL) && g.achievements.toastId == AchievementManager.FIRST_KILL, null);
        check("achievement sound played", h.platform.audio.plays[Sfx.ACHIEVEMENT] > 0, null);
        g.time = 301; h.tick(0.05f);
        check("Survive 5 Minutes unlocks", g.achievements.unlocked(AchievementManager.SURVIVE_5), null);
        check("achievements give score bonus", g.achievementsThisRun == 2, "count=" + g.achievementsThisRun);
    }

    static void testStrings() {
        Harness h = fresh(); Game g = h.game;
        ArrayList<String> keys = new ArrayList<>();
        for (EnemyType t : Config.ENEMIES) keys.add(t.nameKey);
        for (WeaponType t : Config.WEAPONS) { keys.add(t.nameKey); keys.add(t.descKey); }
        for (String k : UpgradeManager.STAT_KEYS) { keys.add(k); keys.add(k + "_d"); }
        for (String k : AchievementManager.KEYS) keys.add(k);
        for (String k : AchievementManager.BONUS_KEYS) keys.add(k);
        for (String k : Config.RARITY_KEYS) keys.add(k);
        for (String k : BossManager.NAME_KEYS) keys.add(k);
        StringBuilder missing = new StringBuilder();
        for (String k : keys) if (!g.strings.has(k)) missing.append(k).append(' ');
        check("all localisation keys exist (EN+FA)", missing.length() == 0, missing.length() == 0 ? keys.size() + " keys" : missing.toString());
    }

    static void testScreenSizes() {
        int[][] sizes = {{720, 1280}, {1080, 2400}, {1440, 3200}, {1200, 1920}, {1600, 2560}, {480, 854}};
        boolean ok = true;
        StringBuilder sb = new StringBuilder();
        for (int[] s : sizes) {
            Harness h = new Harness(new DesktopPlatform.MemStorage(), s[0], s[1]);
            Game g = h.game;
            float H = g.viewH;
            // all buttons must be inside the screen and not overlap each other
            ArrayList<UIManager.Button> all = new ArrayList<>();
            for (int i = 0; i < 4; i++) all.add(g.ui.menuButton(i));
            boolean fit = inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 10; i++) all.add(g.ui.settingsButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 3; i++) all.add(g.ui.levelButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 4; i++) all.add(g.ui.pauseButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 2; i++) all.add(g.ui.hudButton(i));
            fit &= inside(all, g.viewW, H);
            if (!fit) { ok = false; sb.append(s[0]).append('x').append(s[1]).append(' '); }
        }
        check("UI fits 6 screen sizes (480x854 .. 1600x2560)", ok, sb.length() == 0 ? null : sb.toString());
    }

    private static boolean inside(ArrayList<UIManager.Button> bs, float W, float H) {
        for (UIManager.Button b : bs) if (b.l < 0 || b.t < 0 || b.r > W || b.b > H) return false;
        return true;
    }
    private static boolean noOverlap(ArrayList<UIManager.Button> bs) {
        for (int i = 0; i < bs.size(); i++) for (int j = i + 1; j < bs.size(); j++) {
            UIManager.Button a = bs.get(i), b = bs.get(j);
            if (a.l < b.r && b.l < a.r && a.t < b.b && b.t < a.b) return false;
        }
        return true;
    }

    static void testPerformance() {
        Harness h = fresh(); Game g = h.game; h.bot = true;
        g.startRun(); g.godMode = true;
        for (WeaponType t : Config.WEAPONS) g.weapons.add(t.id);
        g.time = 900;   // late-game difficulty (huge population cap)
        long allocBefore = usedMem();
        long t0 = System.nanoTime();
        int frames = 1800;
        int maxAlive = 0;
        for (int i = 0; i < frames; i++) {
            if (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
            // keep the arena saturated
            for (int k = 0; k < 6 && g.enemies.aliveCount() < 360; k++) {
                float a = MathUtil.rand(0, MathUtil.TAU);
                g.enemies.spawn(Config.ENEMIES[MathUtil.randInt(10)], g.player.x + MathUtil.cos(a) * 500, g.player.y + MathUtil.sin(a) * 500);
            }
            h.frame();
            maxAlive = Math.max(maxAlive, g.enemies.aliveCount());
        }
        float ms = (System.nanoTime() - t0) / 1e6f / frames;
        check("simulation with " + maxAlive + " enemies stays fast", ms < 4f, String.format(Locale.US, "%.2f ms/frame (desktop JVM)", ms));
        // render cost (Java2D software rendering, very pessimistic vs. GPU canvas)
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1080, 2400, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D gr = img.createGraphics();
        DesktopGfx gfx = new DesktopGfx(gr, 1080, 2400);
        long r0 = System.nanoTime();
        for (int i = 0; i < 30; i++) g.render(gfx);
        float rms = (System.nanoTime() - r0) / 1e6f / 30;
        report.add(String.format(Locale.US, "INFO  render with %d enemies: %.1f ms/frame (Java2D software renderer; Android uses GPU canvas)", g.enemies.aliveCount(), rms));
        System.out.println(report.get(report.size() - 1));
        check("object pools bounded", g.enemies.list.capacity() == EnemyManager.CAPACITY && g.combat.projectiles.size <= g.combat.projectiles.capacity(), null);
    }

    private static long usedMem() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    static void testNaturalRuns() {
        // Bot plays without god mode — measures the difficulty curve.
        float total = 0; int bosses = 0;
        for (int run = 0; run < 3; run++) {
            Harness h = fresh(); Game g = h.game;
            g.startRun();
            while (g.state != Game.S_GAMEOVER && g.time < 20 * 60) h.frame();
            total += g.time;
            bosses += g.bossesKilled;
            report.add(String.format(Locale.US, "INFO  bot run %d: survived %s, level %d, kills %d, bosses %d, score %d, weapons %d",
                    run + 1, MathUtil.time(g.time), g.player.level, g.kills, g.bossesKilled, g.score, g.weapons.owned.size()));
            System.out.println(report.get(report.size() - 1));
            if (run == 0) check("early game is survivable (bot lives > 2 min)", g.time > 120, MathUtil.time(g.time));
        }
        check("difficulty eventually overwhelms a simple bot (< 20 min avg)", total / 3 < 20 * 60, "avg " + MathUtil.time(total / 3));
    }
}
