# BLOCK KILLERS

A 2D top-down endless survival arcade game for Android. You are a glowing circle in a neon arena,
surrounded by hostile geometric shapes. Move, auto-fire, level up, pick upgrades, beat bosses and survive.

**APK:** [`release/Block-Killers.apk`](release/Block-Killers.apk) (≈200 KB, Android 7.0+ / API 24–34, works fully offline)

| Menu | Gameplay | Level up | Boss | Game over |
|---|---|---|---|---|
| ![](docs/screenshots/android/01-menu.png) | ![](docs/screenshots/android/02-gameplay.png) | ![](docs/screenshots/android/03-level-up.png) | ![](docs/screenshots/android/04-boss.png) | ![](docs/screenshots/android/07-game-over.png) |

*(Screenshots above were rendered by Android's own Skia canvas through Robolectric, not mock-ups.)*

## Install on your phone
1. Copy `release/Block-Killers.apk` to the phone (or download it there) and tap it.
2. Allow "Install unknown apps" for the app you opened it with (browser / file manager / messenger).
3. If Play Protect warns about an unknown developer, choose **Install anyway**. The app is self-signed, and the only permission it asks for is vibration. It has no internet permission.

## How to play
- **Move:** touch and drag anywhere (a floating joystick appears under your thumb). The bottom strip is the gun hotbar.
- **Guns:** you carry up to **5 guns** in the hotbar at the bottom. Tap a slot to switch. Only the gun in your hand fires, and it auto-aims at the nearest enemy (or focuses the boss).
- **NOVA** (right side): an active blast that also destroys enemy bullets. **Your shots, explosions, pulse, laser, blades and companions destroy enemy bullets too.**
- **Maps** (chosen when you press PLAY):
  - *Open World*: a huge map with 4 regions, pillars and ruin walls for cover, chests, and fog-of-war. Tap the minimap to open the full map.
  - *The Arena*: a small closed pit.
  - *The Corridor*: a super narrow lane where you and the enemies only move left and right.
- **Loot:** enemies, bosses and chests drop guns on the ground, colour-coded white, green, blue, purple and orange. Each gun gets a random maker (Krugg, Voltis, Halcyon, Ferro, Ironjaw, Nimbus), random parts (barrel, grip, sight, accessory), stat rolls, item level and an element. Orange legendaries add a trait (Vampiric, Volatile, Overcharged, Fractal). Walk over a drop and tap **PICK UP** to compare DPS, then equip it into a slot, scrap it for XP, or leave it.
- **Elements:**
  - **Burn:** damage over time, ×1.5 vs unprotected enemies.
  - **Shock:** ×2 vs shields, arcs to nearby enemies.
  - **Acid:** damage over time, ×1.75 vs armor.
  - **Slag:** marked enemies take +50% damage.
  - **Frost:** slows enemies.
  - Shielded enemies show a blue ring; armored enemies show steel plating.
- **17 guns:** Blaster, Twin, Tri, Piercer, Rocket, Orbit Blades, Lightning, Pulse, Scattergun, Railgun, Minigun, Flamethrower, Seeker Missiles, Glaive, Ricochet Disc, Laser Beam, Mine Layer.
- **Companions:** you start with a Drone. Level-up cards add a Spark Sprite, Turret Bot or Healing Wisp (up to 3) and upgrade them, and they also scale with your level.
- **Your shape evolves:** every upgrade adds a body part (damage = spikes, armor = shell, attack speed = orbiting nodes, health = rings, and so on). You grow as you level, enemies grow over time, and the camera zooms out.
- **Harder enemies mash up:** after about 1:40 *fused* enemies (two shapes) appear, and after 5:30 *mega-fused* ones (three shapes, shielded). They are bigger, much tougher and drop better loot.
- **Bosses** at 5:00 and then every 5 minutes, with shields and armor. Each drops 3 Epic+ guns.
- **Passive:** every 20 kills restores 2 HP.
- **Coins and Shop:** earn coins from kills, chests and bosses, and spend them in the **SHOP** on 12 permanent upgrades (including Second Wind, a once-per-run revive, and Arsenal, extra starting guns).
- **Developer menu** (main menu → DEVELOPER): add or clear coins, god mode, one-shot kills, starting level and time, legendary loadout, all companions, max or reset the shop, reset all progress, and start a test run.
- **Sounds:** everything is synthesized in code. **Meme Sounds** (on by default, toggle in Settings) adds an air horn on multi-kills, a boom every 100 kills, a bonk on mega-fused kills, an "oof" when hit, a dramatic slow-motion **ELIMINATED** death scene and a sad trombone on game over.
- **Use your own sound clips:** copy files named `death`, `kill`, `hurt`, `multikill`, `bosskill`, `boom`, `bonk`, `levelup`, `gameover`, `shoot`, `boss` or `loot` (.mp3, .ogg, .wav or .m4a) into `Android/data/com.blockkillers/files/sounds/` on your phone, then restart the game. They replace the built-in sounds, only on your device.
- **Score** = enemy values + 2 per second + boss tiers + achievements. Best scores are kept per map. 14 achievements give permanent bonuses.

## Rebuild the APK
Requirements: JDK 17+ (tested with 21), `curl`, `unzip`. The Android SDK is installed automatically.

```bash
./build-apk.sh                       # -> release/Block-Killers.apk
ANDROID_HOME=/path/to/sdk ./build-apk.sh   # use an existing SDK
```
`build-apk.sh` runs `aapt2 → javac → d8 → zipalign → apksigner` directly, so it needs no Gradle and no third-party libraries.

**Android Studio / Gradle** also works (AGP 8.7, Gradle 8.9+):
```bash
gradle :app:assembleRelease          # -> app/build/outputs/apk/release/app-release.apk
gradle :app:testDebugUnitTest        # Robolectric tests of the Android layer
```
Both builds sign with `signing/block-killers.jks` (password `blockkillers`). This keystore is committed on purpose so that rebuilt APKs install as updates. It is **not** a secret. Use your own key if you ever publish the game.

## Tests
```bash
tools/run-tests.sh            # 203 automated gameplay checks (headless, ~10 s)
tools/run-tests.sh screens    # renders every screen to docs/screenshots/
tools/run-tests.sh assets     # regenerates launcher icons + splash logo
```
See [docs/TESTING.md](docs/TESTING.md) for the full testing summary and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the design.

## Project layout
```
app/src/main/java/com/blockkillers/core/     platform-independent game (all gameplay, UI, audio synthesis)
app/src/main/java/com/blockkillers/android/  Android host: Activity, Canvas renderer, SoundPool/AudioTrack, SharedPreferences
app/src/main/res/                            icons (adaptive + PNG), splash, theme
app/src/test/                                Robolectric tests (real Android framework on the JVM)
desktop/src/                                 headless test harness, bot player, Java2D renderer, screenshot + asset generators
build-apk.sh, build.gradle, settings.gradle  build configuration
release/Block-Killers.apk                    the installable game
```
