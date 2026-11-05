# Testing summary

## Environment limitation (stated plainly)
This build environment has **no KVM**, so an Android emulator could not run, and there is no physical device. The APK has therefore
**not been installed on a real phone yet**. To compensate, three layers of verification were used:

| Layer | What runs | Result |
|---|---|---|
| 1. Headless gameplay suite (`tools/run-tests.sh`) | the real `core` game code, driven through real button positions and a bot player | **256 / 256 passed** |
| 2. Robolectric (`gradle :app:testDebugUnitTest`) | the real **Android layer**: `MainActivity` lifecycle, `GameView` driven by `Choreographer` vsync, `MotionEvent` touches, `AndroidGfx` on a native Skia `Canvas`, `SharedPreferences`, SoundPool/AudioTrack, back button, relaunch, hotbar switching, shop + developer menu persistence, corridor map | **5 / 5 passed** |
| 3. APK verification | `aapt2 dump badging`, `dexdump`, `apksigner verify` | package `com.blockkillers`, minSdk 24, targetSdk 34, launcher activity present, only `VIBRATE` permission, v2+v3 signatures valid, no desktop classes in the dex |

## Version 3 additions (checks 204–254)
- **Multiplayer over real TCP sockets on this machine:**
  - Wi-Fi host/client: lobby, names, start, identical map on both, client sees the horde.
  - The client's joystick moves its player on the host, and the client sees its own position.
  - Hotbar mirroring, gun switching, and non-pausing level-up cards applied by the host.
  - Snapshots stay compact (under 40 KB even with a full horde); the host notices a player leaving.
  - **Online relay:** room code, joining by code, a PvPvE match through the relay, a wrong code failing cleanly.
  - Wi-Fi discovery beacon received.
- **Same phone:** the left and right halves steer separate players, each with their own hotbar and companion.
  - Co-op downs and revives, game over when everyone is down.
  - PvP damage scaling, kill scoring and respawn, the match clock and rankings.
  - PvPvE spawns the horde.
- **3D:** the View setting, wall ray casting at the right distance, full 3D frames rendering, look-drag turning, movement along the view, and crosshair-cone auto-fire (ignores zombies behind you).
- **Control layout editor:** drag, resize, Done, kept after a restart, reset.
- **Zombie art:** all zombie types, mutated tiers and boss zombies render.
- **Bugs found in v3:**
  - an array too small for the zombie drawing;
  - the editor's −/+ toolbar overlapped the hotbar;
  - your own bullets showed as huge blobs at the 3D muzzle;
  - the multiplayer BACK button was drawn as a settings row.

## Version 2 additions (checks 100–203)
- **Hotbar:** tapping switches guns, only the held gun fires, and the gun aims at the target.
- **All 17 guns** deal damage.
- **Loot:**
  - rarity distribution
  - legendaries always roll a trait and an element
  - maker and parts are always present, with all 6 makers seen
  - the same gun can roll a 1.6× DPS spread
  - drop → PICK UP → equip / scrap / leave
  - bosses drop 3 Epic+ guns, enemies drop guns during play, drops despawn
- **Map:**
  - obstacles block the player and enemy bullets
  - enemies path around pillars
  - chests open and drop loot
  - fog of war reveals as you explore
  - minimap ↔ full map
  - each of the 3 maps plays with everyone in bounds
  - corridor: strictly left/right movement for the player and enemies
- **Elements:** Burn ×1.5 vs unprotected, Shock ×2 vs shields, Acid ×1.75 vs armor, Slag +50%, Frost slows, plus damage over time. Bosses and fused enemies are protected.
- **Legendary traits:** Vampiric, Volatile, Overcharge and Fractal all work.
- **Growth:** the player grows, the camera zooms out, every upgrade changes the body shape, and fused and mega-fused enemies spawn late (and never early).
- **Coins:** earned, banked and kept after a restart.
- **Shop:** buying, costs, persistence and the bonus applying to the run. Second Wind revives once.
- **Passive:** the 20th kill restores 2 HP.
- **Meme sounds:** air horn, oof, on/off toggle, death sting, sad trombone. All 24 sounds synthesize.
- **Companions:** start with a Drone, shoot, scale with level, upgrade, max 3, Wisp heals, offered on cards.
- **Developer menu:** every option, including the double-tap confirm on reset-all.
- **Bullet interception:** by your bullets, explosions, Nova and Pulse.
- **Screen fit:** every new screen fits on all 6 screen sizes.
- **Bugs found and fixed in version 2:**
  - bosses didn't drop their loot
  - pick-up radius was too small for drops that pop outward
  - a dead player could get stuck on the level-up screen
  - long gun names overflowed their cards
  - achievement pop-ups covered the weapon screen
  - late game snowballed (rebalanced: item-level, companion and late-HP scaling and the XP curve)
