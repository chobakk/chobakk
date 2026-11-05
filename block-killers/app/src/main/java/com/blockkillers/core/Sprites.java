package com.blockkillers.core;

/**
 * Pre-rendered CC0 3D models (Kenney Animated Characters, Kenney Blaster Kit, Quaternius Low Poly Guns,
 * OpenGameArt "Darsh"), rendered offline with three.js into sprite sheets (see tools/render-models).
 * Character sheet: 8 columns (walk frames) x 9 rows (rows 0-7 = facing 0..315 deg relative to the viewer,
 * row 8 = top-down view facing screen-up). Gun atlas: per row a side view and a top view; plus first-person images.
 */
public final class Sprites {
    public static final String[] GUN_NAMES = {"bl_d", "bl_f", "bl_g", "bl_h", "bl_i", "bl_k", "bl_m", "bl_o", "bl_p", "bl_q", "bl_r", "p90", "pistol", "rifle", "shotgun", "sniper"};
    /** Gun model for each weapon type (index = Config.WEAPONS id). */
    public static final String[] WEAPON_MODEL = {"pistol", "p90", "shotgun", "rifle", "bl_q", "bl_i", "bl_f", "bl_o", "shotgun", "sniper", "bl_p", "bl_h", "bl_g", "bl_m", "bl_r", "bl_d", "bl_k"};

    public final Object[] zombies = new Object[11], survivors = new Object[4], bosses = new Object[3];
    public int zcw = 96, zch = 120, bcw = 128, bch = 150;
    public Object guns;
    public final Object[] fp = new Object[GUN_NAMES.length];
    public final float[][] fpMuzzle = new float[GUN_NAMES.length][2];
    public final int[] gunRow = new int[GUN_NAMES.length];
    public boolean ok;

    public Sprites(Platform p) {
        try {
            for (int i = 0; i < zombies.length; i++) zombies[i] = p.loadImage("sprites/z" + i + ".png");
            for (int i = 0; i < survivors.length; i++) survivors[i] = p.loadImage("sprites/s" + i + ".png");
            for (int i = 0; i < bosses.length; i++) bosses[i] = p.loadImage("sprites/b" + i + ".png");
            guns = p.loadImage("sprites/guns.png");
            String json = p.loadText("sprites/sprites.json");
            for (int i = 0; i < GUN_NAMES.length; i++) {
                fp[i] = p.loadImage("sprites/fp_" + GUN_NAMES[i] + ".png");
                gunRow[i] = i;
                if (json != null) {
                    int k = json.indexOf("\"" + GUN_NAMES[i] + "\"");
                    if (k >= 0) {
                        gunRow[i] = intAfter(json, "\"row\":", k);
                        int m = json.indexOf("\"fpMuzzle\":", k);
                        if (m >= 0) { int a = json.indexOf('[', m), c = json.indexOf(',', a), e = json.indexOf(']', c);
                            fpMuzzle[i][0] = Float.parseFloat(json.substring(a + 1, c).trim()); fpMuzzle[i][1] = Float.parseFloat(json.substring(c + 1, e).trim()); }
                    }
                }
            }
            ok = zombies[0] != null && guns != null;
        } catch (Exception e) { ok = false; }
    }

    private static int intAfter(String s, String key, int from) {
        int k = s.indexOf(key, from) + key.length();
        int e = k; while (e < s.length() && (Character.isDigit(s.charAt(e)) || s.charAt(e) == ' ')) e++;
        return Integer.parseInt(s.substring(k, e).trim());
    }

    public int gunIndex(int weaponType) {
        String n = WEAPON_MODEL[weaponType];
        for (int i = 0; i < GUN_NAMES.length; i++) if (GUN_NAMES[i].equals(n)) return i;
        return 12;
    }
}
