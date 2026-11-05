package com.blockkillers.core;

/**
 * Original procedural zombie art. Two views of every zombie type:
 * top-down (2D mode) and front-facing billboards (3D first-person mode).
 * Type ids follow Config.ENEMIES: 0 walker, 1 runner, 2 crawler, 3 riot, 4 twitcher, 5 charger,
 * 6 soldier, 7 bloater, 8 spitter, 9 brute, 10 spawnling. Bosses: 0 abomination, 1 brood mother, 2 stalker.
 */
public final class Zombies {
    private Zombies() {}
    private static final float[] P = new float[32];

    /** Skin tone per type (sickly greens / greys). */
    static final int[] SKIN = {0xFF8FA37A, 0xFF9AAE84, 0xFF7E946C, 0xFF8C9C7E, 0xFFA3B08A, 0xFF7F8F68,
            0xFF889878, 0xFF9DB27A, 0xFF84A46E, 0xFF6E7F5C, 0xFFC7D1A8};
    /** Clothing per type. */
    static final int[] CLOTH = {0xFF4E5F7A, 0xFF7A3B3B, 0xFF5B4A3A, 0xFF263238, 0xFF5D4A6A, 0xFF6D4C41,
            0xFF4B5A33, 0xFF8D6E63, 0xFF3E4F3E, 0xFF37474F, 0xFFB0A090};
    static final int BLOOD = 0xFF7A0E12, DARK_BLOOD = 0xFF3D0507, EYE = 0xFFFFE066;

    private static int tint(int c, Enemy e) {
        if (e.flash > 0) return 0xFFFFFFFF;
        if (e.burnT > 0) c = MathUtil.mix(c, 0xFFFF6D00, 0.35f);
        if (e.slowT > 0) c = MathUtil.mix(c, 0xFFB3E5FC, 0.45f);
        if (e.poisonT > 0) c = MathUtil.mix(c, 0xFF76FF03, 0.3f);
        if (e.slagT > 0) c = MathUtil.mix(c, 0xFFAA00FF, 0.4f);
        return c;
    }

    private static void ellipse(Gfx g, float cx, float cy, float rx, float ry, float rot, int col) {
        int n = 12;
        float c = MathUtil.cos(rot), s = MathUtil.sin(rot);
        for (int i = 0; i < n; i++) {
            float a = i * MathUtil.TAU / n;
            float x = MathUtil.cos(a) * rx, y = MathUtil.sin(a) * ry;
            P[i * 2] = cx + x * c - y * s; P[i * 2 + 1] = cy + x * s + y * c;
        }
        g.poly(P, n, col);
    }

