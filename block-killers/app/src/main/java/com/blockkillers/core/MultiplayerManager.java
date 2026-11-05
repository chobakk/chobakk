package com.blockkillers.core;

import com.blockkillers.core.net.Discovery;
import com.blockkillers.core.net.NetClient;
import com.blockkillers.core.net.NetHost;
import com.blockkillers.core.net.NetSession;
import com.blockkillers.core.net.NetSync;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * Multiplayer flow: same-phone split screen, Wi-Fi host/join (with discovery), and online rooms via the relay.
 * The host simulates everything; clients send inputs/actions and render the host's snapshots.
 */
public final class MultiplayerManager {
    public static final float SNAP_RATE = 1f / 20f, INPUT_RATE = 1f / 30f;
    private final Game g;
    public final Discovery discovery = new Discovery();
    public int lobbyMode = Game.M_COOP;
    public final String[] names = new String[4];
    /** link id per hero slot (host); -1 = local / empty. */
    public final int[] slotLink = {-1, -1, -1, -1};
    public int lobbyCount = 1;
    public boolean online;
    private float snapTimer, inputTimer;
    public String lastError;

    public MultiplayerManager(Game g) { this.g = g; }

    public String myName() { return g.save.playerName; }

    // ------------------------------------------------------------------ entering multiplayer
    public void startSplit(int mode) {
        closeNet();
        g.mode = mode;
        g.splitScreen = true;
        g.heroCount = 2;
        g.localHero = 0;
        for (int i = 0; i < 4; i++) { Hero h = g.heroes[i]; h.active = i < 2; h.local = i < 2; h.link = -1; h.name = "P" + (i + 1); }
        g.heroes[0].region = Hero.REGION_LEFT; g.heroes[1].region = Hero.REGION_RIGHT;
        g.startRun();
    }

    public void hostLan() { host(false, null, NetHost.LAN_PORT); }
    /** Test/advanced: host Wi-Fi on a specific port (0 = any free port). */
    public void hostLan(int port) { host(false, null, port); }
    public void hostOnline(String server, int port) { host(true, server, port); }

    private void host(boolean viaRelay, String server, int port) {
        closeNet();
        NetHost h = new NetHost();
        h.beaconName = myName();
        if (viaRelay) h.startRelay(server, port); else h.startLan(port);
        g.net = h;
        online = viaRelay;
        lobbyCount = 1;
        names[0] = myName();
        for (int i = 1; i < 4; i++) { names[i] = null; slotLink[i] = -1; }
        slotLink[0] = -1;
        g.platform.setNetworkActive(true);
        g.state = Game.S_LOBBY;
    }

    public void joinLan(String ip, int port) { join(false, ip, port, null); }
    public void joinOnline(String server, int port, String code) { join(true, server, port, code); }

    private void join(boolean viaRelay, String host, int port, String code) {
        closeNet();
        NetClient c = new NetClient();
        if (viaRelay) c.connectRelay(host, port, code); else c.connectLan(host, port);
        g.net = c;
        online = viaRelay;
        helloSent = false;
        myIndex = -1;
        g.platform.setNetworkActive(true);
        g.state = Game.S_LOBBY;
    }

    public void startDiscovery() { g.platform.setNetworkActive(true); discovery.start(); }

    // ------------------------------------------------------------------ host: start the match
    public boolean isHost() { return g.net instanceof NetHost; }
    public boolean isClient() { return g.net instanceof NetClient; }

    public void hostStart() {
        if (!isHost()) return;
        NetHost h = (NetHost) g.net;
        long seed = MathUtil.RNG.nextLong();
        h.setSeed(seed);
        g.mode = lobbyMode;
        g.splitScreen = false;
        g.localHero = 0;
        int count = 0;
        int[] remap = new int[4];
        for (int i = 0; i < 4; i++) if (i == 0 || slotLink[i] >= 0) remap[count++] = i;
        String[] nm = new String[4];
        for (int i = 0; i < 4; i++) {
            Hero hero = g.heroes[i];
            hero.active = i < count; hero.local = i == 0; hero.region = Hero.REGION_FULL;
            hero.link = i == 0 ? -1 : slotLink[remap[i]];
            hero.name = i < count ? names[remap[i]] : "P" + (i + 1);
            nm[i] = hero.name;
        }
        g.heroCount = count;
        for (int i = 1; i < count; i++) h.send(g.heroes[i].link, NetSync.start(seed, g.save.selectedMap, g.mode, count, i, nm));
        g.startRun();
    }