- **Bot balance (3 runs):** survived 7:45, 10:04 and 4:59, reaching level 21–60 and beating 0–1 bosses.

## Layer 1: gameplay checks (first 99)
Covered: menu navigation (all buttons), Play; input-vector movement; joystick drag; multi-touch (ability while moving); stopping; world bounds;
enemy spawning off-screen; approach; contact damage; armor math; kill → score; **XP awarded immediately**; auto-targeting kills;
level-up pauses the game and shows 3 cards; card tap applies the upgrade and resumes; XP curve; **all 10 enemy types move**; shooter fires; charger dashes;
splitter splits into 3; **all 8 weapons deal damage**; weapon slot cap; weapon level effects and cap; cards always distinct; all 12 stats appear;
rarity distribution (3238/1438/555/165 over 3000 rolls); boss rewards Epic+; card effects apply; difficulty curves (HP ×8.2 at 10 min,
damage, capped speed, spawn rate, population, variety 1 → 10 types); **boss warning at 4:57 and first boss at 5:00**; boss uses bullets and telegraphs;
**boss defeated by the player's weapons**; next boss scheduled; all 3 boss kinds attack; boss reward screen; death → game over; score formula;
new-best flag; game-over input delay; Restart; Main Menu; games-played counter; **save/load across a simulated app restart** (best score, kills,
time, level, achievements, permanent bonuses); every setting persists; vibration toggle respected; Persian language; settings from the pause menu;
SFX mute; HUD pause; time frozen while paused; resume; **backgrounding auto-pauses, saves and stops music**; foregrounding; restart and menu from pause;
back-button behaviour; achievements and toast/sound; achievement score bonus; all 82 localisation keys in EN+FA; **UI fits 6 screen sizes**
(480×854, 720×1280, 1080×2400, 1440×3200, 1200×1920, 1600×2560: all buttons on-screen and non-overlapping);
**performance with 360 enemies and every weapon: 0.13 ms/frame** simulation; pools bounded.

### Balance (bot plays without god mode, 3 runs)
The bot only kites and picks random cards. It survived 6:52, 16:07 and 10:31, reached levels 20–32, and defeated 0–2 bosses.
The first minutes are gentle (1 spawn/s, 1 enemy type), the first boss is a real but beatable challenge, and later difficulty
overwhelms eventually, as an endless survival game should.

## Bugs found and fixed during testing
- Orbit Blades missed enemies pressed against the player (orbit too wide). The orbit was shrunk and the hit radius enlarged.
- Late game was too easy (bot reached level 110+ and survived 20 min). The XP curve and late HP/damage scaling were made steeper, and attack speed is now capped.
- Bosses were ignored because weapons always shot the nearest minion. Weapons now focus-fire the boss when it is in range.
- Boss fights could last minutes. Boss HP was rebalanced.
- Lightning colour was stored in a float array, which would corrupt colours. It now uses an int array.
- RTL (Persian) settings sliders overlapped labels, so the layout is mirrored. Bidi flipped the `‹ ›` arrows, so they are now drawn as shapes.

## What still needs a real device
Feel, audio mix and haptics on physical hardware; frame rate on low-end GPUs; OEM-specific behaviour (notches, gesture navigation).
Please report anything odd, especially from your own phone.


## Version 3.1 additions (checks 255–256)
- CC0 model sprites load: 11 zombies, 4 survivors, 3 bosses and 16 first-person guns.
- Every weapon type maps to a gun model with a muzzle point.
- The 2D and 3D frame-render checks now draw the model sprites (directional billboards, first-person gun).
