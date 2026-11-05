package com.blockkillers.core;

/** Floating virtual joystick: appears where the thumb touches (left/centre area), outputs a -1..1 vector. */
public final class Joystick {
    public boolean active;
    public int pointer = -1;
    public float baseX, baseY, knobX, knobY, outX, outY;
    public float radius = 80f;

    public void down(int id, float x, float y, float sensitivity) {
        if (active) return;
        active = true; pointer = id;
        radius = 85f / MathUtil.clamp(sensitivity, 0.5f, 2f);
        baseX = knobX = x; baseY = knobY = y;
        outX = outY = 0;
    }

    public void move(int id, float x, float y) {
        if (!active || id != pointer) return;
        float dx = x - baseX, dy = y - baseY;
        float d = MathUtil.len(dx, dy);
        if (d > radius) {
            // Drag the base along so direction changes stay instant.
            float over = d - radius;
            baseX += dx / d * over; baseY += dy / d * over;
            dx = x - baseX; dy = y - baseY; d = radius;
        }
        knobX = x; knobY = y;
        float m = d / radius;
        if (m < 0.12f) { outX = outY = 0; }
        else {
            float s = Math.min(1f, (m - 0.12f) / 0.78f);
            outX = dx / d * s; outY = dy / d * s;
        }
    }

    public void up(int id) { if (id == pointer) reset(); }

    public void reset() { active = false; pointer = -1; outX = outY = 0; }
}
