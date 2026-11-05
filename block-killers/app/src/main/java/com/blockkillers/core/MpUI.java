package com.blockkillers.core;

import com.blockkillers.core.net.Discovery;
import com.blockkillers.core.net.NetHost;
import com.blockkillers.core.net.NetSync;

import java.util.ArrayList;

/** Multiplayer screens (menu, lobby, results) and in-match multiplayer controls (cards, loot, split-screen HUD). */
final class MpUI {
    // actions
    static final int A_MODE = 100, A_MAPC = 101, A_SPLIT = 102, A_HOST_LAN = 103, A_FOUND0 = 104, A_JOIN_IP = 107, A_HOST_ONLINE = 108,
            A_JOIN_ONLINE = 109, A_SERVER = 110, A_NAME = 111, A_MP_BACK = 112, A_LOBBY_START = 120, A_LOBBY_LEAVE = 121,
            A_LOBBY_MODE = 122, A_LOBBY_MAP = 123, A_RES_MENU = 130, A_RES_AGAIN = 131, A_MP_PAUSE_LEAVE = 140, A_MP_PAUSE_RESUME = 141,
            A_HERO = 200; // + hero*20 + {0..4 slots, 10 nova, 11 equip, 12 scrap, 13..15 cards}
    static final int H_NOVA = 10, H_EQUIP = 11, H_SCRAP = 12, H_CARD = 13;

    private final Game g;
    private final UIManager ui;
    final ArrayList<UIManager.Button> menuB = new ArrayList<>(), lobbyB = new ArrayList<>(), resultB = new ArrayList<>(),
            gameB = new ArrayList<>(), pauseB = new ArrayList<>();
    boolean pauseOpen;
    private final ArrayList<Discovery.Found> found = new ArrayList<>();

    MpUI(Game g, UIManager ui) {
        this.g = g; this.ui = ui;
        String[] mk = {"mp_mode", "mp_map", "mp_split", "mp_host_lan", "", "", "", "mp_join_ip", "mp_host_online", "mp_join_online", "mp_server", "mp_name", "back"};
        int[] ma = {A_MODE, A_MAPC, A_SPLIT, A_HOST_LAN, A_FOUND0, A_FOUND0 + 1, A_FOUND0 + 2, A_JOIN_IP, A_HOST_ONLINE, A_JOIN_ONLINE, A_SERVER, A_NAME, A_MP_BACK};
        for (int i = 0; i < mk.length; i++) menuB.add(new UIManager.Button(ma[i], i < 2 || i == 10 || i == 11 ? UIManager.B_CYCLE : UIManager.B_BUTTON, mk[i],
                i == 12 ? UIManager.PINK : (i == 2 ? UIManager.GOLD : (i < 2 ? UIManager.NEON : 0xFF69F0AE))));
        lobbyB.add(new UIManager.Button(A_LOBBY_MODE, UIManager.B_CYCLE, "mp_mode", UIManager.NEON));
        lobbyB.add(new UIManager.Button(A_LOBBY_MAP, UIManager.B_CYCLE, "mp_map", UIManager.NEON));
        lobbyB.add(new UIManager.Button(A_LOBBY_START, UIManager.B_BUTTON, "mp_start", UIManager.GOLD));
        lobbyB.add(new UIManager.Button(A_LOBBY_LEAVE, UIManager.B_BUTTON, "leave", UIManager.PINK));
        resultB.add(new UIManager.Button(A_RES_AGAIN, UIManager.B_BUTTON, "mp_again", UIManager.NEON));
        resultB.add(new UIManager.Button(A_RES_MENU, UIManager.B_BUTTON, "main_menu", UIManager.PINK));
        pauseB.add(new UIManager.Button(A_MP_PAUSE_RESUME, UIManager.B_BUTTON, "resume", UIManager.NEON));
        pauseB.add(new UIManager.Button(A_MP_PAUSE_LEAVE, UIManager.B_BUTTON, "mp_leave_match", UIManager.PINK));
        for (int h = 0; h < 2; h++) {
            for (int s = 0; s < Config.HOTBAR; s++) gameB.add(new UIManager.Button(A_HERO + h * 20 + s, UIManager.B_ICON, "", 0));
            gameB.add(new UIManager.Button(A_HERO + h * 20 + H_NOVA, UIManager.B_ICON, "nova", 0));
            gameB.add(new UIManager.Button(A_HERO + h * 20 + H_EQUIP, UIManager.B_BUTTON, "equip", UIManager.NEON));
            gameB.add(new UIManager.Button(A_HERO + h * 20 + H_SCRAP, UIManager.B_BUTTON, "scrap", UIManager.GOLD));
            for (int c = 0; c < 3; c++) gameB.add(new UIManager.Button(A_HERO + h * 20 + H_CARD + c, UIManager.B_CARD, "", 0));
        }
    }