    // =================================================================== TOP-DOWN (2D)
    /** Draws a zombie seen from above, facing its movement direction. */
    public static void drawTop(Gfx g, Enemy e, float s) {
        int type = e.type.id;
        float dir = MathUtil.atan2(e.vy, e.vx);
        if (e.type.behavior == EnemyType.B_CHARGE && e.state == 1) dir = MathUtil.atan2(e.dirY, e.dirX);
        float walk = MathUtil.sin(e.t * (type == 1 || type == 4 ? 14f : 7f));
        int skin = tint(SKIN[type], e), cloth = tint(CLOTH[type], e);
        float fx = MathUtil.cos(dir), fy = MathUtil.sin(dir), sx = -fy, sy = fx;   // forward / side
        float x = e.x, y = e.y;
        if (type == 4) { x += MathUtil.sin(e.t * 31f) * s * 0.12f; y += MathUtil.cos(e.t * 27f) * s * 0.12f; }   // twitcher jitter
        g.circle(x + 4, y + 6, s * 1.05f, 0x44000000);   // shadow
        // extra mutated arms (fused tiers)
        if (e.tier > 0) {
            int vein = e.tier == 2 ? 0xFFFF1744 : 0xFFFFAB00;
            for (int k = 0; k < (e.tier == 2 ? 4 : 2); k++) {
                float a = dir + (k % 2 == 0 ? 1 : -1) * (1.2f + 0.35f * (k / 2)) + MathUtil.sin(e.t * 5 + k) * 0.2f;
                float ax = x + MathUtil.cos(a) * s * 1.5f, ay = y + MathUtil.sin(a) * s * 1.5f;
                g.line(x, y, ax, ay, s * 0.22f, skin);
                g.circle(ax, ay, s * 0.16f, vein);
            }
        }
        switch (type) {
            case 2: case 10: { // crawler / spawnling: low, wide, clawing
                ellipse(g, x, y, s * 0.75f, s * 0.95f, dir + MathUtil.PI / 2, cloth);
                for (int k = -1; k <= 1; k += 2) {
                    float reach = s * (0.9f + 0.25f * walk * k);
                    float hx = x + fx * reach + sx * k * s * 0.6f, hy = y + fy * reach + sy * k * s * 0.6f;
                    g.line(x + sx * k * s * 0.4f, y + sy * k * s * 0.4f, hx, hy, s * 0.22f, skin);
                    g.circle(hx, hy, s * 0.14f, skin);
                }
                g.circle(x + fx * s * 0.55f, y + fy * s * 0.55f, s * 0.42f, skin);
                eyes(g, x + fx * s * 0.75f, y + fy * s * 0.75f, sx, sy, s * 0.42f);
                return;
            }
            case 7: { // bloater: huge belly with pustules
                g.circle(x, y, s * 1.0f, skin);
                g.circle(x - sx * s * 0.25f, y - sy * s * 0.25f, s * 0.7f, MathUtil.mix(skin, 0xFF6B8E23, 0.35f));
                for (int k = 0; k < 5; k++) {
                    float a = k * 1.3f + e.t * 0.3f;
                    g.circle(x + MathUtil.cos(a) * s * 0.6f, y + MathUtil.sin(a) * s * 0.6f, s * (0.12f + 0.04f * MathUtil.sin(e.t * 3 + k)), 0xFFC6E05A);
                }
                g.circle(x + fx * s * 0.75f, y + fy * s * 0.75f, s * 0.38f, skin);
                eyes(g, x + fx * s * 0.9f, y + fy * s * 0.9f, sx, sy, s * 0.38f);
                return;
            }
        }
        float bodyW = type == 1 ? 0.55f : (type == 5 || type == 9 ? 1.05f : 0.78f);
        // arms reaching forward (runners swing them back)
        boolean reach = type != 1;
        for (int k = -1; k <= 1; k += 2) {
            float swing = walk * k * 0.25f;
            float shx = x + sx * k * s * bodyW * 0.85f, shy = y + sy * k * s * bodyW * 0.85f;
            float len = (type == 5 && k == 1) ? 1.5f : 1.05f;
            float hx, hy;
            if (reach) { hx = shx + (fx * (1f + swing) + sx * k * 0.1f) * s * len; hy = shy + (fy * (1f + swing) + sy * k * 0.1f) * s * len; }
            else { hx = shx - fx * s * (0.4f + swing) + sx * k * s * 0.2f; hy = shy - fy * s * (0.4f + swing) + sy * k * s * 0.2f; }
            float thick = s * (type == 9 ? 0.34f : (type == 5 && k == 1 ? 0.42f : 0.2f));
            g.line(shx, shy, hx, hy, thick, cloth);
            g.line((shx + hx) / 2, (shy + hy) / 2, hx, hy, thick * 0.9f, skin);
            g.circle(hx, hy, thick * 0.7f, skin);
        }
        // torso / shoulders
        ellipse(g, x, y, s * bodyW, s * 0.5f, dir + MathUtil.PI / 2, cloth);
        if (type == 6 || type == 9) {   // armored vest / plating
            ellipse(g, x, y, s * bodyW * 0.85f, s * 0.38f, dir + MathUtil.PI / 2, type == 6 ? 0xFF556B2F : 0xFF455A64);
            g.line(x + sx * s * bodyW * 0.7f, y + sy * s * bodyW * 0.7f, x - sx * s * bodyW * 0.7f, y - sy * s * bodyW * 0.7f, 3, 0xFF263238);
        }
        // blood stains
        g.circle(x - fx * s * 0.1f + sx * s * 0.25f, y - fy * s * 0.1f + sy * s * 0.25f, s * 0.14f, BLOOD);
        // head
        float hr = s * (type == 9 ? 0.4f : 0.36f);
        float hx = x + fx * s * 0.12f, hy = y + fy * s * 0.12f;
        if (type == 8) { hx += fx * s * 0.25f; hy += fy * s * 0.25f; }   // spitter's long neck
        g.circle(hx, hy, hr, skin);
        if (type == 6) g.circle(hx, hy, hr * 1.05f, 0xFF4B5A33);           // helmet
        if (type == 3) { g.circle(hx, hy, hr * 1.05f, 0xFF212121); g.line(hx + sx * hr, hy + sy * hr, hx - sx * hr, hy - sy * hr, hr * 0.35f, 0xFF90A4AE); }
        if (type == 8) g.circle(hx + fx * hr * 0.7f, hy + fy * hr * 0.7f, hr * 0.45f, 0xFF76FF03);   // glowing spit
        if (e.tier == 2) { g.circle(hx - sx * hr * 1.3f, hy - sy * hr * 1.3f, hr * 0.8f, skin); eyes(g, hx - sx * hr * 1.3f + fx * hr * 0.4f, hy - sy * hr * 1.3f + fy * hr * 0.4f, sx, sy, hr * 0.8f); }
        eyes(g, hx + fx * hr * 0.55f, hy + fy * hr * 0.55f, sx, sy, hr);
        if (type == 3) {   // riot shield in front
            float shx = x + fx * s * 1.05f, shy = y + fy * s * 1.05f;
            g.line(shx + sx * s * 0.8f, shy + sy * s * 0.8f, shx - sx * s * 0.8f, shy - sy * s * 0.8f, s * 0.22f, 0xCC90CAF9);
        }
    }

