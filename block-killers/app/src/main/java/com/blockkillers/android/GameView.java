package com.blockkillers.android;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import com.blockkillers.core.Game;

/** Drives the game from vsync (Choreographer), renders via hardware Canvas and forwards multi-touch. */
final class GameView extends View implements Choreographer.FrameCallback {
    private final Game game;
    private final AndroidGfx gfx = new AndroidGfx();
    private boolean running;
    private long lastNs;

    GameView(Context ctx, Game game) {
        super(ctx);
        this.game = game;
        setFocusable(true);
        setKeepScreenOn(true);
    }

    void start() {
        if (running) return;
        running = true;
        lastNs = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    void stop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w > 0 && h > 0) game.resize(w, h);
    }

    @Override public void doFrame(long ns) {
        if (!running) return;
        Choreographer.getInstance().postFrameCallback(this);
        if (lastNs == 0) { lastNs = ns; return; }
        float dt = (ns - lastNs) / 1e9f;
        // Battery saver: update/render at ~30 FPS
        if (game.settings.batterySaver && dt < 1f / 32f) return;
        lastNs = ns;
        game.update(dt);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        gfx.setCanvas(canvas);
        game.render(gfx);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int idx = e.getActionIndex();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                game.touchDown(e.getPointerId(idx), e.getX(idx), e.getY(idx));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) game.touchMove(e.getPointerId(i), e.getX(i), e.getY(i));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                game.touchUp(e.getPointerId(idx), e.getX(idx), e.getY(idx));
                break;
            case MotionEvent.ACTION_CANCEL:
                for (int i = 0; i < e.getPointerCount(); i++) game.touchUp(e.getPointerId(i), -1, -1);
                break;
        }
        return true;
    }
}
