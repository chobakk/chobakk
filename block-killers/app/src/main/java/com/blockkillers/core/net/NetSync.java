package com.blockkillers.core.net;

import com.blockkillers.core.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Wire protocol. Host is authoritative: clients send inputs/actions, the host sends compact snapshots
 * (positions quantized to 1/4 unit) plus recorded one-shot events (particles, sounds, announcements).
 */
public final class NetSync {
    private NetSync() {}
    // client -> host
    public static final byte HELLO = 1, INPUT = 2, ACTION = 4;
    public static final byte A_SLOT = 1, A_CARD = 2, A_EQUIP = 3, A_SCRAP = 4, A_ABILITY = 5;
    // host -> client
    public static final byte WELCOME = 10, LOBBY = 11, START = 12, SNAP = 13, FULL = 15;

    private static short q(float v) { return (short) MathUtil.clamp(Math.round(v * 4f), -32760, 32760); }
    private static float dq(short v) { return v / 4f; }
    private static byte ang(float a) { return (byte) Math.round(a / MathUtil.TAU * 256f); }
    private static float dang(byte b) { return b / 256f * MathUtil.TAU; }
    private static byte frac(float f) { return (byte) Math.round(MathUtil.clamp(f, 0, 1) * 255f); }
    private static float dfrac(byte b) { return (b & 0xFF) / 255f; }

    // ------------------------------------------------------------------ small messages
    public static byte[] hello(String name) {
        try { ByteArrayOutputStream bo = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(bo); o.writeByte(HELLO); o.writeUTF(name); return bo.toByteArray(); }
        catch (IOException e) { throw new RuntimeException(e); }
    }
    public static byte[] input(float x, float y) {
        return new byte[]{INPUT, (byte) Math.round(MathUtil.clamp(x, -1, 1) * 127), (byte) Math.round(MathUtil.clamp(y, -1, 1) * 127)};
    }
    public static byte[] action(byte kind, int arg) { return new byte[]{ACTION, kind, (byte) arg}; }
    public static byte[] welcome(int heroIndex) { return new byte[]{WELCOME, (byte) heroIndex}; }

    /** Lobby state: mode, map, player names. */
    public static byte[] lobby(int mode, int map, String[] names, int count) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(LOBBY); o.writeByte(mode); o.writeByte(map); o.writeByte(count);
            for (int i = 0; i < count; i++) o.writeUTF(names[i] == null ? "" : names[i]);
            return bo.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    public static byte[] start(long seed, int map, int mode, int heroCount, int yourIndex, String[] names) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(START); o.writeLong(seed); o.writeByte(map); o.writeByte(mode); o.writeByte(heroCount); o.writeByte(yourIndex);
            for (int i = 0; i < heroCount; i++) o.writeUTF(names[i] == null ? "P" + (i + 1) : names[i]);
            return bo.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    // ------------------------------------------------------------------ weapons
    private static void writeWeapon(DataOutputStream o, Weapon w) throws IOException {
        if (w == null) { o.writeByte(255); return; }
        o.writeByte(w.type.id); o.writeByte(w.rarity); o.writeByte(w.maker & 0xFF); o.writeByte(w.barrel); o.writeByte(w.grip);
        o.writeByte(w.sight); o.writeByte(w.accessory); o.writeByte(w.element); o.writeByte(w.trait); o.writeByte(w.level);
        o.writeByte(Math.min(255, w.itemLevel)); o.writeByte(w.prefix & 0xFF);
    }
    private static Weapon readWeapon(DataInputStream in) throws IOException {
        int t = in.readUnsignedByte();
        if (t == 255) return null;
        Weapon w = new Weapon(Config.WEAPONS[t]);
        w.rarity = in.readUnsignedByte(); int mk = in.readUnsignedByte(); w.maker = mk == 255 ? -1 : mk;
        w.barrel = in.readUnsignedByte(); w.grip = in.readUnsignedByte(); w.sight = in.readUnsignedByte(); w.accessory = in.readUnsignedByte();
        w.element = in.readUnsignedByte(); w.trait = in.readUnsignedByte(); w.level = in.readUnsignedByte(); w.itemLevel = in.readUnsignedByte();
        int px = in.readUnsignedByte(); w.prefix = px == 255 ? -1 : px;
        return w;
    }