    // ------------------------------------------------------------------ per frame
    private boolean helloSent;
    public int myIndex = -1;
    public int lobbyMap;

    public void update(float dt) {
        if (g.net == null) return;
        if (isHost()) hostUpdate(dt); else clientUpdate(dt);
    }

    private void hostUpdate(float dt) {
        NetHost h = (NetHost) g.net;
        h.beaconPlayers = lobbyCount;
        h.beaconMode = g.strings.get(Game.MODE_KEYS[lobbyMode]);
        g.netEvents.recording = g.state == Game.S_PLAYING;
        NetSession.Msg m;
        boolean lobbyChanged = false;
        while ((m = h.poll()) != null) {
            if (m.kind == NetSession.EV_LEAVE) {
                for (int i = 1; i < 4; i++) if (slotLink[i] == m.link) { slotLink[i] = -1; names[i] = null; lobbyChanged = true; }
                for (int i = 1; i < g.heroCount; i++) if (g.heroes[i].link == m.link && g.state != Game.S_LOBBY) {
                    g.heroes[i].active = false; g.ui.announce(g.heroes[i].name + " " + g.strings.get("left_game"), null);
                }
                continue;
            }
            if (m.kind != NetSession.EV_FRAME || m.data.length == 0) continue;
            byte type = m.data[0];
            if (type == NetSync.HELLO) {
                int slot = -1;
                for (int i = 1; i < 4; i++) if (slotLink[i] < 0) { slot = i; break; }
                if (slot < 0 || g.state != Game.S_LOBBY) { h.send(m.link, new byte[]{NetSync.FULL}); continue; }
                slotLink[slot] = m.link;
                try { names[slot] = new DataInputStream(new ByteArrayInputStream(m.data, 1, m.data.length - 1)).readUTF(); }
                catch (Exception e) { names[slot] = "P" + (slot + 1); }
                h.send(m.link, NetSync.welcome(slot));
                lobbyChanged = true;
                g.audio.play(Sfx.SELECT);
            } else {
                Hero hero = null;
                for (int i = 1; i < g.heroCount; i++) if (g.heroes[i].link == m.link && g.heroes[i].active) hero = g.heroes[i];
                if (hero == null) continue;
                if (type == NetSync.INPUT && m.data.length >= 3) { hero.netX = m.data[1] / 127f; hero.netY = m.data[2] / 127f; }
                else if (type == NetSync.ACTION && m.data.length >= 3) applyAction(hero, m.data[1], m.data[2]);
            }
        }
        lobbyCount = 0;
        for (int i = 0; i < 4; i++) if (i == 0 || slotLink[i] >= 0) lobbyCount++;
        if (g.state == Game.S_LOBBY) {
            snapTimer -= dt;
            if (lobbyChanged || snapTimer <= 0) { snapTimer = 0.5f; h.broadcast(NetSync.lobby(lobbyMode, g.save.selectedMap, names, 4)); }
            return;
        }
        if (g.state == Game.S_PLAYING || g.state == Game.S_MPRESULT) {
            snapTimer -= dt;
            if (snapTimer <= 0) {
                snapTimer = SNAP_RATE;
                for (int i = 1; i < g.heroCount; i++) {
                    Hero hero = g.heroes[i];
                    if (hero.active && hero.link >= 0) h.send(hero.link, NetSync.snapshot(g, hero));
                }
                g.netEvents.clear();
            }
        }
    }

    /** Host applies a discrete action from a remote (or local split) player. */
    public void applyAction(Hero hero, int kind, int arg) {
        switch (kind) {
            case NetSync.A_SLOT: hero.weapons.select(arg); break;
            case NetSync.A_CARD: g.chooseCard(hero, arg); break;
            case NetSync.A_EQUIP: g.loot.equipFor(hero, arg); break;
            case NetSync.A_SCRAP: g.loot.scrapFor(hero); break;
            case NetSync.A_ABILITY: { Hero prev = g.hero; g.bind(hero); g.playerCtl.useAbility(); g.bind(prev); break; }
        }
    }

