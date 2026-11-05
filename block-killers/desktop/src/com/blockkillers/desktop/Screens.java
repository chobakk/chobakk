package com.blockkillers.desktop;

import com.blockkillers.core.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Renders every screen of the real game to PNG at several phone/tablet resolutions. */
public final class Screens {
    static File dir;

    public static void main(String[] args) throws Exception {
        dir = new File(args.length > 0 ? args[0] : "docs/screenshots");
        dir.mkdirs();
        MathUtil.RNG.setSeed(4);
        shoot(1080, 2400, "");
        shoot(720, 1280, "-720x1280");
        shoot(1600, 2560, "-tablet");
        System.out.println("screenshots written to " + dir);
    }

    static void save(Game g, int w, int h, String name) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = img.createGraphics();
        g.render(new DesktopGfx(gr, w, h));
        gr.dispose();
        // store at half resolution to keep the repository small
        int sw = w / 2, sh = h / 2;
        BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = small.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(img, 0, 0, sw, sh, null);
        sg.dispose();
        ImageIO.write(small, "png", new File(dir, name + ".png"));
    }

    static void shoot(int w, int h, String suffix) throws Exception {
        Harness hs = new Harness(new DesktopPlatform.MemStorage(), w, h);
        Game g = hs.game;
        g.save.bestScore = 48210;
        hs.tick(1.5f);
        save(g, w, h, "01-menu" + suffix);
        if (!suffix.isEmpty()) {
            g.startRun(); g.godMode = true;
            hs.tick(75f);
            while (g.state == Game.S_LEVELUP) hs.tick(0.6f);
            save(g, w, h, "03-gameplay" + suffix);
            return;
        }
        g.startRun();
        g.godMode = true;
        hs.tick(3f);
        save(g, w, h, "02-start");
        hs.tick(150f);
        while (g.state == Game.S_LEVELUP) hs.tick(0.6f);
        // show the joystick in use
        float s = g.scale;
        hs.bot = false;
        g.playerCtl.overrideInput = false;
        g.touchDown(1, 170 * s, (g.viewH - 260) * s);
        g.touchMove(1, 225 * s, (g.viewH - 300) * s);
        hs.tick(0.2f);
        save(g, w, h, "03-gameplay");
        g.touchUp(1, 0, 0);
        hs.bot = true;
        g.xp.addXp(Config.xpToNext(g.player.level));
        hs.tick(0.05f);
        hs.tick(0.4f);
        save(g, w, h, "04-level-up");
        g.chooseUpgrade(0);
        while (g.time < 302) { hs.frame(); while (g.state == Game.S_LEVELUP) { g.chooseUpgrade(0); } }
        hs.tick(6f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        // bring the boss on screen
        Enemy b = g.bosses.current;
        if (b != null) { b.x = g.player.x + 160; b.y = g.player.y - 330; }
        hs.tick(0.6f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        save(g, w, h, "05-boss");
        g.pause();
        save(g, w, h, "06-pause");
        g.openSettings();
        save(g, w, h, "07-settings");
        g.closeSettings();
        g.resume();
        g.godMode = false;
        g.player.invuln = 0;
        g.combat.damagePlayer(1e6f);
        hs.tick(3f);
        save(g, w, h, "08-game-over");
        g.toMenu();
        g.state = Game.S_STATS;
        save(g, w, h, "09-statistics");
        g.settings.language = SettingsManager.LANG_FA;
        g.state = Game.S_MENU;
        save(g, w, h, "10-menu-persian");
        g.openSettings();
        save(g, w, h, "11-settings-persian");
        g.settings.language = SettingsManager.LANG_EN;
        lootAndMap(w, h);
        newScreens(w, h);
    }

    static void newScreens(int w, int h) throws Exception {
        Harness hs = new Harness(new DesktopPlatform.MemStorage(), w, h);
        Game g = hs.game;
        g.save.coins = 2380; g.save.bestByMap[0] = 48210; g.save.bestByMap[2] = 12040;
        g.shop.levels[0] = 3; g.shop.levels[1] = 2; g.shop.levels[6] = 1;
        g.state = Game.S_MAPSELECT; hs.tick(0.2f); save(g, w, h, "16-map-select");
        g.state = Game.S_SHOP; save(g, w, h, "17-shop");
        g.dev.godMode = true; g.dev.startLevel = 10;
        g.state = Game.S_DEV; save(g, w, h, "18-dev-menu");
        g.dev.godMode = false; g.dev.startLevel = 1;
        // corridor
        g.startRun(WorldMap.CORRIDOR); g.godMode = true;
        g.pets.add(PetManager.TURRET); g.pets.add(PetManager.WISP);
        hs.tick(70f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        hs.tick(0.3f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        save(g, w, h, "19-corridor");
        // arena with shields/armor + pets + parts gun
        g.startRun(WorldMap.ARENA); g.godMode = true;
        g.pets.add(PetManager.SPRITE);
        g.weapons.slots[0] = g.loot.roll(Config.W_TWIN, Loot.EPIC, 4); g.weapons.slots[0].element = Loot.E_SHOCK;
        g.time = 380;
        hs.tick(25f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        hs.tick(0.3f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        save(g, w, h, "20-arena");
        // death scene
        g.godMode = false; g.player.invuln = 0; g.combat.damagePlayer(1e7f);
        hs.tick(1.3f);
        save(g, w, h, "21-death");
    }

    /** New systems: evolved player + gun, ground loot, loot screen, map, fused enemies. */
    static void lootAndMap(int w, int h) throws Exception {
        Harness hs = new Harness(new DesktopPlatform.MemStorage(), w, h);
        Game g = hs.game;
        g.startRun(); g.godMode = true;
        hs.tick(200f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        hs.bot = false; g.playerCtl.overrideInput = true; g.playerCtl.inputX = 0; g.playerCtl.inputY = 0;
        // give a visibly evolved body and a full hotbar of rolled guns
        int[] f = g.player.features;
        f[UpgradeManager.DAMAGE] = 3; f[UpgradeManager.ARMOR] = 2; f[UpgradeManager.ATTACK_SPEED] = 3; f[UpgradeManager.MAX_HP] = 2;
        f[UpgradeManager.CRIT] = 1; f[UpgradeManager.XP_BOOST] = 1; g.player.upgradesTaken = 14;
        int[] types = {Config.W_SCATTER, Config.W_RAILGUN, Config.W_FLAMER, Config.W_MINIGUN, Config.W_LASER};
        for (int i = 0; i < 5; i++) g.weapons.slots[i] = g.loot.roll(types[i], i, 5);
        g.weapons.active = 2;
        for (int i = 0; i < 5; i++) g.loot.drop(g.player.x - 260 + i * 130, g.player.y + 230, g.loot.roll(MathUtil.randInt(17), i, 6));
        g.time = 560;   // fused enemies era
        for (int i = 0; i < 14; i++) {
            float a = i * MathUtil.TAU / 14;
            Enemy e = g.enemies.spawn(Config.ENEMIES[MathUtil.randInt(9)], g.player.x + MathUtil.cos(a) * 300, g.player.y + MathUtil.sin(a) * 300 - 80);
            if (e != null && i % 2 == 0) g.enemies.fuse(e, i % 4 == 0 ? 2 : 1);
        }
        g.state = Game.S_PLAYING;
        hs.tick(0.5f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        save(g, w, h, "12-loot-and-fused");
        // stand on a drop and open the loot screen
        GroundItem it = g.loot.items.get(g.loot.items.size - 1);
        it.weapon = g.loot.roll(Config.W_SEEKER, Loot.LEGENDARY, 7);
        g.player.x = it.x; g.player.y = it.y; it.vx = it.vy = 0;
        hs.tick(0.1f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        save(g, w, h, "13-pickup");
        g.openLoot();
        save(g, w, h, "14-loot-screen");
        g.closeLoot();
        g.player.x = 1500; g.player.y = -1200; hs.tick(0.1f);
        g.player.x = -2200; g.player.y = 1800; hs.tick(0.1f);
        g.player.x = 300; g.player.y = 300; hs.tick(0.1f);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        g.openMap();
        save(g, w, h, "15-map");
    }
}