    // per-hero control lookup
    UIManager.Button ctl(int hero, int what) {
        int idx = what < Config.HOTBAR ? what : (what == H_NOVA ? 5 : (what == H_EQUIP ? 6 : (what == H_SCRAP ? 7 : 8 + (what - H_CARD))));
        return gameB.get(hero * 11 + idx);
    }

    void layout(float W, float H) {
        float row = Math.min(76, (H - 260) / menuB.size());
        for (int i = 0; i < menuB.size(); i++) menuB.get(i).set(40, 150 + i * row, W - 40, 150 + i * row + row - 8);
        menuB.get(menuB.size() - 1).set(W / 2 - 180, H - 110, W / 2 + 180, H - 40);
        lobbyB.get(0).set(40, H - 420, W - 40, H - 350);
        lobbyB.get(1).set(40, H - 340, W - 40, H - 270);
        lobbyB.get(2).set(40, H - 250, W - 40, H - 160);
        lobbyB.get(3).set(W / 2 - 180, H - 130, W / 2 + 180, H - 50);
        resultB.get(0).set(W / 2 - 230, H - 230, W / 2 + 230, H - 140);
        resultB.get(1).set(W / 2 - 230, H - 120, W / 2 + 230, H - 30);
        pauseB.get(0).set(W / 2 - 230, H * 0.45f, W / 2 + 230, H * 0.45f + 90);
        pauseB.get(1).set(W / 2 - 230, H * 0.45f + 110, W / 2 + 230, H * 0.45f + 200);
        layoutGame(W, H);
    }

    private void layoutGame(float W, float H) {
        boolean split = g.splitScreen;
        for (int h = 0; h < 2; h++) {
            float l = split ? (h == 0 ? 0 : W / 2) : 0, r = split ? l + W / 2 : W;
            float rw = r - l;
            if (split) {
                float ss = 56, gap = 5, x0 = l + (rw - (ss * 5 + gap * 4)) / 2;
                for (int s = 0; s < Config.HOTBAR; s++) ctl(h, s).set(x0 + s * (ss + gap), H - 96, x0 + s * (ss + gap) + ss, H - 40);
                float nx = h == 0 ? l + 70 : r - 70;
                ctl(h, H_NOVA).set(nx - 46, H - 210, nx + 46, H - 118);
                ctl(h, H_EQUIP).set(l + rw / 2 - 150 + 20, H - 280, l + rw / 2 - 5, H - 220);
                ctl(h, H_SCRAP).set(l + rw / 2 + 5, H - 280, l + rw / 2 + 150 - 20, H - 220);
            } else {
                for (int s = 0; s < Config.HOTBAR; s++) ctl(h, s).set(-100, -100, -99, -99);   // uses the solo hotbar
                ctl(h, H_NOVA).set(-100, -100, -99, -99);
                ctl(h, H_EQUIP).set(W / 2 - 200, H - 255, W / 2 - 10, H - 175);
                ctl(h, H_SCRAP).set(W / 2 + 10, H - 255, W / 2 + 200, H - 175);
            }
            float ct = split ? 230 : 300, ch = split ? 64 : 74;
            for (int c = 0; c < 3; c++) ctl(h, H_CARD + c).set(l + 12, ct + c * (ch + 8), r - 12, ct + c * (ch + 8) + ch);
        }
    }