    private void clientUpdate(float dt) {
        NetClient c = (NetClient) g.net;
        if (c.failed) { lastError = c.status; leave(); return; }
        if (c.connected() && !helloSent) { c.send(NetSync.hello(myName())); helloSent = true; }
        NetSession.Msg m;
        byte[] latest = null;
        while ((m = c.poll()) != null) {
            if (m.kind == NetSession.EV_LEAVE) { lastError = g.strings.get("host_left"); leave(); return; }
            if (m.data == null || m.data.length == 0) continue;
            switch (m.data[0]) {
                case NetSync.WELCOME: myIndex = m.data[1]; break;
                case NetSync.FULL: lastError = g.strings.get("room_full"); leave(); return;
                case NetSync.LOBBY: readLobby(m.data); break;
                case NetSync.START: clientStart(m.data); break;
                case NetSync.SNAP:
                    // events must not be lost: apply every snapshot (cheap) in order
                    try { NetSync.apply(g, m.data); } catch (Exception e) { /* ignore a bad frame */ }
                    latest = m.data;
                    break;
            }
        }
        if (latest != null) {
            if (g.netHostState == Game.S_MPRESULT && g.state == Game.S_PLAYING) { g.state = Game.S_MPRESULT; g.gameOverTimer = 0; }
        }
        if (g.state == Game.S_PLAYING) {
            inputTimer -= dt;
            if (inputTimer <= 0) {
                inputTimer = INPUT_RATE;
                Joystick j = g.heroes[g.localHero].joystick;
                float ix = j.outX, iy = j.outY;
                if (g.view3D()) { float h = g.heroes[g.localHero].player.view, fwd = -iy, str = ix; ix = MathUtil.cos(h) * fwd - MathUtil.sin(h) * str; iy = MathUtil.sin(h) * fwd + MathUtil.cos(h) * str; }
                c.send(NetSync.input(ix, iy));
            }
        }
    }

    private void readLobby(byte[] d) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(d, 1, d.length - 1));
            lobbyMode = in.readUnsignedByte(); lobbyMap = in.readUnsignedByte();
            int n = in.readUnsignedByte();
            lobbyCount = 0;
            for (int i = 0; i < n; i++) { String s = in.readUTF(); names[i] = s.isEmpty() ? null : s; if (names[i] != null) lobbyCount++; }
        } catch (Exception ignored) {}
    }

    private void clientStart(byte[] d) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(d, 1, d.length - 1));
            long seed = in.readLong();
            int map = in.readUnsignedByte(), mode = in.readUnsignedByte(), count = in.readUnsignedByte(), you = in.readUnsignedByte();
            g.mode = mode; g.splitScreen = false; g.localHero = you; g.heroCount = count;
            for (int i = 0; i < 4; i++) {
                Hero h = g.heroes[i];
                h.active = i < count; h.local = i == you; h.region = Hero.REGION_FULL; h.link = -1;
                h.name = i < count ? in.readUTF() : "P" + (i + 1);
                h.resetMatchStats();
                h.player.reset(new Player.PermanentBonus());
                h.weapons.reset(); h.pets.count = 0; h.joystick.reset(); h.cardsOpen = false;
            }
            g.net.setSeed(seed);
            g.map.generate(seed, map);
            g.enemies.clear(); g.combat.clear(); g.fx.clear(); g.loot.reset();
            g.time = 0; g.deathSlowmo = 0; g.bindLocal();
            g.camera.snap(g.player.x, g.player.y);
            g.runActive = true;
            g.state = Game.S_PLAYING;
            g.audio.musicOn();
        } catch (Exception e) { lastError = "bad start"; leave(); }
    }

    /** Client-side frame: no simulation, only local effects and camera. */
    public void clientFrame(float dt) {
        Hero me = g.heroes[g.localHero];
        g.bindLocal();
        g.fx.update(dt);
        for (int i = 0; i < g.enemies.list.size; i++) g.enemies.list.get(i).t += dt;
        g.map.reveal(me.player.x, me.player.y, 750f);
        g.camera.targetZoom = Config.zoomFor(me.player.level, g.time);
        g.camera.update(dt, me.player.x, me.player.y, g.settings.screenShake);
    }

    // ------------------------------------------------------------------ actions from this device's UI
    public void action(Hero h, byte kind, int arg) {
        if (isClient()) ((NetClient) g.net).send(NetSync.action(kind, arg));
        else applyAction(h, kind, arg);
    }

    public void leave() {
        closeNet();
        g.mode = Game.M_SOLO;
        g.splitScreen = false;
        g.heroCount = 1;
        g.localHero = 0;
        for (int i = 0; i < 4; i++) { Hero h = g.heroes[i]; h.active = i == 0; h.local = i == 0; h.region = Hero.REGION_FULL; h.link = -1; h.downed = false; }
        g.bindLocal();
        g.runActive = false;
        g.state = Game.S_MENU;
    }

    private void closeNet() {
        if (g.net != null) { g.net.close(); g.net = null; }
        discovery.stop();
        g.netEvents.recording = false; g.netEvents.clear();
        g.platform.setNetworkActive(false);
    }
}