    private static void eyes(Gfx g, float x, float y, float sx, float sy, float r) {
        g.circle(x + sx * r * 0.35f, y + sy * r * 0.35f, Math.max(1.2f, r * 0.16f), EYE);
        g.circle(x - sx * r * 0.35f, y - sy * r * 0.35f, Math.max(1.2f, r * 0.16f), EYE);
    }

    /** Boss zombies seen from above. */
    public static void drawBossTop(Gfx g, Enemy e, float s) {
        int k = e.bossKind;
        int skin = tint(k == 0 ? 0xFF7B8B5E : (k == 1 ? 0xFF9E8A6E : 0xFF6E6A8A), e);
        float dir = MathUtil.atan2(e.vy, e.vx), t = e.t;
        g.circle(e.x + 8, e.y + 10, s * 1.1f, 0x55000000);
        if (k == 0) {   // abomination: many arms around a huge torso
            for (int i = 0; i < 8; i++) {
                float a = i * MathUtil.TAU / 8 + MathUtil.sin(t * 2 + i) * 0.15f;
                float ax = e.x + MathUtil.cos(a) * s * 1.35f, ay = e.y + MathUtil.sin(a) * s * 1.35f;
                g.line(e.x, e.y, ax, ay, s * 0.2f, skin);
                g.circle(ax, ay, s * 0.13f, BLOOD);
            }
            g.circle(e.x, e.y, s, skin);
            g.circle(e.x, e.y, s * 0.7f, MathUtil.mix(skin, BLOOD, 0.35f));
            for (int i = 0; i < 3; i++) { float a = dir + (i - 1) * 0.6f; g.circle(e.x + MathUtil.cos(a) * s * 0.55f, e.y + MathUtil.sin(a) * s * 0.55f, s * 0.22f, skin); g.circle(e.x + MathUtil.cos(a) * s * 0.65f, e.y + MathUtil.sin(a) * s * 0.65f, s * 0.06f, EYE); }
        } else if (k == 1) {   // brood mother: egg sac
            g.circle(e.x - MathUtil.cos(dir) * s * 0.4f, e.y - MathUtil.sin(dir) * s * 0.4f, s * 0.95f, MathUtil.mix(skin, 0xFFE6EE9C, 0.3f));
            for (int i = 0; i < 9; i++) { float a = i * 0.7f + t * 0.2f, r = s * (0.3f + 0.08f * (i % 3)); g.circle(e.x - MathUtil.cos(dir) * s * 0.4f + MathUtil.cos(a) * r, e.y - MathUtil.sin(dir) * s * 0.4f + MathUtil.sin(a) * r, s * 0.14f, 0xFFF0F4C3); }
            g.circle(e.x + MathUtil.cos(dir) * s * 0.45f, e.y + MathUtil.sin(dir) * s * 0.45f, s * 0.5f, skin);
            eyes(g, e.x + MathUtil.cos(dir) * s * 0.75f, e.y + MathUtil.sin(dir) * s * 0.75f, -MathUtil.sin(dir), MathUtil.cos(dir), s * 0.4f);
        } else {   // stalker: long thin limbs
            for (int i = 0; i < 4; i++) {
                float a = dir + (i < 2 ? 0.7f : 2.4f) * (i % 2 == 0 ? 1 : -1) + MathUtil.sin(t * 8 + i) * 0.25f;
                g.line(e.x, e.y, e.x + MathUtil.cos(a) * s * 1.7f, e.y + MathUtil.sin(a) * s * 1.7f, s * 0.1f, skin);
            }
            ellipse(g, e.x, e.y, s * 0.45f, s * 0.75f, dir, MathUtil.alpha(skin, 0.85f));
            g.circle(e.x + MathUtil.cos(dir) * s * 0.6f, e.y + MathUtil.sin(dir) * s * 0.6f, s * 0.3f, skin);
            eyes(g, e.x + MathUtil.cos(dir) * s * 0.75f, e.y + MathUtil.sin(dir) * s * 0.75f, -MathUtil.sin(dir), MathUtil.cos(dir), s * 0.3f);
        }
    }