    ArrayList<UIManager.Button> buttons() {
        switch (g.state) {
            case Game.S_MPMENU: return menuB;
            case Game.S_LOBBY: return lobbyB;
            case Game.S_MPRESULT: return resultB;
            default: return null;
        }
    }

    // ------------------------------------------------------------------ in-match touch handling
    /** Returns true if the touch hit a multiplayer control. */
    boolean touchGame(int id, float x, float y) {
        if (pauseOpen) {
            for (UIManager.Button b : pauseB) if (b.hit(x, y)) { ui.pressed = b; return true; }
            return true;
        }
        layoutGame(g.viewW, g.viewH);
        for (int h = 0; h < localCount(); h++) {
            Hero hero = localHero(h);
            for (int k = 0; k < 11; k++) {
                UIManager.Button b = gameB.get(h * 11 + k);
                if (!visible(hero, k) || !b.hit(x, y)) continue;
                int what = (b.action - A_HERO) % 20;
                if (what < Config.HOTBAR) g.mpm.action(hero, NetSync.A_SLOT, what);
                else if (what == H_NOVA) g.mpm.action(hero, NetSync.A_ABILITY, 0);
                else if (what == H_EQUIP) g.mpm.action(hero, NetSync.A_EQUIP, -1);
                else if (what == H_SCRAP) g.mpm.action(hero, NetSync.A_SCRAP, 0);
                else { g.mpm.action(hero, NetSync.A_CARD, what - H_CARD); if (g.isClient()) hero.cardsOpen = false; }
                g.audio.play(Sfx.CLICK);
                return true;
            }
        }
        return false;
    }

    private int localCount() { return g.splitScreen ? 2 : 1; }
    private Hero localHero(int i) { return g.splitScreen ? g.heroes[i] : g.heroes[g.localHero]; }

    private boolean visible(Hero hero, int k) {
        if (k < Config.HOTBAR || k == 5) return g.splitScreen;
        if (k == 6 || k == 7) return hero.nearby != null && hero.canAct();
        int c = k - 8;
        return hero.cardsOpen && c < hero.upgrades.cardCount && hero.canAct();
    }

    void touchUpGame(UIManager.Button b) {
        if (b.action == A_MP_PAUSE_RESUME) pauseOpen = false;
        else if (b.action == A_MP_PAUSE_LEAVE) { pauseOpen = false; g.toMenu(); }
    }

