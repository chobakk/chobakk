package com.blockkillers.android;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.app.AlertDialog;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.blockkillers.core.Audio;
import com.blockkillers.core.Game;
import com.blockkillers.core.Platform;
import com.blockkillers.core.Storage;

/** Android entry point: full-screen immersive activity hosting the game view. */
public final class MainActivity extends Activity implements Platform {
    private Game game;
    private GameView view;
    private AndroidAudio audio;
    private AndroidStorage storage;
    private Vibrator vibrator;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        storage = new AndroidStorage(this);
        audio = new AndroidAudio(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        game = new Game(this);
        view = new GameView(this, game);
        setContentView(view);
        // Replace the splash window background once the game draws its own frames.
        getWindow().setBackgroundDrawable(null);
        hideSystemUi();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
        else if (game != null) game.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        hideSystemUi();
        view.start();
        game.onHostResume();
    }

    @Override protected void onPause() {
        game.onHostPause();
        view.stop();
        super.onPause();
    }

    @Override protected void onDestroy() {
        audio.release();
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (!game.onBack()) super.onBackPressed();
    }

    /** For instrumentation / Robolectric tests. */
    Game game() { return game; }
    GameView gameView() { return view; }

    // ---- Platform ----
    @Override public Storage storage() { return storage; }
    @Override public Audio audio() { return audio; }
    @Override public boolean canExit() { return true; }
    @Override public void exitApp() { game.onHostPause(); finish(); }

    @Override public void promptText(final String title, final String initial, final TextCallback cb) {
        runOnUiThread(() -> {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            input.setText(initial);
            input.setSelectAllOnFocus(true);
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        final String text = input.getText().toString().trim();
                        // deliver on the next frame (game thread == UI thread here)
                        view.post(() -> cb.done(text));
                        hideSystemUi();
                    })
                    .setNegativeButton(android.R.string.cancel, (d, w) -> hideSystemUi())
                    .show();
        });
    }

    @Override public Object loadImage(String path) {
        try (java.io.InputStream in = getAssets().open(path)) {
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
            return android.graphics.BitmapFactory.decodeStream(in, null, o);
        } catch (Exception e) { return null; }
    }

    @Override public String loadText(String path) {
        try (java.io.InputStream in = getAssets().open(path)) {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        } catch (Exception e) { return null; }
    }

    private WifiManager.MulticastLock multicastLock;

    @Override public void setNetworkActive(boolean active) {
        try {
            if (active) {
                if (multicastLock == null) {
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm == null) return;
                    multicastLock = wm.createMulticastLock("blockkillers-discovery");
                    multicastLock.setReferenceCounted(false);
                }
                multicastLock.acquire();
            } else if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        } catch (RuntimeException ignored) { }
    }

    @SuppressWarnings("deprecation")
    @Override public void vibrate(int ms) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(ms);
        } catch (RuntimeException ignored) { }
    }
}