    // =================================================================== FRONT VIEW (3D billboards)
    /**
     * Front-facing zombie standing at (x, footY) with total height h. phase drives the shamble animation.
     */
    public static void drawFront(Gfx g, Enemy e, float x, float footY, float h, float phase, float fog) {
        int type = e.boss ? -1 : e.type.id;
        int skin = e.boss ? (e.bossKind == 0 ? 0xFF7B8B5E : (e.bossKind == 1 ? 0xFF9E8A6E : 0xFF6E6A8A)) : SKIN[type];
        int cloth = e.boss ? 0xFF3E2723 : CLOTH[type];
        skin = MathUtil.mix(tint(skin, e), 0xFF05070C, fog);
        cloth = MathUtil.mix(tint(cloth, e), 0xFF05070C, fog);
        int eye = MathUtil.mix(EYE, 0xFF05070C, fog * 0.5f);
        int blood = MathUtil.mix(BLOOD, 0xFF05070C, fog);
        float sway = MathUtil.sin(phase) * h * 0.03f;
        float w = h * (type == 9 || type == 5 || e.boss ? 0.55f : (type == 7 ? 0.62f : (type == 1 ? 0.32f : 0.4f)));
        g.rect(x - w * 0.55f, footY - h * 0.02f, x + w * 0.55f, footY + h * 0.02f, MathUtil.alpha(0xFF000000, 0.4f * (1 - fog)));
        if (type == 2 || type == 10) {   // crawler: low to the ground, arms reaching
            float top = footY - h * 0.45f;
            ellipse(g, x, footY - h * 0.18f, w * 0.9f, h * 0.16f, 0, cloth);
            g.line(x - w * 0.7f, footY - h * 0.15f, x - w * 1.0f + sway, footY, h * 0.06f, skin);
            g.line(x + w * 0.7f, footY - h * 0.15f, x + w * 1.0f - sway, footY, h * 0.06f, skin);
            g.circle(x + sway, top + h * 0.12f, h * 0.13f, skin);
            g.circle(x - h * 0.045f + sway, top + h * 0.1f, h * 0.022f, eye);
            g.circle(x + h * 0.045f + sway, top + h * 0.1f, h * 0.022f, eye);
            return;
        }
        float hipY = footY - h * 0.45f, shY = footY - h * 0.78f, headR = h * (e.boss ? 0.12f : 0.1f);
        // legs (shambling)
        float step = MathUtil.sin(phase) * w * 0.25f;
        g.line(x - w * 0.22f, hipY, x - w * 0.25f + step, footY, w * 0.22f, cloth);
        g.line(x + w * 0.22f, hipY, x + w * 0.25f - step, footY, w * 0.22f, cloth);
        // torso
        if (type == 7) g.circle(x, (hipY + shY) / 2, w * 0.62f, skin);
        P[0] = x - w * 0.5f + sway; P[1] = shY; P[2] = x + w * 0.5f + sway; P[3] = shY;
        P[4] = x + w * 0.38f; P[5] = hipY; P[6] = x - w * 0.38f; P[7] = hipY;
        g.poly(P, 4, type == 7 ? MathUtil.alpha(cloth, 0.6f) : cloth);
        if (type == 6 || type == 9 || e.boss) g.rect(x - w * 0.32f + sway, shY + h * 0.04f, x + w * 0.32f + sway, hipY - h * 0.04f, MathUtil.mix(type == 6 ? 0xFF556B2F : 0xFF455A64, 0xFF05070C, fog));
        g.circle(x + w * 0.15f + sway, shY + h * 0.12f, w * 0.1f, blood);
        // arms raised toward the camera (classic zombie reach) — drawn foreshortened
        float reach = MathUtil.sin(phase * 0.5f) * h * 0.02f;
        for (int k = -1; k <= 1; k += 2) {
            float ax = x + k * w * 0.5f + sway, ay = shY + h * 0.03f;
            float hx = x + k * w * (type == 1 ? 0.75f : 0.42f) + sway, hy = shY + h * (type == 1 ? 0.25f : 0.06f) + reach * k;
            float thick = w * (type == 9 || (type == 5 && k == 1) ? 0.3f : 0.17f);
            g.line(ax, ay, hx, hy, thick, cloth);
            g.circle(hx, hy, thick * 0.75f, skin);
        }
        if (e.tier > 0) {   // mutated: extra limbs with glowing veins
            int vein = MathUtil.mix(e.tier == 2 ? 0xFFFF1744 : 0xFFFFAB00, 0xFF05070C, fog * 0.6f);
            for (int k = -1; k <= 1; k += 2) {
                g.line(x + k * w * 0.4f, (shY + hipY) / 2, x + k * w * 1.05f, (shY + hipY) / 2 - h * 0.1f + MathUtil.sin(phase + k) * h * 0.04f, w * 0.12f, skin);
                g.circle(x + k * w * 1.05f, (shY + hipY) / 2 - h * 0.1f, w * 0.08f, vein);
            }
            g.line(x - w * 0.2f, shY + h * 0.05f, x + w * 0.1f, hipY - h * 0.05f, Math.max(1, w * 0.04f), vein);
        }
        // head
        float hx = x + sway * 1.5f, hy = shY - headR * 0.9f;
        if (type == 8) hy -= headR * 0.6f;
        g.circle(hx, hy, headR, skin);
        if (type == 6) { g.arc(hx, hy, headR * 1.12f, MathUtil.PI, MathUtil.PI, MathUtil.mix(0xFF4B5A33, 0xFF05070C, fog)); }
        if (type == 3) {
            g.circle(hx, hy, headR * 1.1f, MathUtil.mix(0xFF212121, 0xFF05070C, fog));
            g.rect(hx - headR * 0.8f, hy - headR * 0.2f, hx + headR * 0.8f, hy + headR * 0.25f, MathUtil.mix(0xFF90A4AE, 0xFF05070C, fog));
            g.roundRect(x - w * 0.55f, shY + h * 0.02f, x + w * 0.55f, hipY + h * 0.1f, w * 0.08f, MathUtil.alpha(MathUtil.mix(0xFF90CAF9, 0xFF05070C, fog), 0.55f));
        }
        g.circle(hx - headR * 0.38f, hy - headR * 0.1f, headR * 0.17f, eye);
        g.circle(hx + headR * 0.38f, hy - headR * 0.1f, headR * 0.17f, eye);
        g.rect(hx - headR * 0.35f, hy + headR * 0.4f, hx + headR * 0.35f, hy + headR * 0.55f, MathUtil.mix(DARK_BLOOD, 0xFF05070C, fog));
        if (type == 8) g.circle(hx, hy + headR * 0.5f, headR * 0.25f, MathUtil.mix(0xFF76FF03, 0xFF05070C, fog));
        if (e.tier == 2) { g.circle(hx + headR * 1.4f, hy + headR * 0.4f, headR * 0.75f, skin); g.circle(hx + headR * 1.4f, hy + headR * 0.3f, headR * 0.14f, eye); }
        if (e.boss && e.bossKind == 0) for (int i = -2; i <= 2; i++) g.line(x + i * w * 0.2f, shY, x + i * w * 0.45f, shY - h * 0.25f + MathUtil.sin(phase + i) * h * 0.05f, w * 0.08f, skin);
    }