    // ------------------------------------------------------------------ screen actions
    boolean action(int a) {
        MultiplayerManager m = g.mpm;
        switch (a) {
            case A_MODE: case A_LOBBY_MODE:
                if (g.mpm.isClient()) return true;
                m.lobbyMode = m.lobbyMode == Game.M_COOP ? Game.M_PVP : (m.lobbyMode == Game.M_PVP ? Game.M_PVPVE : Game.M_COOP); return true;
            case A_MAPC: case A_LOBBY_MAP:
                if (g.mpm.isClient()) return true;
                g.save.selectedMap = (g.save.selectedMap + 1) % WorldMap.MODES; g.save.save(); return true;
            case A_SPLIT: m.startSplit(m.lobbyMode); return true;
            case A_HOST_LAN: m.hostLan(); return true;
            case A_FOUND0: case A_FOUND0 + 1: case A_FOUND0 + 2: {
                int i = a - A_FOUND0;
                if (i < found.size()) m.joinLan(found.get(i).ip, found.get(i).port);
                return true;
            }
            case A_JOIN_IP: g.platform.promptText(s("mp_enter_ip"), "192.168.", t -> { if (!t.isEmpty()) m.joinLan(hostPart(t), portPart(t, NetHost.LAN_PORT)); }); return true;
            case A_HOST_ONLINE:
                if (g.save.serverAddress.isEmpty()) { askServer(() -> m.hostOnline(hostPart(g.save.serverAddress), portPart(g.save.serverAddress, 47800))); return true; }
                m.hostOnline(hostPart(g.save.serverAddress), portPart(g.save.serverAddress, 47800)); return true;
            case A_JOIN_ONLINE: {
                Runnable ask = () -> g.platform.promptText(s("mp_enter_code"), "", t -> { if (t.length() == 4) m.joinOnline(hostPart(g.save.serverAddress), portPart(g.save.serverAddress, 47800), t); });
                if (g.save.serverAddress.isEmpty()) askServer(ask); else ask.run();
                return true;
            }
            case A_SERVER: askServer(null); return true;
            case A_NAME: g.platform.promptText(s("mp_name"), g.save.playerName, t -> { if (!t.isEmpty()) { g.save.playerName = t.length() > 14 ? t.substring(0, 14) : t; g.save.save(); } }); return true;
            case A_MP_BACK: m.discovery.stop(); g.state = Game.S_MENU; return true;
            case A_LOBBY_START: if (m.isHost() && (m.lobbyCount >= 2 || m.lobbyMode == Game.M_COOP)) m.hostStart(); return true;
            case A_LOBBY_LEAVE: m.leave(); g.state = Game.S_MPMENU; m.startDiscovery(); return true;
            case A_RES_MENU: g.toMenu(); return true;
            case A_MP_PAUSE_RESUME: pauseOpen = false; return true;
            case A_MP_PAUSE_LEAVE: pauseOpen = false; g.toMenu(); return true;
            case A_RES_AGAIN:
                if (g.splitScreen) { g.startRun(); return true; }
                if (m.isHost()) { g.state = Game.S_LOBBY; return true; }
                g.toMenu(); return true;
        }
        return false;
    }

    private void askServer(Runnable then) {
        g.platform.promptText(s("mp_server_prompt"), g.save.serverAddress.isEmpty() ? "" : g.save.serverAddress, t -> {
            if (t.isEmpty()) return;
            g.save.serverAddress = t; g.save.save();
            if (then != null) then.run();
        });
    }
    static String hostPart(String a) { int c = a.lastIndexOf(':'); return c > 0 ? a.substring(0, c).trim() : a.trim(); }
    static int portPart(String a, int def) { int c = a.lastIndexOf(':'); try { return c > 0 ? Integer.parseInt(a.substring(c + 1).trim()) : def; } catch (Exception e) { return def; } }

    private String s(String k) { return g.strings.get(k); }

