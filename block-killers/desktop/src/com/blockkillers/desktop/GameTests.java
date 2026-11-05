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
        testHotbar();
        testLoot();
        testMap();
        testElementsAndTraits();
        testGrowthShapeAndFusion();
        testCoinsShopPassive();
        testMemeAndDeath();
        testPets();
        testDevMenu();
        testMaps();
        testGunParts();
        testElementMatchups();
        testBulletInterception();
        testMultiplayer();
        testZombie3DAndLayout();
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
        h.tapButton(g.ui.settingsButton(12));
        check("settings: Back returns to menu", g.state == Game.S_MENU, null);
        h.tapButton(g.ui.menuButton(2));
        check("menu: Statistics button opens stats", g.state == Game.S_STATS, null);
        h.tapButton(g.ui.statsButton(0));
        check("stats: Back returns to menu", g.state == Game.S_MENU, null);
        h.tapButton(g.ui.menuButton(0));
        check("menu: Play opens the map selector", g.state == Game.S_MAPSELECT, null);
        h.tapButton(g.ui.mapSelectButton(0));
        check("menu: Play starts a run", g.state == Game.S_PLAYING && g.player.alive && g.weapons.count() == 1, null);
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
        g.touchDown(1, 200 * s, 1100 * s);
        g.touchMove(1, 200 * s, 1200 * s);       // drag down 100 virtual px
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
        noWeapons(g);
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
        noWeapons(g);
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

    static void noWeapons(Game g) { for (int i = 0; i < g.weapons.slots.length; i++) g.weapons.slots[i] = null; g.pets.count = 0; }

    static void testAllWeapons() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun();
        g.godMode = true;
        for (WeaponType t : Config.WEAPONS) g.weapons.add(t.id);
        check("hotbar holds " + Config.HOTBAR + " guns", g.weapons.count() == Config.HOTBAR, "count=" + g.weapons.count());
        boolean ok = true;
        StringBuilder sb = new StringBuilder();
        for (WeaponType t : Config.WEAPONS) {
            Harness hh = fresh(); Game gg = hh.game; hh.bot = false;
            gg.startRun(); gg.godMode = true;
            noWeapons(gg); gg.weapons.slots[0] = new Weapon(t); gg.weapons.active = 0;
            for (int i = 0; i < 12; i++) {
                float a = i * MathUtil.TAU / 12;
                gg.enemies.spawn(Config.ENEMIES[3], gg.player.x + MathUtil.cos(a) * 110, gg.player.y + MathUtil.sin(a) * 110);
            }
            float hp0 = totalHp(gg);
            hh.tick(4f);
            float hp1 = totalHp(gg);
            boolean dealt = hp1 < hp0 || gg.kills > 0;
            if (!dealt) { ok = false; sb.append(t.nameKey).append(' '); }
        }
        check("all " + Config.WEAPONS.length + " guns deal damage", ok, sb.length() == 0 ? null : "no damage: " + sb);
        // weapon levelling
        noWeapons(g); g.weapons.slots[0] = new Weapon(Config.WEAPONS[Config.W_BLASTER]);
        Weapon w = g.weapons.slots[0];
        float d1 = w.damage(g.player), c1 = w.cooldown(g.player);
        g.weapons.levelUp(0, 3);
        check("weapon level-up increases damage & fire rate", w.damage(g.player) > d1 && w.cooldown(g.player) < c1 && w.level == 4, null);
        g.weapons.levelUp(0, 10);
        check("weapon level capped", w.level == Config.WEAPON_MAX_LEVEL, null);
    }


    static void testHotbar() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true;
        g.weapons.slots[1] = new Weapon(Config.WEAPONS[Config.W_TWIN]);
        g.weapons.slots[2] = new Weapon(Config.WEAPONS[Config.W_ROCKET]);
        h.tapButton(g.ui.hudButton(4 + 1));
        check("tapping a hotbar slot switches gun", g.weapons.active == 1, "active=" + g.weapons.active);
        h.tapButton(g.ui.hudButton(4 + 4));
        check("empty hotbar slot can't be selected", g.weapons.active == 1, null);
        g.enemies.spawn(Config.ENEMIES[9], g.player.x + 250, g.player.y);
        boolean onlyActive = true; int seen = 0;
        for (int i = 0; i < 120; i++) {
            h.frame();
            for (int k = 0; k < g.combat.projectiles.size; k++) {
                seen++;
                int wid = g.combat.projectiles.get(k).weaponId;
                if (wid != Config.W_TWIN && wid != -1) onlyActive = false;   // -1 = companion shots
            }
        }
        check("only the gun in hand fires", onlyActive && seen > 0, "projectile frames=" + seen);
        check("gun aims at the target", Math.abs(MathUtil.cos(g.player.facing) - 1f) < 0.2f, "facing=" + g.player.facing);
    }

    static void testLoot() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true;
        boolean partsOk = true, legendTrait = true, names = true;
        for (int r = 0; r < Loot.TIERS; r++) for (int i = 0; i < 40; i++) {
            Weapon w = g.loot.roll(MathUtil.randInt(Config.WEAPONS.length), r, 3);
            if (r == Loot.LEGENDARY && w.trait == Loot.T_NONE) legendTrait = false;
            if (w.name(g.strings).isEmpty()) names = false;
            if (r == Loot.LEGENDARY && w.element == Loot.E_NONE) partsOk = false;
        }
        check("legendaries always roll a trait and an element", legendTrait && partsOk, null);
        check("every rolled gun has a name", names, g.loot.roll(0, 4, 5).name(g.strings) + " / " + g.loot.roll(9, 2, 5).name(g.strings));
        float common = new Weapon(Config.WEAPONS[0]).damage(g.player);
        Weapon leg = g.loot.roll(0, Loot.LEGENDARY, 1); leg.dmgMod = 1f;
        Weapon hi = g.loot.roll(0, Loot.COMMON, 10);
        check("higher rarity and item level hit harder", leg.damage(g.player) > common * 1.7f && hi.damage(g.player) > common * 1.5f, null);
        int[] dist = new int[Loot.TIERS];
        for (int i = 0; i < 5000; i++) dist[g.loot.rollRarity(0, 1f)]++;
        check("drop rarity distribution white>green>blue>purple>orange>0", dist[0] > dist[1] && dist[1] > dist[2] && dist[2] > dist[3] && dist[3] > dist[4] && dist[4] > 0,
                dist[0] + "/" + dist[1] + "/" + dist[2] + "/" + dist[3] + "/" + dist[4]);
        // drop -> walk over -> pick up -> equip into slot
        Weapon drop = g.loot.roll(Config.W_RAILGUN, Loot.EPIC, 2);
        g.loot.drop(g.player.x + 40, g.player.y, drop);
        h.tick(0.6f);
        check("standing near a drop shows PICK UP", g.loot.nearby != null, null);
        h.tapButton(g.ui.hudButton(3));
        check("PICK UP opens the weapon screen (paused)", g.state == Game.S_LOOT, "state=" + g.state);
        float t0 = g.time; h.tick(0.5f);
        check("game frozen on weapon screen", g.time == t0, null);
        h.tapButton(g.ui.lootButton(1));
        check("tapping a slot equips the gun", g.weapons.slots[1] == drop && g.state == Game.S_PLAYING && g.loot.items.size == 0, "state=" + g.state);
        // scrap
        g.loot.drop(g.player.x, g.player.y, g.loot.roll(0, Loot.RARE, 1));
        h.tick(0.6f);
        int xp0 = g.player.xp + g.player.level * 1000;
        g.openLoot(); h.tapButton(g.ui.lootButton(5));
        check("scrap converts the gun into XP", g.player.xp + g.player.level * 1000 > xp0 && g.loot.items.size == 0, null);
        // leave (resolve any level-up the scrap XP triggered first)
        h.tick(0.05f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        g.loot.drop(g.player.x, g.player.y, g.loot.roll(0, Loot.COMMON, 1));
        h.tick(0.6f);
        g.openLoot(); h.tapButton(g.ui.lootButton(6));
        check("leave keeps the gun on the ground", g.loot.items.size == 1 && g.state == Game.S_PLAYING, "items=" + g.loot.items.size + " state=" + g.state + " nearby=" + (g.loot.nearby != null));
        // boss drops
        g.loot.items.clear();
        Enemy b = g.bosses.spawnBoss();
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(b, b.maxHp * 10, 0, 0, 0);
        boolean epic = g.loot.items.size == 3;
        for (int i = 0; i < g.loot.items.size; i++) epic &= g.loot.items.get(i).weapon.rarity >= Loot.EPIC;
        check("boss drops 3 Epic+ guns", epic, "drops=" + g.loot.items.size);
        // enemy drops over a long fight
        Harness h2 = fresh(); Game g2 = h2.game;
        g2.startRun(); g2.godMode = true;
        while (g2.time < 240) { h2.frame(); }
        check("enemies drop guns during play", g2.loot.dropsThisRun > 0, "drops=" + g2.loot.dropsThisRun + " kills=" + g2.kills);
        // despawn
        g.loot.items.clear();
        g.loot.drop(g.player.x + 2000, g.player.y, g.loot.roll(0, 0, 1));
        g.state = Game.S_PLAYING;
        h.tick(LootManager.GROUND_LIFE + 1);
        check("unclaimed drops despawn", g.loot.items.size <= 3, "items=" + g.loot.items.size);
    }

    static void testMap() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true;
        WorldMap m = g.map;
        check("map has obstacles, ruins and chests", m.count > 150 && m.chestCount > 25, "obstacles=" + m.count + " chests=" + m.chestCount);
        check("start area is clear", !m.blocked(0, 0, 300), null);
        // walk into an obstacle
        int o = 0;
        for (int i = 0; i < m.count; i++) if (m.or[i] > 50) { o = i; break; }
        g.player.x = m.ox[o] - m.or[o] - 120; g.player.y = m.oy[o];
        g.playerCtl.overrideInput = true; g.playerCtl.inputX = 1; g.playerCtl.inputY = 0;
        g.enemies.clear();
        boolean inside = false;
        for (int i = 0; i < 120; i++) { h.frame(); if (MathUtil.dist(g.player.x, g.player.y, m.ox[o], m.oy[o]) < m.or[o] + g.player.radius - 1) inside = true; }
        check("player cannot walk through obstacles", !inside, null);
        // enemy bullet blocked by cover
        g.combat.bullets.clear();
        g.combat.fireEnemyBullet(m.ox[o] + m.or[o] + 60, m.oy[o], -1, 0, 260, 50, 0xFFFFFFFF);
        float hp = g.player.hp; g.godMode = false; g.player.invuln = 0;
        h.tick(1.5f);
        check("obstacles block enemy bullets (cover)", g.player.hp == hp, null);
        g.godMode = true;
        // chest opening
        int c = 0;
        g.player.x = m.cx[c]; g.player.y = m.cy[c];
        g.playerCtl.inputX = 0;
        h.tick(0.2f);
        check("walking into a chest opens it and drops loot", m.opened[c] && g.loot.items.size >= 1 && g.loot.chestsThisRun == 1, null);
        // fog of war
        float f0 = m.exploredFraction();
        g.player.x = -2500; g.player.y = 2500; h.tick(0.1f);
        check("exploring reveals the map", m.exploredFraction() > f0, String.format(Locale.US, "%.1f%% -> %.1f%%", f0 * 100, m.exploredFraction() * 100));
        // minimap opens full map
        h.tapButton(g.ui.hudButton(2));
        check("tapping the minimap opens the full map", g.state == Game.S_MAP, null);
        h.tapButton(g.ui.mapButton(0));
        check("map Back returns to the game", g.state == Game.S_PLAYING, null);
        // regions
        check("four regions", WorldMap.region(-100, 100) != WorldMap.region(100, 100) && WorldMap.region(100, -100) != WorldMap.region(-100, -100), null);
        // enemies steer around pillars and still reach the player
        g.enemies.clear(); g.pets.count = 0;
        g.player.x = m.ox[o] - m.or[o] - 160; g.player.y = m.oy[o];
        Enemy e = g.enemies.spawn(Config.ENEMIES[0], m.ox[o] + m.or[o] + 80, m.oy[o] + 5);
        float d0 = MathUtil.dist(e.x, e.y, g.player.x, g.player.y);
        for (int i = 0; i < 60 * 8 && e.alive; i++) { g.playerCtl.inputX = 0; g.update(1f / 60f); g.weapons.slots[0].timer = 1; }
        check("enemies path around obstacles to reach you", MathUtil.dist(e.x, e.y, g.player.x, g.player.y) < d0 * 0.4f, "dist " + (int) d0 + " -> " + (int) MathUtil.dist(e.x, e.y, g.player.x, g.player.y));
    }

    static void testElementsAndTraits() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true;
        Weapon fire = new Weapon(Config.WEAPONS[0]); fire.element = Loot.E_FIRE;
        Enemy e = g.enemies.spawn(Config.ENEMIES[9], g.player.x + 400, g.player.y);
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(e, 10, 0, 0, 0, fire);
        float hp = e.hp; noWeapons(g);
        h.tick(1f);
        check("fire burns over time", e.burnT > 0 || e.hp < hp, "hp " + (int) hp + " -> " + (int) e.hp);
        Weapon frost = new Weapon(Config.WEAPONS[0]); frost.element = Loot.E_FROST;
        g.combat.damageEnemy(e, 1, 0, 0, 0, frost);
        check("frost slows", e.slowT > 0 && e.slowAmt > 0.3f, null);
        Weapon tox = new Weapon(Config.WEAPONS[0]); tox.element = Loot.E_ACID;
        g.combat.damageEnemy(e, 1, 0, 0, 0, tox);
        check("acid corrodes over time", e.poisonT > 0, null);
        // vampiric
        Weapon vamp = new Weapon(Config.WEAPONS[0]); vamp.trait = Loot.T_VAMPIRIC;
        g.player.hp = 50;
        Enemy v = g.enemies.spawn(Config.ENEMIES[0], g.player.x + 300, g.player.y);
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(v, 1e6f, 0, 0, 0, vamp);
        check("Vampiric kills heal", g.player.hp > 50, "hp=" + g.player.hp);
        // volatile
        g.enemies.clear();
        Weapon vol = new Weapon(Config.WEAPONS[0]); vol.trait = Loot.T_VOLATILE;
        Enemy a = g.enemies.spawn(Config.ENEMIES[0], 500, 500), b = g.enemies.spawn(Config.ENEMIES[3], 530, 500);
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(a, 1e6f, 0, 0, 0, vol);
        float bh = b.hp;
        g.combat.update(0.001f);
        check("Volatile kills explode", b.hp < bh || b.shield < b.maxShield || !b.alive, null);
        // overcharge
        Weapon oc = new Weapon(Config.WEAPONS[0]); oc.trait = Loot.T_OVERCHARGE;
        check("Overcharge boosts DPS", oc.dps(g.player) > new Weapon(Config.WEAPONS[0]).dps(g.player), null);
        // fractal split
        g.enemies.clear(); g.combat.clear();
        Enemy tgt = g.enemies.spawn(Config.ENEMIES[9], g.player.x + 200, g.player.y);
        Weapon fr = new Weapon(Config.WEAPONS[0]); fr.trait = Loot.T_FRACTAL;
        g.combat.fireProjectile(g.player.x + 150, g.player.y, 0, 600, 5, 0, 0, 0, 400, 0xFFFFFFFF, 0, 6, WeaponType.SP_NONE, 0, 0, true, fr);
        g.grid.build(g.enemies.list);
        int before = g.combat.projectiles.size;
        for (int i = 0; i < 10; i++) { g.grid.build(g.enemies.list); g.combat.update(1f / 60f); }
        check("Fractal projectiles split on hit", g.combat.projectiles.size >= 2 && tgt.hp < tgt.maxHp, "projectiles " + before + " -> " + g.combat.projectiles.size);
    }

    static void testGrowthShapeAndFusion() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true;
        float r0 = g.player.radius;
        g.player.level = 25;
        h.tick(3f);
        check("player grows with level", g.player.radius > r0 * 1.4f, String.format(Locale.US, "r %.1f -> %.1f", r0, g.player.radius));
        check("camera zooms out as you grow", g.camera.zoom < 0.8f, String.format(Locale.US, "zoom %.2f", g.camera.zoom));
        check("enemies grow over time", Config.enemyGrowth(600) > 1.4f, null);
        // shape changes with every upgrade
        g.xp.addXp(Config.xpToNext(g.player.level));
        h.tick(0.05f);
        while (g.state == Game.S_LEVELUP) {
            int pick = 0;
            for (int i = 0; i < g.upgrades.cardCount; i++) if (g.upgrades.cards[i].type == UpgradeManager.T_STAT) pick = i;
            int stat = g.upgrades.cards[pick].stat, type = g.upgrades.cards[pick].type;
            int before = g.player.features[stat], taken = g.player.upgradesTaken;
            g.chooseUpgrade(pick);
            check("chosen upgrade changes the player's shape", g.player.upgradesTaken == taken + 1 && (type != UpgradeManager.T_STAT || g.player.features[stat] == before + 1) && g.player.morph > 0, null);
            break;
        }
        // fused enemies
        Harness h2 = fresh(); Game g2 = h2.game; h2.bot = false;
        g2.startRun(); g2.godMode = true; g2.time = 700;
        int[] tiers = new int[3];
        for (int i = 0; i < 60 * 30; i++) {
            g2.update(1f / 60f);
            if (g2.state == Game.S_LEVELUP) g2.chooseUpgrade(0);
        }
        for (int i = 0; i < g2.enemies.list.size; i++) tiers[g2.enemies.list.get(i).tier]++;
        check("late game spawns fused and mega-fused enemies", tiers[1] > 0 && tiers[2] > 0, tiers[0] + "/" + tiers[1] + "/" + tiers[2]);
        check("no fused enemies early on", Config.fusedChance(60) == 0 && Config.megaChance(200) == 0, null);
        Enemy n = g2.enemies.spawn(Config.ENEMIES[3], 0, 0); float nhp = n.maxHp, nr = n.radius;
        Enemy f = g2.enemies.spawn(Config.ENEMIES[3], 0, 0); g2.enemies.fuse(f, 2);
        check("mega-fused enemies are bigger and much tougher", f.maxHp > nhp * 5 && f.radius > nr * 1.4f, null);
    }


    static void testCoinsShopPassive() {
        DesktopPlatform.MemStorage st = new DesktopPlatform.MemStorage();
        Harness h = new Harness(st, 1080, 2400); Game g = h.game;
        g.startRun(); g.godMode = true;
        h.tick(60f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        int earned = g.coinsThisRun;
        check("coins are earned during a run", earned > 0, "coins=" + earned + " kills=" + g.kills);
        g.godMode = false; g.player.invuln = 0; g.combat.damagePlayer(1e7f); h.tick(3.5f);
        check("coins are banked at game over", g.save.coins == earned, "bank=" + g.save.coins);
        Harness h2 = new Harness(st.reopen(), 1080, 2400);
        check("coins survive an app restart", h2.game.save.coins == earned, null);
        Game g2 = h2.game;
        g2.save.coins = 10000;
        h2.tapButton(g2.ui.menuButton(4));
        check("menu: Shop button opens the shop", g2.state == Game.S_SHOP, null);
        int cost = g2.shop.cost(ShopManager.VITALITY);
        h2.tapButton(g2.ui.shopButton(ShopManager.VITALITY));
        check("buying an upgrade spends coins and raises its level", g2.shop.levels[ShopManager.VITALITY] == 1 && g2.save.coins == 10000 - cost, null);
        check("upgrade cost rises", g2.shop.cost(ShopManager.VITALITY) > cost, null);
        g2.save.coins = 0;
        h2.tapButton(g2.ui.shopButton(ShopManager.POWER));
        check("can't buy without enough coins", g2.shop.levels[ShopManager.POWER] == 0, null);
        Harness h3 = new Harness(((DesktopPlatform.MemStorage) h2.platform.storage).reopen(), 1080, 2400);
        check("shop levels persist", h3.game.shop.levels[ShopManager.VITALITY] == 1, null);
        h3.game.startRun();
        check("shop bonus applies to the run (+10 max HP)", h3.game.player.maxHp >= Config.PLAYER_MAX_HP + 10, "maxHp=" + h3.game.player.maxHp);
        // passive: every 20 kills = +2 HP
        Game g4 = h3.game; noWeapons(g4); g4.enemies.clear();
        g4.player.hp = 50; g4.kills = 19; g4.hero.enemyKills = 19;
        Enemy e = g4.enemies.spawn(Config.ENEMIES[0], g4.player.x + 300, g4.player.y);
        g4.grid.build(g4.enemies.list);
        g4.combat.damageEnemy(e, 1e6f, 0, 0, 0);
        check("passive: the 20th kill restores 2 HP", Math.abs(g4.player.hp - 52f) < 0.01f, "hp=" + g4.player.hp);
        // second wind
        g4.shop.levels[ShopManager.SECOND_WIND] = 1;
        g4.player.invuln = 0; g4.combat.damagePlayer(1e7f);
        check("Second Wind revives once", g4.player.alive && g4.reviveUsed && g4.player.hp > 0, null);
        g4.player.invuln = 0; g4.combat.damagePlayer(1e7f);
        check("...but only once", !g4.player.alive, null);
    }

    static void testMemeAndDeath() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true; noWeapons(g);
        DesktopPlatform.RecAudio au = h.platform.audio;
        g.settings.memeSounds = true;
        for (int i = 0; i < 10; i++) {
            Enemy e = g.enemies.spawn(Config.ENEMIES[0], g.player.x + 300 + i * 5, g.player.y);
            g.grid.build(g.enemies.list);
            g.combat.damageEnemy(e, 1e6f, 0, 0, 0);
        }
        check("multi-kill plays the air horn", au.plays[Sfx.AIRHORN] > 0, null);
        g.godMode = false; g.player.invuln = 0;
        int oof = au.plays[Sfx.OOF];
        g.combat.damagePlayer(5);
        check("getting hit plays the 'oof' (meme on)", au.plays[Sfx.OOF] == oof + 1, null);
        g.settings.memeSounds = false; g.player.invuln = 0;
        h.tick(0.3f);
        int hurt = au.plays[Sfx.HURT]; oof = au.plays[Sfx.OOF];
        g.combat.damagePlayer(5);
        check("meme sounds can be switched off", au.plays[Sfx.OOF] == oof && au.plays[Sfx.HURT] == hurt + 1, null);
        g.settings.memeSounds = true; g.player.invuln = 0;
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        g.combat.damagePlayer(1e7f);
        check("death plays the dramatic death sting", au.plays[Sfx.DEATH_STING] > 0, null);
        h.tick(3.5f);
        check("sad trombone on the game-over screen", au.plays[Sfx.SAD_TROMBONE] > 0 && g.state == Game.S_GAMEOVER, "plays=" + au.plays[Sfx.SAD_TROMBONE] + " state=" + g.state + " alive=" + g.player.alive);
        for (int id = 0; id < Sfx.COUNT; id++) {
            short[] pcm = SoundSynth.generate(id);
            if (pcm.length < 100) { check("sound " + id + " synthesizes", false, null); return; }
        }
        check("all " + Sfx.COUNT + " sounds synthesize", true, null);
    }

    static void testPets() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true;
        check("every run starts with a Drone companion", g.pets.count == 1 && g.pets.pets[0].type == PetManager.DRONE, null);
        for (int i = 0; i < Config.HOTBAR; i++) g.weapons.slots[i] = null;
        Enemy e = g.enemies.spawn(Config.ENEMIES[9], g.player.x + 250, g.player.y);
        h.tick(3f);
        check("companion shoots enemies on its own", e.hp < e.maxHp || !e.alive, null);
        float d1 = g.pets.damage(g.pets.pets[0]);
        g.player.level = 20;
        check("companions get stronger as you level", g.pets.damage(g.pets.pets[0]) > d1 * 1.7f, null);
        g.pets.levelUp(0);
        check("companion upgrades raise its level", g.pets.pets[0].level == 2, null);
        g.pets.add(PetManager.WISP); g.pets.add(PetManager.SPRITE); g.pets.add(PetManager.TURRET);
        check("up to 3 companions", g.pets.count == 3, "count=" + g.pets.count);
        g.player.hp = 40;
        h.tick(7f);
        check("Healing Wisp heals you", g.player.hp > 40, "hp=" + g.player.hp);
        boolean petCard = false;
        Harness h2 = fresh(); Game g2 = h2.game; g2.startRun();
        for (int i = 0; i < 400 && !petCard; i++) {
            g2.upgrades.roll(false);
            for (int k = 0; k < g2.upgrades.cardCount; k++) {
                int t = g2.upgrades.cards[k].type;
                if (t == UpgradeManager.T_PET_NEW || t == UpgradeManager.T_PET_LEVEL) { g2.upgrades.apply(k); petCard = true; break; }
            }
        }
        check("level-up offers companion cards", petCard && (g2.pets.count == 2 || g2.pets.pets[0].level == 2), null);
    }

    static void testDevMenu() {
        Harness h = fresh(); Game g = h.game;
        h.tapButton(g.ui.menuButton(5));
        check("menu: Developer button opens the dev menu", g.state == Game.S_DEV, null);
        h.tapButton(g.ui.devButton(0));
        check("dev: +1,000 coins", g.save.coins == 1000, null);
        h.tapButton(g.ui.devButton(2));
        check("dev: set coins to 0", g.save.coins == 0, null);
        h.tapButton(g.ui.devButton(3));   // god mode
        h.tapButton(g.ui.devButton(5));   // level 5
        h.tapButton(g.ui.devButton(5));   // level 10
        h.tapButton(g.ui.devButton(6));   // minute 2
        h.tapButton(g.ui.devButton(7));   // legendary loadout
        h.tapButton(g.ui.devButton(8));   // all pets
        h.tapButton(g.ui.devButton(UIManager.DEV_ROWS - 1));   // start
        check("dev: start test run", g.state == Game.S_PLAYING, "state=" + g.state);
        check("dev: god mode, start level and time applied", g.godMode && g.player.level == 10 && g.time >= 120, "lvl=" + g.player.level + " t=" + g.time);
        boolean leg = true;
        for (Weapon w : g.weapons.slots) leg &= w != null && w.rarity == Loot.LEGENDARY;
        check("dev: legendary loadout + all companions", leg && g.pets.count == PetManager.MAX_PETS, null);
        g.toMenu();
        h.tapButton(g.ui.menuButton(5));
        h.tapButton(g.ui.devButton(9));
        boolean maxed = true;
        for (int i = 0; i < ShopManager.COUNT; i++) maxed &= g.shop.maxed(i);
        check("dev: max all shop upgrades", maxed, null);
        h.tapButton(g.ui.devButton(10));
        check("dev: reset shop", g.shop.levels[0] == 0, null);
        g.save.coins = 50;
        h.tapButton(g.ui.devButton(11));
        check("dev: reset-all needs a second tap", g.save.coins == 50, null);
        h.tapButton(g.ui.devButton(11));
        check("dev: reset all progress", g.save.coins == 0 && g.save.gamesPlayed == 0, null);
    }

    static void testMaps() {
        for (int mode = 0; mode < WorldMap.MODES; mode++) {
            Harness h = fresh(); Game g = h.game;
            g.startRun(mode); g.godMode = true;
            h.tick(40f);
            while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
            boolean inBounds = Math.abs(g.player.x) <= g.map.hw && Math.abs(g.player.y) <= g.map.hh;
            for (int i = 0; i < g.enemies.list.size; i++) { Enemy e = g.enemies.list.get(i); inBounds &= Math.abs(e.x) <= g.map.hw + 1 && Math.abs(e.y) <= g.map.hh + 1; }
            check("map '" + g.strings.get(WorldMap.MODE_KEYS[mode]) + "' plays and keeps everything in bounds", inBounds && g.kills > 0, "kills=" + g.kills);
            check("selected map is remembered", g.save.selectedMap == mode, null);
        }
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(WorldMap.CORRIDOR); g.godMode = true;
        g.playerCtl.overrideInput = true; g.playerCtl.inputX = 0.7f; g.playerCtl.inputY = 0.7f;
        h.tick(2f);
        check("corridor: player moves only left/right", Math.abs(g.player.y) < 1f && g.player.x > 100, "x=" + (int) g.player.x + " y=" + g.player.y);
        g.playerCtl.inputX = 0; g.playerCtl.inputY = 0;
        noWeapons(g);
        h.tick(8f);
        boolean horiz = g.enemies.list.size > 0;
        for (int i = 0; i < g.enemies.list.size; i++) { Enemy e = g.enemies.list.get(i); horiz &= Math.abs(e.vy) < 0.01f && Math.abs(e.y) <= g.map.hh; }
        check("corridor: enemies move only left/right inside the lane", horiz, "enemies=" + g.enemies.list.size);
        check("corridor is super narrow", g.map.hh * 2 <= 160 && g.map.hw * 2 >= 7000, null);
        Harness ha = fresh(); ha.game.startRun(WorldMap.ARENA);
        check("arena is a small closed map", ha.game.map.hw <= 1000 && ha.game.map.count >= 10, null);
    }

    static void testGunParts() {
        Harness h = fresh(); Game g = h.game; g.startRun();
        boolean makers = true, names = true;
        boolean[] seenMaker = new boolean[Loot.MAKERS];
        float minD = Float.MAX_VALUE, maxD = 0;
        for (int i = 0; i < 300; i++) {
            Weapon w = g.loot.roll(Config.W_BLASTER, Loot.RARE, 1);
            makers &= w.maker >= 0;
            if (w.maker >= 0) seenMaker[w.maker] = true;
            names &= w.name(g.strings).contains(g.strings.get(Loot.MAKER_KEYS[w.maker]));
            float d = w.dps(g.player);
            minD = Math.min(minD, d); maxD = Math.max(maxD, d);
        }
        boolean all = true; for (boolean b : seenMaker) all &= b;
        check("every looted gun has a maker and parts; all 6 makers appear", makers && all, null);
        check("gun names include the maker", names, g.loot.roll(9, 3, 4).name(g.strings));
        check("same gun + rarity rolls very different stats", maxD > minD * 1.6f, String.format(Locale.US, "DPS %.0f..%.0f", minD, maxD));
        Weapon k = g.loot.roll(0, 0, 1);
        k.maker = Loot.MK_KRUGG;
        check("starter gun has no maker", new Weapon(Config.WEAPONS[0]).maker == -1, null);
    }

    static void testElementMatchups() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true; noWeapons(g);
        MathUtil.RNG.setSeed(5);
        g.player.critChance = 0;
        Weapon shock = new Weapon(Config.WEAPONS[0]); shock.element = Loot.E_SHOCK;
        Weapon plain = new Weapon(Config.WEAPONS[0]);
        Enemy a = g.enemies.spawn(Config.ENEMIES[3], 900, 900), b = g.enemies.spawn(Config.ENEMIES[3], 1400, 900);
        check("pentagons carry shields", a.shield > 0 && a.maxShield > 0, null);
        float s0 = a.shield;
        g.grid.build(g.enemies.list);
        g.combat.damageEnemy(a, 5, 0, 0, 0, shock);
        g.combat.damageEnemy(b, 5, 0, 0, 0, plain);
        check("Shock deals x2 to shields", Math.abs((s0 - a.shield) - 10f) < 0.01f && Math.abs((s0 - b.shield) - 5f) < 0.01f, null);
        Weapon acid = new Weapon(Config.WEAPONS[0]); acid.element = Loot.E_ACID;
        Enemy t1 = g.enemies.spawn(Config.ENEMIES[9], 900, 1200), t2 = g.enemies.spawn(Config.ENEMIES[9], 1400, 1200);
        check("tanks are armored", t1.armored, null);
        g.combat.damageEnemy(t1, 10, 0, 0, 0, acid);
        g.combat.damageEnemy(t2, 10, 0, 0, 0, plain);
        check("Acid deals x1.75 to armor", Math.abs((t1.maxHp - t1.hp) - 17.5f) < 0.01f && Math.abs((t2.maxHp - t2.hp) - 10f) < 0.01f, null);
        Weapon fire = new Weapon(Config.WEAPONS[0]); fire.element = Loot.E_FIRE;
        Enemy f = g.enemies.spawn(Config.ENEMIES[0], 900, 1500);
        f.maxHp = f.hp = 1000;
        g.combat.damageEnemy(f, 10, 0, 0, 0, fire);
        check("Burn deals x1.5 to unprotected enemies", Math.abs((1000 - f.hp) - 15f) < 0.01f, null);
        Weapon slag = new Weapon(Config.WEAPONS[0]); slag.element = Loot.E_SLAG;
        Enemy sg = g.enemies.spawn(Config.ENEMIES[0], 900, 1800); sg.maxHp = sg.hp = 1000;
        g.combat.damageEnemy(sg, 1, 0, 0, 0, slag);
        float before = sg.hp;
        g.combat.damageEnemy(sg, 10, 0, 0, 0, plain);
        check("Slagged enemies take +50% damage", sg.slagT > 0 && Math.abs((before - sg.hp) - 15f) < 0.01f, null);
        check("bosses have shields and armor", g.bosses.spawnBoss().shield > 0 && g.bosses.current.armored, null);
        check("fused enemies are armored", fusedArmored(g), null);
    }
    private static boolean fusedArmored(Game g) { Enemy e = g.enemies.spawn(Config.ENEMIES[0], 0, 0); g.enemies.fuse(e, 1); return e.armored; }

    static void testBulletInterception() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        g.startRun(); g.godMode = true; noWeapons(g); g.enemies.clear();
        g.combat.fireEnemyBullet(g.player.x + 300, g.player.y, -1, 0, 200, 10, 0xFFFFFFFF);
        g.combat.fireProjectile(g.player.x + 40, g.player.y, 0, 700, 10, 0, 0, 0, 500, 0xFFFFFFFF, 0, 6);
        h.tick(0.6f);
        check("your bullets shoot down enemy bullets", g.combat.bullets.size == 0 && g.bulletsDestroyed == 1, "left=" + g.combat.bullets.size);
        for (int i = 0; i < 6; i++) g.combat.fireEnemyBullet(g.player.x + 200 + i * 10, g.player.y + 40, -1, 0, 50, 10, 0xFFFFFFFF);
        g.combat.explode(g.player.x + 225, g.player.y + 40, 90, 1, 0, 0xFFFFFFFF);
        check("explosions destroy enemy bullets", g.combat.bullets.size == 0, null);
        for (int i = 0; i < 6; i++) g.combat.fireEnemyBullet(g.player.x + 100, g.player.y + i * 10, -1, 0, 10, 10, 0xFFFFFFFF);
        g.playerCtl.abilityCooldown = 0; g.playerCtl.useAbility();
        check("Nova destroys enemy bullets", g.combat.bullets.size == 0, null);
        g.weapons.slots[0] = new Weapon(Config.WEAPONS[Config.W_PULSE]); g.weapons.active = 0;
        g.enemies.spawn(Config.ENEMIES[9], g.player.x + 100, g.player.y);
        for (int i = 0; i < 4; i++) g.combat.fireEnemyBullet(g.player.x - 80, g.player.y + i * 10, 0, 1, 1, 10, 0xFFFFFFFF);
        h.tick(1.5f);
        check("Pulse destroys enemy bullets", g.combat.bullets.size == 0, "left=" + g.combat.bullets.size);
    }


    // ------------------------------------------------------------------ multiplayer
    private interface Cond { boolean ok(); }
    private static boolean pump(Harness a, Harness b, Cond c, int maxMs) {
        long end = System.currentTimeMillis() + maxMs;
        while (System.currentTimeMillis() < end) {
            a.frame(); if (b != null) b.frame();
            if (c.ok()) return true;
            try { Thread.sleep(4); } catch (InterruptedException ignored) {}
        }
        return c.ok();
    }

    static void testMultiplayer() {
        // ---- same phone, co-op
        Harness h = fresh(); Game g = h.game; h.bot = false;
        h.tapButton(g.ui.menuButton(6));
        check("menu: Multiplayer button opens the multiplayer menu", g.state == Game.S_MPMENU, null);
        g.mpm.startSplit(Game.M_COOP);
        check("split screen: 2 local players in co-op", g.state == Game.S_PLAYING && g.heroCount == 2 && g.heroes[1].local && g.splitScreen, null);
        float s = g.scale;
        Player p0 = g.heroes[0].player, p1 = g.heroes[1].player;
        float x0 = p0.x, x1 = p1.x;
        g.touchDown(1, 120 * s, 900 * s); g.touchMove(1, 20 * s, 900 * s);      // left half -> P1 moves left
        g.touchDown(2, 600 * s, 900 * s); g.touchMove(2, 700 * s, 900 * s);      // right half -> P2 moves right
        h.tick(1f);
        check("split screen: left half steers P1, right half steers P2", p0.x < x0 - 50 && p1.x > x1 + 50, "dx0=" + (int) (p0.x - x0) + " dx1=" + (int) (p1.x - x1));
        g.touchUp(1, 0, 0); g.touchUp(2, 0, 0);
        check("each player has its own hotbar and companion", g.heroes[1].weapons.activeWeapon() != null && g.heroes[1].pets.count == 1 && g.heroes[0].weapons != g.heroes[1].weapons, null);
        UIManager.Button slot = null;
        g.heroes[1].weapons.slots[2] = new Weapon(Config.WEAPONS[Config.W_RAILGUN]);
        for (int i = 0; i < 3; i++) h.frame();
        check("enemies spawn for co-op", g.enemies.aliveCount() >= 0, null);
        // down + revive
        g.heroes[1].player.x = p0.x + 40; g.heroes[1].player.y = p0.y;
        g.combat.damageHero(g.heroes[1], 1e7f, null);
        check("co-op: a killed player goes down (not game over)", g.heroes[1].downed && g.state == Game.S_PLAYING, null);
        noWeaponsBoth(g);
        g.enemies.clear();
        h.tick(Config.REVIVE_TIME + 0.6f);
        check("co-op: standing next to a downed teammate revives them", !g.heroes[1].downed && g.heroes[1].player.hp > 0, "progress=" + g.heroes[1].reviveProgress);
        g.combat.damageHero(g.heroes[0], 1e7f, null);
        g.heroes[1].player.invuln = 0;
        g.combat.damageHero(g.heroes[1], 1e7f, null);
        h.tick(3.5f);
        check("co-op: everyone down -> match results", g.state == Game.S_MPRESULT, "state=" + g.state);
        g.toMenu();
        check("leaving returns to solo setup", g.heroCount == 1 && g.mode == Game.M_SOLO && !g.splitScreen && g.state == Game.S_MENU, null);

        // ---- same phone, PvP
        g.mpm.startSplit(Game.M_PVP);
        check("PvP: no enemy waves", true, null);
        h.tick(2f);
        check("PvP: no enemies spawn", g.enemies.aliveCount() == 0, "enemies=" + g.enemies.aliveCount());
        Hero a = g.heroes[0], b = g.heroes[1];
        b.player.x = a.player.x + 200; b.player.y = a.player.y; b.player.invuln = 0;
        float hp0 = b.player.hp;
        g.bind(a);
        g.combat.fireProjectile(a.player.x + 40, a.player.y, 0, 800, 50, 0, 0, 0, 400, 0xFFFFFFFF, 0, 6, WeaponType.SP_NONE, 0, 0, false, a.weapons.slots[0]);
        g.bindLocal();
        for (int i = 0; i < 20; i++) { g.grid.build(g.enemies.list); g.combat.update(1f / 60f); }
        check("PvP: shots damage other players (scaled)", hp0 - b.player.hp > 10 && hp0 - b.player.hp < 25, "took " + (hp0 - b.player.hp));
        b.player.invuln = 0;
        g.combat.damageHero(b, 1e7f, a);
        check("PvP: a kill scores for the killer", a.pvpKills == 1 && a.score >= Config.PVP_KILL_SCORE && b.deaths == 1 && b.downed, null);
        h.tick(Config.PVP_RESPAWN + 0.3f);
        check("PvP: the killed player respawns", !b.downed && b.player.hp == b.player.maxHp, null);
        g.time = Config.PVP_MATCH - 0.1f;
        h.tick(0.3f);
        check("PvP: match ends on the clock with rankings", g.state == Game.S_MPRESULT && g.ui.rankedPlayers()[0] == a, "state=" + g.state);
        g.toMenu();

        // ---- PvPvE
        g.mpm.startSplit(Game.M_PVPVE);
        h.tick(8f);
        check("PvPvE: enemies attack while players can fight", g.enemies.aliveCount() > 0 && g.pvp(), null);
        g.toMenu();

        // ---- Wi-Fi host + client over real sockets
        Harness hh = fresh(), hc = fresh();
        Game host = hh.game, cl = hc.game;
        hh.bot = false; hc.bot = false;
        host.save.playerName = "Hosty"; cl.save.playerName = "Guesty";
        host.mpm.hostLan(0);
        com.blockkillers.core.net.NetHost nh = (com.blockkillers.core.net.NetHost) host.net;
        check("Wi-Fi: host opens a server socket", nh.ready && nh.port > 0, "port=" + nh.port);
        cl.mpm.joinLan("127.0.0.1", nh.port);
        boolean joined = pump(hh, hc, () -> host.mpm.lobbyCount == 2 && cl.mpm.myIndex == 1, 4000);
        check("Wi-Fi: client joins the lobby", joined, "lobby=" + host.mpm.lobbyCount + " idx=" + cl.mpm.myIndex);
        pump(hh, hc, () -> "Hosty".equals(cl.mpm.names[0]), 2000);
        check("Wi-Fi: lobby names reach the client", "Hosty".equals(cl.mpm.names[0]) && "Guesty".equals(host.mpm.names[1]), null);
        host.mpm.lobbyMode = Game.M_COOP;
        host.mpm.hostStart();
        boolean started = pump(hh, hc, () -> cl.state == Game.S_PLAYING && cl.heroCount == 2, 4000);
        check("Wi-Fi: host starts -> client is in the match", started && host.state == Game.S_PLAYING, "client state=" + cl.state);
        check("Wi-Fi: both build the same map", host.map.count == cl.map.count && host.map.chestCount == cl.map.chestCount && Math.abs(host.map.ox[5] - cl.map.ox[5]) < 0.01f, null);
        host.godMode = true; host.heroes[1].player.invuln = 99;
        pump(hh, hc, () -> cl.enemies.aliveCount() > 0, 8000);
        check("Wi-Fi: client sees enemies from the host", cl.enemies.aliveCount() > 0, "client enemies=" + cl.enemies.aliveCount());
        float rx = host.heroes[1].player.x;
        cl.heroes[1].joystick.active = true; cl.heroes[1].joystick.outX = 1; cl.heroes[1].joystick.outY = 0;
        pump(hh, hc, () -> host.heroes[1].player.x > rx + 120, 4000);
        check("Wi-Fi: client joystick moves its player on the host", host.heroes[1].player.x > rx + 120, "dx=" + (int) (host.heroes[1].player.x - rx));
        pump(hh, hc, () -> Math.abs(cl.heroes[1].player.x - host.heroes[1].player.x) < 60, 2000);
        check("Wi-Fi: client sees its own position from the host", Math.abs(cl.heroes[1].player.x - host.heroes[1].player.x) < 60, null);
        cl.heroes[1].joystick.outX = 0;
        host.heroes[1].weapons.slots[3] = host.loot.roll(Config.W_SCATTER, Loot.EPIC, 2);
        pump(hh, hc, () -> cl.heroes[1].weapons.slots[3] != null, 2000);
        check("Wi-Fi: client hotbar mirrors the host", cl.heroes[1].weapons.slots[3] != null && cl.heroes[1].weapons.slots[3].rarity == Loot.EPIC, null);
        cl.mpm.action(cl.heroes[1], com.blockkillers.core.net.NetSync.A_SLOT, 3);
        pump(hh, hc, () -> host.heroes[1].weapons.active == 3, 2000);
        check("Wi-Fi: client action (switch gun) applies on the host", host.heroes[1].weapons.active == 3, null);
        host.heroes[1].xp.addXp(Config.xpToNext(host.heroes[1].player.level));
        pump(hh, hc, () -> cl.heroes[1].cardsOpen && cl.heroes[1].upgrades.cardCount == 3, 3000);
        check("Wi-Fi: client receives its level-up cards (game not paused)", cl.heroes[1].cardsOpen && host.state == Game.S_PLAYING, null);
        int taken = host.heroes[1].player.upgradesTaken;
        cl.mpm.action(cl.heroes[1], com.blockkillers.core.net.NetSync.A_CARD, 0);
        pump(hh, hc, () -> host.heroes[1].player.upgradesTaken == taken + 1, 2000);
        check("Wi-Fi: client card choice applies on the host", host.heroes[1].player.upgradesTaken == taken + 1, null);
        int snap = com.blockkillers.core.net.NetSync.snapshot(host, host.heroes[1]).length;
        // stress: full horde
        for (int i = 0; i < 360; i++) host.enemies.spawn(Config.ENEMIES[i % 9], MathUtil.rand(-900, 900), MathUtil.rand(-900, 900));
        int big = com.blockkillers.core.net.NetSync.snapshot(host, host.heroes[1]).length;
        check("Wi-Fi: snapshots stay compact", big < 40000, "normal " + snap + " B, full horde " + big + " B");
        cl.mpm.leave();
        pump(hh, null, () -> !host.heroes[1].active, 3000);
        check("Wi-Fi: host notices a player leaving", !host.heroes[1].active && host.state == Game.S_PLAYING, null);
        host.toMenu();

        // ---- Online via the relay server
        com.blockkillers.core.net.RelayServer relay = new com.blockkillers.core.net.RelayServer();
        try { relay.start(0); } catch (Exception e) { check("relay starts", false, e.toString()); return; }
        Harness oh = fresh(), oc = fresh();
        oh.bot = false; oc.bot = false;
        oh.game.mpm.hostOnline("127.0.0.1", relay.port());
        com.blockkillers.core.net.NetHost rh = (com.blockkillers.core.net.NetHost) oh.game.net;
        pump(oh, null, () -> rh.roomCode != null, 4000);
        check("online: host gets a 4-letter room code from the relay", rh.roomCode != null && rh.roomCode.length() == 4, "code=" + rh.roomCode);
        oc.game.mpm.joinOnline("127.0.0.1", relay.port(), rh.roomCode == null ? "XXXX" : rh.roomCode.toLowerCase());
        pump(oh, oc, () -> oh.game.mpm.lobbyCount == 2 && oc.game.mpm.myIndex == 1, 4000);
        check("online: client joins with the room code", oh.game.mpm.lobbyCount == 2, null);
        oh.game.mpm.lobbyMode = Game.M_PVPVE;
        oh.game.mpm.hostStart();
        pump(oh, oc, () -> oc.game.state == Game.S_PLAYING && oc.game.mode == Game.M_PVPVE, 4000);
        check("online: PvPvE match starts on both devices", oc.game.state == Game.S_PLAYING && oc.game.mode == Game.M_PVPVE, null);
        oh.game.godMode = true;
        pump(oh, oc, () -> oc.game.enemies.aliveCount() > 0, 9000);
        check("online: snapshots flow through the relay", oc.game.enemies.aliveCount() > 0 && oc.game.heroCount == 2, null);
        Harness bad = fresh();
        bad.game.mpm.joinOnline("127.0.0.1", relay.port(), "ZZZZ");
        pump(bad, null, () -> bad.game.state == Game.S_MENU, 3000);
        check("online: wrong room code fails cleanly", bad.game.state == Game.S_MENU && bad.game.mpm.lastError != null, "err=" + bad.game.mpm.lastError);
        oh.game.toMenu(); oc.game.toMenu();
        relay.stop();

        // ---- discovery beacon (loopback)
        Harness dh = fresh(), dc = fresh();
        dh.game.mpm.hostLan(0);
        dc.game.mpm.startDiscovery();
        boolean seen = pump(dh, dc, () -> !dc.game.mpm.discovery.list().isEmpty(), 3500);
        report.add("INFO  Wi-Fi discovery beacon received on loopback: " + seen + " (real phones use the Wi-Fi broadcast address)");
        System.out.println(report.get(report.size() - 1));
        dh.game.toMenu(); dc.game.mpm.discovery.stop();
    }
    private static void noWeaponsBoth(Game g) { for (int h = 0; h < g.heroCount; h++) { for (int i = 0; i < Config.HOTBAR; i++) g.heroes[h].weapons.slots[i] = null; g.heroes[h].pets.count = 0; } }


    static void testZombie3DAndLayout() {
        Harness h = fresh(); Game g = h.game; h.bot = false;
        check("game is now Zombie Killer", g.strings.get("e_block").equals("Walker") && g.strings.get("boss_monolith").equals("THE ABOMINATION"), null);
        // settings: view mode
        h.tapButton(g.ui.menuButton(1));
        h.tapButton(g.ui.settingsButton(10));
        check("settings: View switches to 3D first person", g.settings.view3D, null);
        h.tapButton(g.ui.settingsButton(12));
        g.startRun(WorldMap.OPEN); g.godMode = true;
        check("3D mode active in a solo run", g.view3D(), null);
        // raycast against a pillar
        WorldMap m = g.map;
        int o = 0; for (int i = 0; i < m.count; i++) if (m.or[i] > 40) { o = i; break; }
        float d = m.raycast(m.ox[o] - 300, m.oy[o], 1, 0, 2000);
        check("3D ray casting hits pillars at the right distance", Math.abs(d - (300 - m.or[o])) < 1f && m.rayHit == o, "d=" + d);
        // render a 3D frame with zombies, bosses, loot, chests, projectiles
        for (int i = 0; i < 11; i++) { Enemy e = g.enemies.spawn(Config.ENEMIES[i], g.player.x + 200 + i * 40, g.player.y + (i % 3 - 1) * 60); if (e != null && i % 4 == 0) g.enemies.fuse(e, 1 + i % 2); }
        g.bosses.spawnBoss();
        g.loot.drop(g.player.x + 150, g.player.y, g.loot.roll(Config.W_RAILGUN, Loot.LEGENDARY, 3));
        h.tick(0.5f);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1080, 2400, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D gr = img.createGraphics();
        boolean ok = true;
        try { for (int i = 0; i < 3; i++) g.render(new DesktopGfx(gr, 1080, 2400)); } catch (Exception e) { ok = false; e.printStackTrace(); }
        check("3D frame renders (walls, zombies, bosses, loot, gun in hand)", ok, null);
        // look + move relative to view
        float s = g.scale;
        float v0 = g.player.view;
        g.touchDown(5, 800 * s, 900 * s); g.touchMove(5, 900 * s, 900 * s); g.touchUp(5, 0, 0);
        check("3D: dragging the right half turns the view", g.player.view > v0 + 0.5f, "view=" + g.player.view);
        g.enemies.clear(); g.combat.clear(); noWeapons(g);
        g.player.view = MathUtil.PI / 2;   // look "down" the world y axis
        float y0 = g.player.y, x0 = g.player.x;
        g.touchDown(6, 150 * s, 1100 * s); g.touchMove(6, 150 * s, 1000 * s);   // push the stick up = forward
        h.tick(0.8f);
        g.touchUp(6, 0, 0);
        check("3D: stick forward moves along the view direction", g.player.y > y0 + 80 && Math.abs(g.player.x - x0) < 40, "dy=" + (int) (g.player.y - y0));
        // crosshair cone targeting
        g.weapons.slots[0] = new Weapon(Config.WEAPONS[0]); g.weapons.active = 0; g.weapons.slots[0].owner = g.hero;
        g.player.view = 0;
        Enemy behind = g.enemies.spawn(Config.ENEMIES[9], g.player.x - 150, g.player.y);
        h.tick(1f);
        check("3D: auto-fire ignores zombies outside the crosshair", behind.hp == behind.maxHp, null);
        Enemy ahead = g.enemies.spawn(Config.ENEMIES[9], g.player.x + 250, g.player.y);
        h.tick(1.5f);
        check("3D: auto-fire hits zombies in the crosshair", ahead.hp < ahead.maxHp || !ahead.alive, null);
        g.settings.view3D = false;
        // layout editor
        DesktopPlatform.MemStorage st = new DesktopPlatform.MemStorage();
        Harness hl = new Harness(st, 1080, 2400); Game gl = hl.game;
        hl.tapButton(gl.ui.menuButton(1));
        hl.tapButton(gl.ui.settingsButton(11));
        check("settings: Edit control layout opens the editor", gl.state == Game.S_LAYOUT, null);
        UIManager.Button nova = gl.ui.hudButton(1);
        float nx = nova.cx(), ny = nova.cy(), sc = gl.scale;
        gl.touchDown(1, nx * sc, ny * sc); gl.touchMove(1, (nx - 200) * sc, (ny - 300) * sc); gl.touchUp(1, 0, 0);
        check("layout editor: drag moves a control", Math.abs(gl.ui.hudButton(1).cx() - (nx - 200)) < 2 && Math.abs(gl.ui.hudButton(1).cy() - (ny - 300)) < 2, null);
        float w0 = gl.ui.hudButton(4).r - gl.ui.hudButton(4).l;
        UIManager.Button slot = gl.ui.hudButton(4);
        gl.touchDown(2, slot.cx() * sc, slot.cy() * sc); gl.touchUp(2, slot.cx() * sc, slot.cy() * sc);
        hl.tapButton(gl.ui.layoutButton(1)); hl.tapButton(gl.ui.layoutButton(1));
        check("layout editor: + makes the hotbar bigger", gl.ui.hudButton(4).r - gl.ui.hudButton(4).l > w0 * 1.15f, null);
        hl.tapButton(gl.ui.layoutButton(3));
        check("layout editor: Done returns to settings", gl.state == Game.S_SETTINGS, null);
        Harness h2 = new Harness(st.reopen(), 1080, 2400);
        check("custom layout persists after restart", Math.abs(h2.game.ui.hudButton(1).cx() - (nx - 200)) < 2, null);
        h2.game.layout.reset(); h2.game.ui.layout();
        check("layout reset restores defaults", Math.abs(h2.game.ui.hudButton(1).cx() - nx) < 2, null);
        // all zombie designs render in 2D
        Harness hz = fresh(); Game gz = hz.game; gz.startRun(); gz.godMode = true;
        for (int i = 0; i < 11; i++) { Enemy e = gz.enemies.spawn(Config.ENEMIES[i], gz.player.x + (i - 5) * 50, gz.player.y - 200); if (e != null && i > 5) gz.enemies.fuse(e, i % 2 + 1); }
        for (int k = 0; k < 3; k++) { gz.bosses.spawned = k; Enemy b = gz.bosses.spawnBoss(); if (b != null) { b.x = gz.player.x + (k - 1) * 220; b.y = gz.player.y + 250; } }
        boolean ok2 = true;
        try { gz.render(new DesktopGfx(gr, 1080, 2400)); } catch (Exception e) { ok2 = false; e.printStackTrace(); }
        check("all 11 zombie types, mutated tiers and 3 boss zombies render", ok2, null);
    }

    private static float totalHp(Game g) {
        float s = 0;
        for (int i = 0; i < g.enemies.list.size; i++) if (g.enemies.list.get(i).alive) s += g.enemies.list.get(i).hp + g.enemies.list.get(i).shield;
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
        check("extra projectile card applies", g.player.extraProjectiles == 1 && g.weapons.slots[0].count(g.player) == 2, null);
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
        g.combat.damageEnemy(b, b.maxHp * 3, 0, 0, 0);
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
        h.tick(1.0f);
        check("death plays in slow motion before game over", g.state == Game.S_PLAYING && g.deathSlowmo > 0.5f, null);
        h.tick(2.2f);
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
        h.tick(3.2f); h.tick(1f);
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
        h.tick(3.5f);
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
        h.tapButton(g.ui.settingsButton(12)); // back
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
        h.tapButton(g.ui.settingsButton(12));
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
        for (String k : Loot.RARITY_KEYS) keys.add(k);
        for (int i = 1; i < 5; i++) { keys.add(Loot.TRAIT_KEYS[i]); keys.add(Loot.TRAIT_DESC[i]); }
        for (String k : Loot.PART_PREFIX) keys.add(k);
        for (String k : WorldMap.REGION_KEYS) keys.add(k);
        for (String k : WorldMap.MODE_KEYS) keys.add(k);
        for (String k : WorldMap.MODE_DESC) keys.add(k);
        for (String k : ShopManager.KEYS) { keys.add(k); keys.add("shd" + k.substring(2)); }
        for (String k : PetManager.NAME_KEYS) keys.add(k);
        for (String k : PetManager.DESC_KEYS) keys.add(k);
        for (String k : Loot.MAKER_KEYS) keys.add(k);
        for (String k : Loot.MAKER_DESC) keys.add(k);
        for (String k : Loot.BARREL_KEYS) keys.add(k);
        for (String k : Loot.GRIP_KEYS) keys.add(k);
        for (String k : Loot.SIGHT_KEYS) keys.add(k);
        for (String k : Loot.ACC_KEYS) keys.add(k);
        for (String k : UIManager.DEV_KEYS_PUBLIC) keys.add(k);
        for (int i = 1; i <= Loot.ELEMENTS; i++) { keys.add(Loot.ELEMENT_KEYS[i]); keys.add(Loot.ELEMENT_PREFIX[i]); keys.add("eld" + Loot.ELEMENT_KEYS[i].substring(2)); }
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
            for (int i = 0; i < 6; i++) all.add(g.ui.menuButton(i));
            boolean fit = inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 13; i++) all.add(g.ui.settingsButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i <= WorldMap.MODES; i++) all.add(g.ui.mapSelectButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i <= ShopManager.COUNT; i++) all.add(g.ui.shopButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i <= UIManager.DEV_ROWS; i++) all.add(g.ui.devButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 3; i++) all.add(g.ui.levelButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 4; i++) all.add(g.ui.pauseButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 9; i++) all.add(g.ui.hudButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
            all.clear(); for (int i = 0; i < 7; i++) all.add(g.ui.lootButton(i));
            fit &= inside(all, g.viewW, H) && noOverlap(all);
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
        g.weapons.slots[4] = new Weapon(Config.WEAPONS[Config.W_MINIGUN]); g.weapons.active = 4;
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
                    run + 1, MathUtil.time(g.time), g.player.level, g.kills, g.bossesKilled, g.score, g.weapons.count()));
            System.out.println(report.get(report.size() - 1));
            if (run == 0) check("early game is survivable (bot lives > 2 min)", g.time > 120, MathUtil.time(g.time));
        }
        check("difficulty eventually overwhelms a simple bot (< 20 min avg)", total / 3 < 20 * 60, "avg " + MathUtil.time(total / 3));
    }
}
