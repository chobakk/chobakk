package com.blockkillers.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.blockkillers.core.Storage;

/** {@link Storage} backed by SharedPreferences (survives app restarts; offline). */
final class AndroidStorage implements Storage {
    private final SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    AndroidStorage(Context ctx) { prefs = ctx.getSharedPreferences("block_killers_save", Context.MODE_PRIVATE); }

    private SharedPreferences.Editor ed() { if (editor == null) editor = prefs.edit(); return editor; }

    @Override public int getInt(String k, int d) { try { return prefs.getInt(k, d); } catch (ClassCastException e) { return d; } }
    @Override public long getLong(String k, long d) { try { return prefs.getLong(k, d); } catch (ClassCastException e) { return d; } }
    @Override public float getFloat(String k, float d) { try { return prefs.getFloat(k, d); } catch (ClassCastException e) { return d; } }
    @Override public String getString(String k, String d) { try { return prefs.getString(k, d); } catch (ClassCastException e) { return d; } }
    @Override public void putInt(String k, int v) { ed().putInt(k, v); }
    @Override public void putLong(String k, long v) { ed().putLong(k, v); }
    @Override public void putFloat(String k, float v) { ed().putFloat(k, v); }
    @Override public void putString(String k, String v) { ed().putString(k, v); }
    @Override public void flush() { if (editor != null) { editor.apply(); editor = null; } }
}