    // ------------------------------------------------------------------ snapshot
    public static byte[] snapshot(Game g, Hero to) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(16384);
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(SNAP);
            o.writeFloat(g.time);
            o.writeByte(g.state);
            o.writeFloat(g.deathSlowmo);
            o.writeFloat(g.bosses.warning);
            // heroes
            o.writeByte(g.heroCount);
            for (int i = 0; i < g.heroCount; i++) {
                Hero h = g.heroes[i];
                Player p = h.player;
                o.writeBoolean(h.active);
                o.writeFloat(p.x); o.writeFloat(p.y); o.writeFloat(p.radius); o.writeFloat(p.hp); o.writeFloat(p.maxHp);
                o.writeByte(ang(p.facing)); o.writeShort(p.level); o.writeByte(frac(h.xp.progressOf(p)));
                int flags = (p.alive ? 1 : 0) | (h.downed ? 2 : 0) | (p.invuln > 0 ? 4 : 0) | (p.hurtFlash > 0 ? 8 : 0);
                o.writeByte(flags);
                o.writeByte(frac(h.reviveProgress / Config.REVIVE_TIME)); o.writeByte((int) Math.ceil(Math.max(0, h.respawnTimer)));
                o.writeByte(frac(p.morph)); o.writeShort(p.upgradesTaken);
                for (int f = 0; f < p.features.length; f++) o.writeByte(Math.min(255, p.features[f]));
                o.writeInt(h.score); o.writeShort(h.pvpKills); o.writeShort(h.deaths); o.writeInt(h.enemyKills); o.writeInt(h.coins);
                o.writeFloat(p.vx); o.writeFloat(p.vy);
                Weapon w = h.weapons.activeWeapon();
                writeWeapon(o, w);
                o.writeByte(ang(w != null ? w.angle : 0)); o.writeBoolean(h.weapons.beamActive()); o.writeByte(frac(w != null ? w.recoil : 0));
                o.writeByte(frac(h.weapons.switchTimer / Config.SWITCH_DELAY));
                o.writeByte(h.pets.count);
                for (int k = 0; k < h.pets.count; k++) {
                    PetManager.Pet pt = h.pets.pets[k];
                    o.writeByte(pt.type); o.writeByte(pt.level); o.writeShort(q(pt.x)); o.writeShort(q(pt.y)); o.writeByte(ang(pt.aim));
                }
            }
            // private data for the receiving hero
            for (int s = 0; s < Config.HOTBAR; s++) writeWeapon(o, to.weapons.slots[s]);
            o.writeByte(to.weapons.active);
            o.writeFloat(to.ctl.abilityCooldown);
            o.writeBoolean(to.cardsOpen);
            if (to.cardsOpen) {
                UpgradeManager um = to.upgrades;
                o.writeByte(um.cardCount);
                for (int c = 0; c < um.cardCount; c++) {
                    UpgradeManager.Card cd = um.cards[c];
                    o.writeByte(cd.type); o.writeByte(cd.stat); o.writeByte(cd.weapon); o.writeByte(cd.rarity); o.writeByte(cd.levels); o.writeFloat(cd.value);
                    writeWeapon(o, cd.rolled);
                }
            }
            GroundItem near = to.nearby;
            int nearIdx = -1;
            for (int i = 0; i < g.loot.items.size; i++) if (g.loot.items.get(i) == near) nearIdx = i;
            o.writeShort(nearIdx);
            // enemies
            EntityList<Enemy> en = g.enemies.list;
            int n = Math.min(en.size, 420);
            o.writeShort(n);
            for (int i = 0; i < n; i++) {
                Enemy e = en.get(i);
                int f = (e.boss ? 1 : 0) | (e.flash > 0 ? 2 : 0) | (e.burnT > 0 ? 4 : 0) | (e.slowT > 0 ? 8 : 0) | (e.poisonT > 0 ? 16 : 0)
                        | (e.slagT > 0 ? 32 : 0) | (e.armored ? 64 : 0) | (e.shield > 0 ? 128 : 0);
                o.writeByte(e.type.id); o.writeByte(f); o.writeByte(e.tier);
                o.writeShort(q(e.x)); o.writeShort(q(e.y)); o.writeByte(Math.min(255, Math.round(e.radius)));
                o.writeByte(ang(e.rot)); o.writeByte(ang(MathUtil.atan2(e.vy, e.vx))); o.writeByte(frac(e.hp / e.maxHp));
                o.writeByte(e.state);
                if ((f & 128) != 0) o.writeByte(frac(e.shield / Math.max(1, e.maxShield)));
                if (e.tier > 0) { o.writeByte(e.fuseShapeA); o.writeByte(e.fuseShapeB); o.writeInt(e.fuseColor); }
                if (e.boss) { o.writeByte(e.bossKind); o.writeFloat(e.dirX); o.writeFloat(e.dirY); }
                else if (e.type.behavior == EnemyType.B_CHARGE && e.state == 1) { o.writeByte(ang(MathUtil.atan2(e.dirY, e.dirX))); }
            }
            int bossIdx = -1;
            for (int i = 0; i < n; i++) if (en.get(i) == g.bosses.current) bossIdx = i;
            o.writeShort(bossIdx);
            // enemy bullets
            EntityList<EnemyBullet> bl = g.combat.bullets;
            int nb = Math.min(bl.size, 300);
            o.writeShort(nb);
            for (int i = 0; i < nb; i++) { EnemyBullet b = bl.get(i); o.writeShort(q(b.x)); o.writeShort(q(b.y)); o.writeInt(b.color); o.writeByte((int) b.radius); }
            // player projectiles
            EntityList<Projectile> pr = g.combat.projectiles;
            int np = Math.min(pr.size, 450);
            o.writeShort(np);
            for (int i = 0; i < np; i++) {
                Projectile p = pr.get(i);
                o.writeShort(q(p.x)); o.writeShort(q(p.y)); o.writeShort(q(p.vx / 8f)); o.writeShort(q(p.vy / 8f));
                o.writeInt(p.color); o.writeByte(Math.min(255, (int) p.radius)); o.writeByte(p.special); o.writeByte(p.weaponId & 0xFF);
                o.writeByte(Math.min(255, (int) (p.age * 50)));
            }
            // ground loot
            EntityList<GroundItem> it = g.loot.items;
            o.writeByte(it.size);
            for (int i = 0; i < it.size; i++) { GroundItem gi = it.get(i); o.writeShort(q(gi.x)); o.writeShort(q(gi.y)); o.writeByte((int) Math.min(255, gi.life)); writeWeapon(o, gi.weapon); }
            // chests
            WorldMap m = g.map;
            o.writeByte(m.chestCount);
            for (int i = 0; i < m.chestCount; i++) o.writeBoolean(m.opened[i]);
            // events
            NetEvents ev = g.netEvents;
            o.writeByte(ev.fxN);
            for (int i = 0; i < ev.fxN; i++) {
                o.writeByte(ev.fxKind[i]); o.writeInt(ev.fxColor[i]);
                for (int k = 0; k < 5; k++) o.writeFloat(ev.fxF[i][k]);
                if (ev.fxKind[i] == 2) o.writeUTF(ev.fxText[i] == null ? "" : ev.fxText[i]);
            }
            o.writeByte(ev.sfxN);
            for (int i = 0; i < ev.sfxN; i++) o.writeByte(ev.sfx[i]);
            o.writeBoolean(ev.annTitle != null);
            if (ev.annTitle != null) { o.writeUTF(ev.annTitle); o.writeUTF(ev.annSub == null ? "" : ev.annSub); }
            return bo.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    /** Client: rebuild the visible world from a snapshot. */
    public static void apply(Game g, byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        in.readByte();
        g.time = in.readFloat();
        int hostState = in.readByte();
        g.deathSlowmo = in.readFloat();
        g.bosses.warning = in.readFloat();
        int hc = in.readUnsignedByte();
        g.heroCount = hc;
        for (int i = 0; i < hc; i++) {
            Hero h = g.heroes[i];
            Player p = h.player;
            h.active = in.readBoolean();
            p.x = in.readFloat(); p.y = in.readFloat(); p.radius = in.readFloat(); p.hp = in.readFloat(); p.maxHp = in.readFloat();
            p.facing = dang(in.readByte()); p.level = in.readShort(); h.netXp = dfrac(in.readByte());
            int flags = in.readUnsignedByte();
            p.alive = (flags & 1) != 0; h.downed = (flags & 2) != 0; p.invuln = (flags & 4) != 0 ? 0.1f : 0; p.hurtFlash = (flags & 8) != 0 ? 0.2f : 0;
            h.reviveProgress = dfrac(in.readByte()) * Config.REVIVE_TIME; h.respawnTimer = in.readUnsignedByte();
            p.morph = dfrac(in.readByte()); p.upgradesTaken = in.readShort();
            for (int f = 0; f < p.features.length; f++) p.features[f] = in.readUnsignedByte();
            h.score = in.readInt(); h.pvpKills = in.readShort(); h.deaths = in.readShort(); h.enemyKills = in.readInt(); h.coins = in.readInt();
            p.vx = in.readFloat(); p.vy = in.readFloat();
            Weapon w = readWeapon(in);
            float wa = dang(in.readByte()); boolean beam = in.readBoolean(); float recoil = dfrac(in.readByte());
            float sw = dfrac(in.readByte()) * Config.SWITCH_DELAY;
            if (h != g.heroes[g.localHero]) {
                h.weapons.slots[0] = w; h.weapons.active = 0;
                for (int s = 1; s < Config.HOTBAR; s++) h.weapons.slots[s] = null;
            } else if (w != null) h.netActiveRemote = w;
            h.netBeamAngle = wa; h.netBeam = beam; h.netRecoil = recoil; h.weapons.switchTimer = sw;
            int pc = in.readUnsignedByte();
            h.pets.count = pc;
            for (int k = 0; k < pc; k++) {
                PetManager.Pet pt = h.pets.pets[k];
                pt.type = in.readUnsignedByte(); pt.level = in.readUnsignedByte(); pt.x = dq(in.readShort()); pt.y = dq(in.readShort()); pt.aim = dang(in.readByte());
                pt.anim += 0.05f;
            }
        }
        Hero me = g.heroes[g.localHero];
        for (int s = 0; s < Config.HOTBAR; s++) me.weapons.slots[s] = readWeapon(in);
        me.weapons.active = in.readUnsignedByte();
        for (int i = 0; i < hc; i++) {
            Hero h = g.heroes[i];
            Weapon w = h.weapons.activeWeapon();
            if (w != null) { w.angle = h.netBeamAngle; w.recoil = h.netRecoil; w.beamTime = h.netBeam ? 1f : 0f; w.owner = h; }
        }
        me.ctl.abilityCooldown = in.readFloat();
        me.cardsOpen = in.readBoolean();
        if (me.cardsOpen) {
            UpgradeManager um = me.upgrades;
            um.cardCount = in.readUnsignedByte();
            for (int c = 0; c < um.cardCount; c++) {
                UpgradeManager.Card cd = um.cards[c];
                cd.type = in.readUnsignedByte(); cd.stat = in.readUnsignedByte(); cd.weapon = in.readUnsignedByte();
                cd.rarity = in.readUnsignedByte(); cd.levels = in.readUnsignedByte(); cd.value = in.readFloat();
                cd.rolled = readWeapon(in);
            }
        }
        int nearIdx = in.readShort();
        // enemies
        EntityList<Enemy> en = g.enemies.list;
        int n = in.readShort();
        en.clear();
        for (int i = 0; i < n; i++) {
            Enemy e = en.obtain();
            int type = in.readUnsignedByte(), f = in.readUnsignedByte(), tier = in.readUnsignedByte();
            EnemyType et = Config.ENEMIES[type];
            if (e.type != et || !e.alive) { e.init(et, 0, 0, 1, 1, 1); e.spawnAnim = 1; }
            e.alive = true; e.boss = (f & 1) != 0; e.flash = (f & 2) != 0 ? 0.05f : 0;
            e.burnT = (f & 4) != 0 ? 1 : 0; e.slowT = (f & 8) != 0 ? 1 : 0; e.poisonT = (f & 16) != 0 ? 1 : 0; e.slagT = (f & 32) != 0 ? 1 : 0;
            e.armored = (f & 64) != 0;
            e.tier = tier;
            e.x = dq(in.readShort()); e.y = dq(in.readShort()); e.radius = in.readUnsignedByte();
            e.rot = dang(in.readByte()); float mv = dang(in.readByte()); e.vx = MathUtil.cos(mv); e.vy = MathUtil.sin(mv);
            e.maxHp = 1; e.hp = dfrac(in.readByte()); e.state = in.readByte(); e.spawnAnim = 1; e.t += 0.05f;
            if ((f & 128) != 0) { e.maxShield = 1; e.shield = Math.max(0.01f, dfrac(in.readByte())); } else { e.shield = 0; e.maxShield = 0; }
            if (tier > 0) { e.fuseShapeA = in.readUnsignedByte(); e.fuseShapeB = in.readUnsignedByte(); e.fuseColor = in.readInt(); } else e.fuseColor = et.color;
            if (e.boss) { e.bossKind = in.readUnsignedByte(); e.dirX = in.readFloat(); e.dirY = in.readFloat(); }
            else if (et.behavior == EnemyType.B_CHARGE && e.state == 1) { float a = dang(in.readByte()); e.dirX = MathUtil.cos(a); e.dirY = MathUtil.sin(a); }
        }
        int bossIdx = in.readShort();
        g.bosses.current = bossIdx >= 0 && bossIdx < en.size ? en.get(bossIdx) : null;
        // bullets
        EntityList<EnemyBullet> bl = g.combat.bullets;
        bl.clear();
        int nb = in.readShort();
        for (int i = 0; i < nb; i++) { EnemyBullet b = bl.obtain(); b.x = dq(in.readShort()); b.y = dq(in.readShort()); b.color = in.readInt(); b.radius = in.readUnsignedByte(); b.life = 1; }
        EntityList<Projectile> pr = g.combat.projectiles;
        pr.clear();
        int np = in.readShort();
        for (int i = 0; i < np; i++) {
            Projectile p = pr.obtain();
            p.x = dq(in.readShort()); p.y = dq(in.readShort()); p.vx = dq(in.readShort()) * 8f; p.vy = dq(in.readShort()) * 8f;
            p.color = in.readInt(); p.radius = in.readUnsignedByte(); p.special = in.readUnsignedByte(); int wid = in.readUnsignedByte();
            p.weaponId = wid == 255 ? -1 : wid; p.age = in.readUnsignedByte() / 50f; p.alive = true;
        }
        EntityList<GroundItem> it = g.loot.items;
        it.clear();
        int ni = in.readUnsignedByte();
        for (int i = 0; i < ni; i++) {
            GroundItem gi = it.obtain();
            gi.x = dq(in.readShort()); gi.y = dq(in.readShort()); gi.life = in.readUnsignedByte(); gi.weapon = readWeapon(in); gi.vx = gi.vy = 0; gi.t += 0.05f;
        }
        me.nearby = nearIdx >= 0 && nearIdx < it.size ? it.get(nearIdx) : null;
        g.loot.nearby = me.nearby;
        WorldMap m = g.map;
        int cc = in.readUnsignedByte();
        for (int i = 0; i < cc; i++) { boolean op = in.readBoolean(); if (i < m.chestCount) m.opened[i] = op; }
        // events
        int fxn = in.readUnsignedByte();
        for (int i = 0; i < fxn; i++) {
            int kind = in.readUnsignedByte(), col = in.readInt();
            float a = in.readFloat(), b = in.readFloat(), c = in.readFloat(), d = in.readFloat(), e = in.readFloat();
            String txt = kind == 2 ? in.readUTF() : null;
            switch (kind) {
                case 0: g.fx.burst(a, b, col, (int) c, d, e); break;
                case 1: g.fx.ring(a, b, c, col, d); break;
                case 2: g.fx.text(a, b, txt, col, c); break;
                default: g.fx.bolt(a, b, c, d, col);
            }
        }
        int sn = in.readUnsignedByte();
        for (int i = 0; i < sn; i++) g.audio.play(in.readUnsignedByte());
        if (in.readBoolean()) { String t = in.readUTF(), s = in.readUTF(); g.ui.announce(t, s.isEmpty() ? null : s); }
        g.netHostState = hostState;
    }
}
