package com.blockkillers.core;

/** Simple persistent key-value storage. */
public interface Storage {
    int getInt(String key, int def);
    long getLong(String key, long def);
    float getFloat(String key, float def);
    String getString(String key, String def);
    void putInt(String key, int v);
    void putLong(String key, long v);
    void putFloat(String key, float v);
    void putString(String key, String v);
    /** Persist pending writes. */
    void flush();
}
