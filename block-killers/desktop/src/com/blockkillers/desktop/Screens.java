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
    }
}
