package com.blockkillers.core;

/**
 * One player in the match (local, split-screen or remote). Bundles that player's body, hotbar, companions,
 * XP, upgrade cards, joystick and match stats. Game systems operate on the currently bound hero.
 */
public final class Hero {
    public static final int[] COLORS = {0xFF40C4FF, 0xFFFF4081, 0xFF76FF03, 0xFFFFD740};
    public static final int REGION_FULL = 0, REGION_LEFT = 1, REGION_RIGHT = 2;

    public final int id;
    public final Player player = new Player();
    public final PlayerController ctl;
    public final WeaponManager weapons;
    public final PetManager pets;
    public final XpManager xp;
    public final UpgradeManager upgrades;
    public final Joystick joystick = new Joystick();

    public boolean active;
    /** Controlled on this device (split-screen players are local too). */
    public boolean local;
    public int region = REGION_FULL;
    /** Remote connection id (host side) or -1. */
    public int link = -1;
    public String name = "P1";
    public int color;

    // multiplayer state
    public boolean downed, cardsOpen;
    public float reviveProgress, respawnTimer, downTime;
    public int pvpKills, deaths, enemyKills, score, coins;
    public float coinsF;
    public GroundItem nearby;
    public Hero lastAttacker;
    /** Remote input (host side). */
    public float netX, netY;
    public int netAbilitySeq, lastAbilitySeq;
    /** Client mirror helpers. */
    public float netXp, netBeamAngle, netRecoil;
    public boolean netBeam;
    public Weapon netActiveRemote;

    public Hero(Game g, int id) {
        this.id = id;
        color = COLORS[id % COLORS.length];
        ctl = new PlayerController(g);
        weapons = new WeaponManager(g);
        pets = new PetManager(g);
        xp = new XpManager(g);
        upgrades = new UpgradeManager(g);
        weapons.owner = this;
    }

    public boolean canAct() { return active && player.alive && !downed; }

    public void resetMatchStats() {
        downed = false; cardsOpen = false; reviveProgress = 0; respawnTimer = 0; downTime = 0;
        pvpKills = deaths = enemyKills = score = coins = 0; coinsF = 0; nearby = null; lastAttacker = null;
        netX = netY = 0; netAbilitySeq = lastAbilitySeq = 0;
    }
}
