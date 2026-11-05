# Testing summary

## Environment limitation (stated plainly)
This build environment has **no KVM**, so an Android emulator could not run, and there is no physical device. The APK has therefore
**not been installed on a real phone yet**. To compensate, three layers of verification were used:

| Layer | What runs | Result |
|---|---|---|
| 1. Headless gameplay suite (`tools/run-tests.sh`) | the real `core` game code, driven through real button positions and a bot player | **99 / 99 passed** |
| 2. Robolectric (`gradle :app:testDebugUnitTest`) | the real **Android layer**: `MainActivity` lifecycle, `GameView` driven by `Choreographer` vsync, `MotionEvent` touches, `AndroidGfx` on a native Skia `Canvas`, `SharedPreferences`, SoundPool/AudioTrack, back button, relaunch | **3 / 3 passed** |
| 3. APK verification | `aapt2 dump badging`, `dexdump`, `apksigner verify` | package `com.blockkillers`, minSdk 24, targetSdk 34, launcher activity present, only `VIBRATE` permission, v2+v3 signatures valid, no desktop classes in the dex |

## Layer 1: gameplay checks (99)
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
