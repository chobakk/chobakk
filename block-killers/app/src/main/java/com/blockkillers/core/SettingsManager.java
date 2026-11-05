package com.blockkillers.core;

/** User settings, persisted through {@link Storage}. */
public final class SettingsManager {
    public static final int Q_LOW = 0, Q_MEDIUM = 1, Q_HIGH = 2;
    public static final int LANG_EN = 0, LANG_FA = 1;

    private final Storage st;
    public float musicVolume, sfxVolume, sensitivity;
    public boolean vibration, screenShake, batterySaver, showDamageNumbers;
    public int quality, language;

    public SettingsManager(Storage st) { this.st = st; load(); }

    public void load() {
        musicVolume = st.getFloat("set_music", 0.6f);
        sfxVolume = st.getFloat("set_sfx", 0.8f);
        sensitivity = st.getFloat("set_sens", 1.0f);
        vibration = st.getInt("set_vib", 1) == 1;
        screenShake = st.getInt("set_shake", 1) == 1;
        batterySaver = st.getInt("set_battery", 0) == 1;
        showDamageNumbers = st.getInt("set_dmgnum", 1) == 1;
        quality = MathUtil.clamp(st.getInt("set_quality", Q_HIGH), 0, 2);
        language = MathUtil.clamp(st.getInt("set_lang", LANG_EN), 0, 1);
    }

    public void save() {
        st.putFloat("set_music", musicVolume);
        st.putFloat("set_sfx", sfxVolume);
        st.putFloat("set_sens", sensitivity);
        st.putInt("set_vib", vibration ? 1 : 0);
        st.putInt("set_shake", screenShake ? 1 : 0);
        st.putInt("set_battery", batterySaver ? 1 : 0);
        st.putInt("set_dmgnum", showDamageNumbers ? 1 : 0);
        st.putInt("set_quality", quality);
        st.putInt("set_lang", language);
        st.flush();
    }

    /** Particle budget for the current quality level. */
    public int particleBudget() { return quality == Q_LOW ? 250 : (quality == Q_MEDIUM ? 600 : 1100); }
}
