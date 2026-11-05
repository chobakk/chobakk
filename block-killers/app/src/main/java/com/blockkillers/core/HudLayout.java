package com.blockkillers.core;

/** User-editable HUD layout: per-element offset and scale, saved in storage (Settings → Edit control layout). */
public final class HudLayout {
    public static final int HOTBAR = 0, NOVA = 1, PICKUP = 2, PAUSE = 3, MINIMAP = 4;
    public static final int COUNT = 5;
    public static final String[] KEYS = {"lay_hotbar", "lay_nova", "lay_pickup", "lay_pause", "lay_minimap"};
    public final float[] dx = new float[COUNT], dy = new float[COUNT], scale = new float[COUNT];
    private final Storage st;

    public HudLayout(Storage st) {
        this.st = st;
        for (int i = 0; i < COUNT; i++) {
            dx[i] = st.getFloat("lay" + i + "_dx", 0); dy[i] = st.getFloat("lay" + i + "_dy", 0);
            scale[i] = MathUtil.clamp(st.getFloat("lay" + i + "_s", 1f), 0.6f, 1.6f);
        }
    }

    public void save() {
        for (int i = 0; i < COUNT; i++) { st.putFloat("lay" + i + "_dx", dx[i]); st.putFloat("lay" + i + "_dy", dy[i]); st.putFloat("lay" + i + "_s", scale[i]); }
        st.flush();
    }

    public void reset() { for (int i = 0; i < COUNT; i++) { dx[i] = dy[i] = 0; scale[i] = 1f; } save(); }

    /** Moves/scales a button around the pivot (group centre) for element el. */
    public void apply(UIManager.Button b, int el, float pivotX, float pivotY, float W, float H) {
        float s = scale[el];
        float l = pivotX + (b.l - pivotX) * s + dx[el], r = pivotX + (b.r - pivotX) * s + dx[el];
        float t = pivotY + (b.t - pivotY) * s + dy[el], bt = pivotY + (b.b - pivotY) * s + dy[el];
        b.set(l, t, r, bt);
    }
}
