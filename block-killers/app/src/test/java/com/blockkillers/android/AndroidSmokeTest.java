package com.blockkillers.android;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.blockkillers.core.Config;
import com.blockkillers.core.Game;
import com.blockkillers.core.UIManager;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowChoreographer;
import java.time.Duration;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config.*;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import static org.robolectric.Shadows.shadowOf;

/**
 * Runs the real Android layer (Activity, GameView, AndroidGfx on a real Skia Canvas, SharedPreferences,
 * SoundPool/AudioTrack, lifecycle, MotionEvents) on the JVM with Robolectric's Android framework.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@org.robolectric.annotation.Config(sdk = 34, qualifiers = "w360dp-h800dp-port-xxhdpi")
public class AndroidSmokeTest {
    private static final int W = 1080, H = 2400;

    @Before public void vsync() {
        // Frames only advance when the test advances the clock (16 ms per vsync, like a 60 Hz display).
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));
    }

    /** Advance the real Choreographer-driven loop (GameView.doFrame -> Game.update -> invalidate). */
    private static void realFrames(int n) {
        for (int i = 0; i < n; i++) shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16));
    }

    private static void tap(GameView v, float px, float py) {
        long t = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, px, py, 0);
        MotionEvent up = MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, px, py, 0);
        v.dispatchTouchEvent(down);
        v.dispatchTouchEvent(up);
        down.recycle(); up.recycle();
    }
    private static void tapButton(Game g, GameView v, UIManager.Button b) { tap(v, b.cx() * g.scale, b.cy() * g.scale); }

    private static void frames(Game g, GameView v, int n) {
        for (int i = 0; i < n; i++) g.update(1f / 60f);
    }

    private static void snapshot(GameView v, String name) throws Exception {
        assertEquals("view width matches window", W, v.getWidth());
        Bitmap bmp = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(bmp));
        File dir = new File(System.getProperty("user.dir"), "build/android-screens");
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        // the frame must not be blank: sample a few pixels for non-background colours
        int distinct = 0, last = 0;
        for (int y = 0; y < bmp.getHeight(); y += 97) for (int x = 0; x < bmp.getWidth(); x += 89) {
            int p = bmp.getPixel(x, y);
            if (p != last) { distinct++; last = p; }
        }
        assertTrue("rendered frame " + name + " has content", distinct > 10);
    }

    private ActivityController<MainActivity> launch() {
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        GameView v = c.get().gameView();
        v.measure(W, H);
        v.layout(0, 0, W, H);
        shadowOf(Looper.getMainLooper()).idle();
        return c;
    }

    @Test public void launchPlayAndRenderAllScreens() throws Exception {
        ActivityController<MainActivity> c = launch();
        MainActivity a = c.get();
        Game g = a.game();
        GameView v = a.gameView();
        assertEquals("virtual width", Config.VIRTUAL_WIDTH, g.viewW, 0.01f);
        assertEquals(Game.S_MENU, g.state);
        float rt = g.realTime;
        realFrames(60);
        assertTrue("Choreographer drives the game loop (realTime advanced)", g.realTime - rt > 0.5f);
        snapshot(v, "01-menu");

        tapButton(g, v, g.ui.menuButton(0));
        assertEquals("PLAY opens map selector", Game.S_MAPSELECT, g.state);
        snapshot(v, "00-map-select");
        tapButton(g, v, g.ui.mapSelectButton(0));
        assertEquals("map card starts a run via real MotionEvents", Game.S_PLAYING, g.state);

        // joystick: press + drag with real MotionEvents
        long t = SystemClock.uptimeMillis();
        float sx = 200 * g.scale, sy = (g.viewH - 300) * g.scale;
        v.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, sx, sy, 0));
        v.dispatchTouchEvent(MotionEvent.obtain(t, t + 16, MotionEvent.ACTION_MOVE, sx + 150, sy, 0));
        frames(g, v, 60);
        assertTrue("player moved right via touch joystick", g.player.x > 150);
        v.dispatchTouchEvent(MotionEvent.obtain(t, t + 1000, MotionEvent.ACTION_UP, sx + 150, sy, 0));

        g.godMode = true;
        for (int i = 0; i < 60 * 90; i++) {
            g.update(1f / 60f);
            if (g.state == Game.S_LEVELUP && i % 40 == 0) g.chooseUpgrade(0);
            if (g.state == Game.S_LEVELUP && i % 40 == 1) { /* allow the card UI to animate */ }
        }
        if (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        assertTrue("enemies were killed", g.kills > 20);
        snapshot(v, "02-gameplay");

        g.xp.addXp(Config.xpToNext(g.player.level));
        frames(g, v, 30);
        assertEquals(Game.S_LEVELUP, g.state);
        snapshot(v, "03-level-up");
        frames(g, v, 30);
        tapButton(g, v, g.ui.levelButton(0));
        assertNotEquals("card tap handled", Game.S_GAMEOVER, g.state);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);

        g.bosses.spawnBoss();
        g.bosses.current.x = g.player.x + 150; g.bosses.current.y = g.player.y - 300;
        frames(g, v, 40);
        while (g.state == Game.S_LEVELUP) g.chooseUpgrade(0);
        snapshot(v, "04-boss");

        // switch guns with the hotbar
        g.weapons.slots[1] = g.loot.roll(com.blockkillers.core.Config.W_RAILGUN, com.blockkillers.core.Loot.LEGENDARY, 3);
        tapButton(g, v, g.ui.hudButton(5));
        assertEquals("hotbar tap switches gun", 1, g.weapons.active);
        tapButton(g, v, g.ui.hudButton(0));
        assertEquals("HUD pause", Game.S_PAUSED, g.state);
        snapshot(v, "05-pause");

        tapButton(g, v, g.ui.pauseButton(2));
        assertEquals(Game.S_SETTINGS, g.state);
        g.settings.language = 1;
        snapshot(v, "06-settings-fa");
        g.settings.language = 0;
        tapButton(g, v, g.ui.settingsButton(12));
        assertEquals(Game.S_PAUSED, g.state);
        tapButton(g, v, g.ui.pauseButton(0));

        g.godMode = false; g.player.invuln = 0;
        g.combat.damagePlayer(1e7f);
        frames(g, v, 80);
        snapshot(v, "08-death");
        frames(g, v, 140);
        assertEquals(Game.S_GAMEOVER, g.state);
        snapshot(v, "07-game-over");
        int score = g.score;

        // close app and reopen: SharedPreferences must hold the record
        c.pause().stop().destroy();
        SharedPreferences prefs = a.getSharedPreferences("block_killers_save", Context.MODE_PRIVATE);
        assertEquals("best score saved to SharedPreferences", score, prefs.getInt("best_score", -1));
        ActivityController<MainActivity> c2 = launch();
        assertEquals("best score restored after relaunch", score, c2.get().game().save.bestScore);
        assertEquals(1, c2.get().game().save.gamesPlayed);
    }

    @Test public void lifecycleAndBackButton() {
        ActivityController<MainActivity> c = launch();
        MainActivity a = c.get();
        Game g = a.game();
        g.startRun();
        frames(g, a.gameView(), 60);
        c.pause();
        assertEquals("backgrounding pauses the game", Game.S_PAUSED, g.state);
        c.resume();
        assertEquals("still paused after returning", Game.S_PAUSED, g.state);
        a.onBackPressed();
        assertEquals("back resumes from pause", Game.S_PLAYING, g.state);
        a.onBackPressed();
        assertEquals("back pauses", Game.S_PAUSED, g.state);
        g.toMenu();
        a.onBackPressed();
        assertTrue("back on main menu finishes activity", a.isFinishing());
    }

    @Test public void shopAndDevMenu() {
        ActivityController<MainActivity> c = launch();
        Game g = c.get().game();
        GameView v = c.get().gameView();
        tapButton(g, v, g.ui.menuButton(5));
        assertEquals(Game.S_DEV, g.state);
        tapButton(g, v, g.ui.devButton(0));
        assertEquals(1000, g.save.coins);
        tapButton(g, v, g.ui.devButton(UIManager.DEV_ROWS));
        tapButton(g, v, g.ui.menuButton(4));
        assertEquals(Game.S_SHOP, g.state);
        tapButton(g, v, g.ui.shopButton(0));
        assertEquals(1, g.shop.levels[0]);
        c.pause().stop().destroy();
        Game g2 = launch().get().game();
        assertEquals("shop level persisted", 1, g2.shop.levels[0]);
        assertTrue("coins persisted", g2.save.coins < 1000 && g2.save.coins > 0);
    }

    @Test public void corridorMap() {
        ActivityController<MainActivity> c = launch();
        Game g = c.get().game();
        g.startRun(com.blockkillers.core.WorldMap.CORRIDOR);
        g.godMode = true;
        g.playerCtl.overrideInput = true; g.playerCtl.inputX = 1; g.playerCtl.inputY = 1;
        frames(g, c.get().gameView(), 120);
        assertEquals("corridor: no vertical movement", 0f, g.player.y, 1f);
        assertTrue(g.player.x > 100);
    }

    @Test public void settingsPersistAcrossRelaunch() {
        ActivityController<MainActivity> c = launch();
        Game g = c.get().game();
        g.openSettings();
        g.ui.action(UIManager.A_QUALITY);   // High -> Low
        g.ui.action(UIManager.A_LANG);      // English -> Persian
        g.ui.action(UIManager.A_VIB);       // vibration off
        g.closeSettings();
        c.pause().stop().destroy();
        Game g2 = launch().get().game();
        assertEquals(0, g2.settings.quality);
        assertEquals(1, g2.settings.language);
        assertFalse(g2.settings.vibration);
        assertEquals("شروع", g2.strings.get("play"));
    }
}
