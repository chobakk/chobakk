package com.blockkillers.core;

/** Smooth follow camera with screen shake. */
public final class Camera {
    public float x, y, shake, offX, offY;
    /** World-to-screen zoom (1 = default; smaller = zoomed out as everything grows). */
    public float zoom = 1f, targetZoom = 1f;

    public void snap(float tx, float ty) { x = tx; y = ty; shake = 0; zoom = targetZoom = 1f; }

    public void update(float dt, float tx, float ty, boolean shakeEnabled) {
        float k = Math.min(1f, Config.CAMERA_LERP * dt);
        x += (tx - x) * k;
        y += (ty - y) * k;
        zoom += (targetZoom - zoom) * Math.min(1f, 1.5f * dt);
        shake = Math.max(0f, shake - dt * 2.5f);
        if (shakeEnabled && shake > 0) {
            float m = shake * shake * 22f;
            offX = MathUtil.rand(-m, m); offY = MathUtil.rand(-m, m);
        } else { offX = offY = 0; }
    }

    public void addShake(float amount) { shake = Math.min(1f, shake + amount); }
}
