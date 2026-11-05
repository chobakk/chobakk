package com.blockkillers.core;

/** Pooled visual particle. kind: 0 dot, 1 shard (rotating triangle), 2 ring (expanding shockwave), 3 line spark. */
public final class Particle {
    public float x, y, vx, vy, life, maxLife, size, rot, rotSpeed, drag;
    public int color, kind;
}