    // ------------------------------------------------------------------ rendering: screens
    void renderMenu(Gfx gfx) {
        float W = g.viewW;
        ui.neonText(gfx, s("multiplayer"), W / 2, 100, 56, UIManager.NEON);
        gfx.text(s("mp_hint"), W / 2, 132, 18, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
        found.clear();
        found.addAll(g.mpm.discovery.list());
        for (int i = 0; i < menuB.size(); i++) {
            UIManager.Button b = menuB.get(i);
            int a = b.action;
            if (a >= A_FOUND0 && a <= A_FOUND0 + 2) {
                int k = a - A_FOUND0;
                b.enabled = k < found.size();
                if (!b.enabled) {
                    if (k == 0) gfx.text(s("mp_searching") + dots(), b.cx(), b.cy() + 8, 20, 0x88FFFFFF, Gfx.ALIGN_CENTER, false);
                    continue;
                }
                Discovery.Found f = found.get(k);
                b.key = "";
                ui.drawButton(gfx, b);
                gfx.text(s("mp_join") + " " + f.name + "  ·  " + f.mode + "  ·  " + f.players + "/4", b.cx(), b.cy() + 8, 22, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
                continue;
            }
            if (b.kind == UIManager.B_CYCLE) {
                gfx.roundRect(b.l, b.t, b.r, b.b, 12, b == ui.pressed ? 0x5518FFFF : 0xCC0E1530);
                gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 12, 2, b.color);
                String v = a == A_MODE ? s(Game.MODE_KEYS[g.mpm.lobbyMode]) : a == A_MAPC ? s(WorldMap.MODE_KEYS[g.save.selectedMap])
                        : a == A_SERVER ? (g.save.serverAddress.isEmpty() ? s("mp_not_set") : g.save.serverAddress) : g.save.playerName;
                gfx.text(s(b.key), b.l + 18, b.cy() + 8, 22, 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
                gfx.text(v, b.r - 18, b.cy() + 8, ui.fit(gfx, v, (b.r - b.l) * 0.55f, 22, true), b.color, Gfx.ALIGN_RIGHT, true);
                continue;
            }
            ui.drawButton(gfx, b);
        }
        String err = g.mpm.lastError;
        if (err != null) gfx.text(err, W / 2, g.viewH - 128, 18, 0xFFFF5252, Gfx.ALIGN_CENTER, true);
    }

    private String dots() { int n = ((int) (g.realTime * 2)) % 4; return n == 0 ? "" : (n == 1 ? "." : (n == 2 ? ".." : "...")); }

    void renderLobby(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        MultiplayerManager m = g.mpm;
        ui.neonText(gfx, s("mp_lobby"), W / 2, 100, 56, UIManager.NEON);
        String info;
        if (m.isHost()) {
            NetHost h = (NetHost) g.net;
            if (m.online) info = h.roomCode != null ? s("mp_room_code") + ":  " + h.roomCode : (h.failed ? h.status : s("mp_connecting") + dots());
            else info = h.failed ? h.status : s("mp_your_ip") + ":  " + NetHost.localAddresses();
        } else {
            info = g.net != null && g.net.failed ? g.net.status : (m.myIndex >= 0 ? s("mp_waiting_host") + dots() : s("mp_connecting") + dots());
        }
        gfx.text(info, W / 2, 160, ui.fit(gfx, info, W - 60, m.online && m.isHost() ? 40 : 24, true), UIManager.GOLD, Gfx.ALIGN_CENTER, true);
        gfx.text(s(Game.MODE_KEYS[m.lobbyMode]) + "  ·  " + s(WorldMap.MODE_KEYS[m.isHost() ? g.save.selectedMap : m.lobbyMap]), W / 2, 200, 22, 0xFFB0BEC5, Gfx.ALIGN_CENTER, true);
        float y = 250;
        for (int i = 0; i < 4; i++) {
            String n = m.names[i];
            gfx.roundRect(60, y, W - 60, y + 70, 14, n != null ? 0xCC0E1530 : 0x55000000);
            gfx.strokeRoundRect(60, y, W - 60, y + 70, 14, 2, n != null ? Hero.COLORS[i] : 0x33FFFFFF);
            gfx.circle(100, y + 35, 16, n != null ? Hero.COLORS[i] : 0x33FFFFFF);
            gfx.text(n != null ? n : s("mp_empty"), 130, y + 44, 26, n != null ? 0xFFFFFFFF : 0x66FFFFFF, Gfx.ALIGN_LEFT, true);
            if (i == 0 && n != null) gfx.text(s("mp_host_tag"), W - 80, y + 44, 18, UIManager.GOLD, Gfx.ALIGN_RIGHT, true);
            y += 82;
        }
        boolean host = m.isHost();
        for (int i = 0; i < lobbyB.size(); i++) {
            UIManager.Button b = lobbyB.get(i);
            b.enabled = host || i == 3;
            if (i == 2) b.enabled = host && (m.lobbyCount >= 2 || m.lobbyMode == Game.M_COOP);
            if (b.kind == UIManager.B_CYCLE) {
                if (!host) continue;
                gfx.roundRect(b.l, b.t, b.r, b.b, 12, 0xCC0E1530);
                gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 12, 2, b.color);
                gfx.text(s(b.key), b.l + 18, b.cy() + 8, 22, 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
                gfx.text(i == 0 ? s(Game.MODE_KEYS[m.lobbyMode]) : s(WorldMap.MODE_KEYS[g.save.selectedMap]), b.r - 18, b.cy() + 8, 22, b.color, Gfx.ALIGN_RIGHT, true);
                continue;
            }
            if (i == 2 && !host) continue;
            ui.drawButton(gfx, b);
        }
        if (host) gfx.text(s(Game.MODE_DESC[m.lobbyMode]), W / 2, H - 436, 18, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
    }

    void renderResults(Gfx gfx) {
        float W = g.viewW;
        ui.dim(gfx, 0xD9070B16);
        boolean pvp = g.pvp();
        ui.neonText(gfx, s(pvp ? "mp_match_over" : "game_over"), W / 2, 120, 64, pvp ? UIManager.GOLD : UIManager.PINK);
        gfx.text(s(Game.MODE_KEYS[g.mode]) + "  ·  " + MathUtil.time(g.time), W / 2, 165, 24, 0xFFB0BEC5, Gfx.ALIGN_CENTER, true);
        Hero[] order = ranked();
        float y = 220;
        for (int r = 0; r < order.length; r++) {
            Hero h = order[r];
            gfx.roundRect(50, y, W - 50, y + 100, 16, r == 0 ? 0x44FFD740 : 0xCC0E1530);
            gfx.strokeRoundRect(50, y, W - 50, y + 100, 16, r == 0 ? 3 : 2, Hero.COLORS[h.id]);
            gfx.text("#" + (r + 1), 80, y + 62, 36, r == 0 ? UIManager.GOLD : 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
            gfx.text(h.name, 160, y + 45, 28, Hero.COLORS[h.id], Gfx.ALIGN_LEFT, true);
            String line = pvp ? s("mp_pk") + " " + h.pvpKills + "   " + s("mp_deaths") + " " + h.deaths + "   " + s("kills") + " " + h.enemyKills
                    : s("kills") + " " + h.enemyKills + "   " + s("lv") + " " + h.player.level + "   " + s("mp_downs") + " " + h.deaths;
            gfx.text(line, 160, y + 80, 19, 0xFFB0BEC5, Gfx.ALIGN_LEFT, false);
            gfx.text(Integer.toString(h.score), W - 80, y + 62, 30, UIManager.GOLD, Gfx.ALIGN_RIGHT, true);
            y += 112;
        }
        ui.coinIcon(gfx, W / 2 - 80, y + 26, 13);
        gfx.text("+" + g.heroes[g.localHero].coins + " " + s("coins"), W / 2 - 60, y + 35, 26, 0xFFFFC400, Gfx.ALIGN_LEFT, true);
        resultB.get(0).enabled = g.gameOverTimer > 0.8f && (g.splitScreen || g.mpm.isHost());
        resultB.get(1).enabled = g.gameOverTimer > 0.8f;
        for (UIManager.Button b : resultB) if (b.action != A_RES_AGAIN || g.splitScreen || g.mpm.isHost()) ui.drawButton(gfx, b);
    }

    Hero[] ranked() {
        int n = 0;
        for (int i = 0; i < g.heroCount; i++) if (g.heroes[i].active || g.heroes[i].score > 0) n++;
        Hero[] out = new Hero[n];
        int k = 0;
        for (int i = 0; i < g.heroCount; i++) if (g.heroes[i].active || g.heroes[i].score > 0) out[k++] = g.heroes[i];
        java.util.Arrays.sort(out, (a, b) -> g.pvp() && a.pvpKills != b.pvpKills ? b.pvpKills - a.pvpKills : b.score - a.score);
        return out;
    }

    // ------------------------------------------------------------------ rendering: in-match overlays
    void renderGame(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        layoutGame(W, H);
        if (g.splitScreen) renderSplitHud(gfx);
        // teammates / opponents strip
        float y = g.splitScreen ? 120 : 150;
        if (!g.splitScreen) {
            for (int i = 0; i < g.heroCount; i++) {
                Hero h = g.heroes[i];
                if (!h.active || i == g.localHero) continue;
                gfx.circle(30, y, 8, h.color);
                gfx.text(h.name, 44, y + 6, 16, 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
                ui.bar(gfx, 140, y - 6, 250, y + 6, h.downed ? 0 : h.player.hp / h.player.maxHp, h.downed ? 0xFF888888 : h.color, 0x88000000);
                if (h.downed) gfx.text(g.pvp() ? Integer.toString((int) Math.ceil(h.respawnTimer)) : s("mp_down_short"), 260, y + 6, 15, 0xFFFF5252, Gfx.ALIGN_LEFT, true);
                y += 24;
            }
        }
        // PvP scoreboard + match clock
        if (g.pvp()) {
            Hero[] r = ranked();
            float sx = W / 2 - 120, sy = g.splitScreen ? 170 : 210;
            gfx.roundRect(sx, sy, sx + 240, sy + 22 + 22 * r.length, 10, 0x99000000);
            gfx.text(MathUtil.time(Math.max(0, g.matchLength - g.time)), W / 2, sy + 18, 18, UIManager.GOLD, Gfx.ALIGN_CENTER, true);
            for (int i = 0; i < r.length; i++) {
                gfx.text(r[i].name, sx + 12, sy + 40 + i * 22, 16, r[i].color, Gfx.ALIGN_LEFT, true);
                gfx.text(r[i].pvpKills + " ⚔  " + r[i].score, sx + 228, sy + 40 + i * 22, 16, 0xFFFFFFFF, Gfx.ALIGN_RIGHT, true);
            }
        }
        for (int h = 0; h < localCount(); h++) {
            Hero hero = localHero(h);
            float l = g.splitScreen ? (h == 0 ? 0 : W / 2) : 0, rr = g.splitScreen ? l + W / 2 : W;
            if (hero.downed) {
                gfx.rect(l, 0, rr, H, 0x55200000);
                String t = g.pvp() ? s("mp_respawn_in") + " " + (int) Math.ceil(hero.respawnTimer) : s("mp_downed");
                gfx.text(t, (l + rr) / 2, H * 0.45f, g.splitScreen ? 26 : 40, 0xFFFF5252, Gfx.ALIGN_CENTER, true);
                if (!g.pvp()) gfx.text(s("revive_hint"), (l + rr) / 2, H * 0.45f + 36, 18, 0xFFFFFFFF, Gfx.ALIGN_CENTER, false);
                continue;
            }
            // loot prompt
            if (hero.nearby != null) {
                Weapon w = hero.nearby.weapon;
                UIManager.Button e = ctl(h, H_EQUIP), sc = ctl(h, H_SCRAP);
                String nm = w.name(g.strings);
                gfx.text(nm, (e.l + sc.r) / 2, e.t - 10, ui.fit(gfx, nm, sc.r - e.l, 20, true), Loot.COLORS[w.rarity], Gfx.ALIGN_CENTER, true);
                e.color = Loot.COLORS[w.rarity];
                ui.drawButton(gfx, e); ui.drawButton(gfx, sc);
            }
            // non-pausing upgrade cards
            if (hero.cardsOpen) {
                Hero prev = g.hero;
                g.bind(hero);
                UpgradeManager um = hero.upgrades;
                gfx.text(s("level_up") + "  " + hero.name, (l + rr) / 2, ctl(h, H_CARD).t - 12, 22, UIManager.NEON, Gfx.ALIGN_CENTER, true);
                for (int c = 0; c < um.cardCount; c++) {
                    UIManager.Button b = ctl(h, H_CARD + c);
                    UpgradeManager.Card cd = um.cards[c];
                    int rc = cd.type == UpgradeManager.T_STAT ? Config.RARITY_COLORS[cd.rarity] : (cd.rolled != null ? Loot.COLORS[cd.rolled.rarity] : 0xFF64FFDA);
                    gfx.roundRect(b.l, b.t, b.r, b.b, 12, b == ui.pressed ? MathUtil.alpha(rc, 0.3f) : 0xE6101830);
                    gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 12, 2, rc);
                    String ttl = um.title(cd), ds = um.describe(cd);
                    float bw = b.r - b.l - 24;
                    gfx.text(ttl, b.l + 12, b.t + (b.b - b.t) * 0.45f, ui.fit(gfx, ttl, bw, g.splitScreen ? 18 : 22, true), 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
                    gfx.text(ds, b.l + 12, b.t + (b.b - b.t) * 0.82f, ui.fit(gfx, ds, bw, g.splitScreen ? 13 : 16, false), 0xFFCFD8DC, Gfx.ALIGN_LEFT, false);
                }
                g.bind(prev);
            }
        }
        if (pauseOpen) {
            ui.dim(gfx, 0xCC070B16);
            ui.neonText(gfx, s("paused"), W / 2, H * 0.35f, 64, UIManager.NEON);
            gfx.text(s("mp_no_pause"), W / 2, H * 0.35f + 46, 18, 0xFFB0BEC5, Gfx.ALIGN_CENTER, false);
            for (UIManager.Button b : pauseB) ui.drawButton(gfx, b);
        }
    }

    private void renderSplitHud(Gfx gfx) {
        float W = g.viewW, H = g.viewH;
        gfx.line(W / 2, 140, W / 2, H, 2, 0x33FFFFFF);
        for (int h = 0; h < 2; h++) {
            Hero hero = g.heroes[h];
            Player p = hero.player;
            float l = h == 0 ? 0 : W / 2, r = l + W / 2;
            gfx.text(hero.name, l + 16, 70, 18, hero.color, Gfx.ALIGN_LEFT, true);
            ui.bar(gfx, l + 16, 78, r - 16, 98, p.hp / p.maxHp, hero.color, 0x88000000);
            ui.bar(gfx, l + 16, 102, r - 16, 110, hero.xp.progressOf(p), 0xFF7C4DFF, 0x66000000);
            gfx.text(s("lv") + " " + p.level + "   " + s("kills") + " " + hero.enemyKills, l + 16, 132, 16, 0xFFFFFFFF, Gfx.ALIGN_LEFT, true);
            for (int s2 = 0; s2 < Config.HOTBAR; s2++) {
                UIManager.Button b = ctl(h, s2);
                Weapon w = hero.weapons.slots[s2];
                boolean act = s2 == hero.weapons.active;
                int rc = w == null ? 0x55FFFFFF : Loot.COLORS[w.rarity];
                gfx.roundRect(b.l, b.t, b.r, b.b, 10, act ? 0xEE16213E : 0xAA0B1022);
                gfx.strokeRoundRect(b.l, b.t, b.r, b.b, 10, act ? 3 : 1.5f, rc);
                if (w != null) { gfx.save(); gfx.translate(b.cx() - 10, b.cy()); gfx.rotate(-0.25f); g.world.drawGun(gfx, w, 0.75f, 0); gfx.restore(); }
            }
            UIManager.Button nb = ctl(h, H_NOVA);
            float cd = hero.ctl.abilityCooldown, rad = (nb.r - nb.l) / 2;
            gfx.circle(nb.cx(), nb.cy(), rad, cd <= 0 ? 0x5540C4FF : 0x66000000);
            if (cd > 0) gfx.arc(nb.cx(), nb.cy(), rad, -MathUtil.PI / 2, MathUtil.TAU * (1 - cd / Config.NOVA_COOLDOWN), 0x4440C4FF);
            gfx.ring(nb.cx(), nb.cy(), rad, 3, cd <= 0 ? 0xFF40C4FF : 0x8840C4FF);
            gfx.text(cd <= 0 ? s("nova") : Integer.toString((int) Math.ceil(cd)), nb.cx(), nb.cy() + 7, 18, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
            Joystick j = hero.joystick;
            if (j.active) {
                gfx.circle(j.baseX, j.baseY, j.radius, 0x22FFFFFF);
                gfx.circle(j.baseX + j.outX * j.radius, j.baseY + j.outY * j.radius, 30, MathUtil.alpha(hero.color, 0.6f));
            }
        }
        gfx.text(MathUtil.time(g.time), W / 2, 40, 30, 0xFFFFFFFF, Gfx.ALIGN_CENTER, true);
    }
}
