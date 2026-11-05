package com.blockkillers.desktop;

import com.blockkillers.core.Audio;
import com.blockkillers.core.Platform;
import com.blockkillers.core.Sfx;
import com.blockkillers.core.Storage;

import java.util.HashMap;

/** In-memory platform for headless tests: records sounds and vibrations instead of playing them. */
public final class DesktopPlatform implements Platform {
    public final MemStorage storage;
    public final RecAudio audio = new RecAudio();
    public int vibrations;

    public DesktopPlatform(MemStorage st) { storage = st; }

    @Override public Storage storage() { return storage; }
    @Override public Audio audio() { return audio; }
    @Override public void vibrate(int ms) { vibrations++; }
    @Override public boolean canExit() { return true; }
    @Override public void exitApp() { }
    /** Tests queue answers for text prompts here. */
    public final java.util.ArrayDeque<String> promptAnswers = new java.util.ArrayDeque<>();
    public String lastPromptTitle;
    @Override public void promptText(String title, String initial, TextCallback cb) {
        lastPromptTitle = title;
        String a = promptAnswers.poll();
        if (a != null) cb.done(a);
    }
    public boolean networkActive;
    public static java.io.File assetDir = new java.io.File("app/src/main/assets");
    private static final java.util.HashMap<String, Object> IMG = new java.util.HashMap<>();
    @Override public Object loadImage(String path) {
        synchronized (IMG) {
            if (IMG.containsKey(path)) return IMG.get(path);
            Object o = null;
            try { java.awt.image.BufferedImage b = javax.imageio.ImageIO.read(new java.io.File(assetDir, path));
                if (b != null) { java.awt.image.BufferedImage c = new java.awt.image.BufferedImage(b.getWidth(), b.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    c.getGraphics().drawImage(b, 0, 0, null); o = c; } } catch (Exception ignored) {}
            IMG.put(path, o);
            return o;
        }
    }
    @Override public String loadText(String path) {
        try { return new String(java.nio.file.Files.readAllBytes(new java.io.File(assetDir, path).toPath()), "UTF-8"); } catch (Exception e) { return null; }
    }
    @Override public void setNetworkActive(boolean active) { networkActive = active; }

    public static final class MemStorage implements Storage {
        public final HashMap<String, Object> committed = new HashMap<>();
        private final HashMap<String, Object> pending = new HashMap<>();
        public int flushes;
        private Object v(String k) { return pending.containsKey(k) ? pending.get(k) : committed.get(k); }
        @Override public int getInt(String k, int d) { Object o = v(k); return o instanceof Integer ? (Integer) o : d; }
        @Override public long getLong(String k, long d) { Object o = v(k); return o instanceof Long ? (Long) o : d; }
        @Override public float getFloat(String k, float d) { Object o = v(k); return o instanceof Float ? (Float) o : d; }
        @Override public String getString(String k, String d) { Object o = v(k); return o instanceof String ? (String) o : d; }
        @Override public void putInt(String k, int x) { pending.put(k, x); }
        @Override public void putLong(String k, long x) { pending.put(k, x); }
        @Override public void putFloat(String k, float x) { pending.put(k, x); }
        @Override public void putString(String k, String x) { pending.put(k, x); }
        @Override public void flush() { committed.putAll(pending); pending.clear(); flushes++; }
        /** Simulates killing the process: unflushed data is lost. */
        public MemStorage reopen() { MemStorage m = new MemStorage(); m.committed.putAll(committed); return m; }
    }

    public static final class RecAudio implements Audio {
        public final int[] plays = new int[Sfx.COUNT];
        public boolean music;
        public float musicVolume;
        @Override public void play(int sfx, float volume) { plays[sfx]++; }
        @Override public void startMusic() { music = true; }
        @Override public void stopMusic() { music = false; }
        @Override public void setMusicVolume(float v) { musicVolume = v; }
        @Override public void release() { }
    }
}