    // =================================================================== survivor (player) top-down
    /** The player: an armed survivor seen from above. Gear grows with upgrades (drawn by WorldRenderer). */
    public static void drawSurvivorTop(Gfx g, float x, float y, float r, float facing, int suit, int skin, boolean moving, float t) {
        float fx = MathUtil.cos(facing), fy = MathUtil.sin(facing), sx = -fy, sy = fx;
        g.circle(x + 4, y + 6, r * 1.05f, 0x44000000);
        float step = moving ? MathUtil.sin(t * 12f) * r * 0.25f : 0;
        // feet
        g.circle(x - fx * r * 0.1f + sx * r * 0.35f + fx * step, y - fy * r * 0.1f + sy * r * 0.35f + fy * step, r * 0.22f, 0xFF263238);
        g.circle(x - fx * r * 0.1f - sx * r * 0.35f - fx * step, y - fy * r * 0.1f - sy * r * 0.35f - fy * step, r * 0.22f, 0xFF263238);
        // backpack
        ellipse(g, x - fx * r * 0.45f, y - fy * r * 0.45f, r * 0.42f, r * 0.3f, facing + MathUtil.PI / 2, 0xFF5D4037);
        // shoulders / torso
        ellipse(g, x, y, r * 0.95f, r * 0.52f, facing + MathUtil.PI / 2, suit);
        // arms forward to the gun
        g.line(x + sx * r * 0.7f, y + sy * r * 0.7f, x + fx * r * 0.9f + sx * r * 0.18f, y + fy * r * 0.9f + sy * r * 0.18f, r * 0.26f, suit);
        g.line(x - sx * r * 0.7f, y - sy * r * 0.7f, x + fx * r * 0.75f - sx * r * 0.05f, y + fy * r * 0.75f - sy * r * 0.05f, r * 0.26f, suit);
        g.circle(x + fx * r * 0.9f + sx * r * 0.18f, y + fy * r * 0.9f + sy * r * 0.18f, r * 0.16f, skin);
        // head with cap
        g.circle(x + fx * r * 0.05f, y + fy * r * 0.05f, r * 0.42f, skin);
        g.arc(x + fx * r * 0.05f, y + fy * r * 0.05f, r * 0.44f, facing + MathUtil.PI / 2, MathUtil.PI, 0xFF33691E);
    }
}
