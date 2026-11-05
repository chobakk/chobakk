# Architecture

## Technology choice
**Native Android, plain Java and a custom 2D engine on the hardware-accelerated Canvas.** No game engine and no libraries.

Why:
- The game is vector-style neon geometry (circles, polygons, lines). Canvas draws this natively with anti-aliasing, so no textures or engine are needed.
- The APK is about 150 KB, starts instantly, needs no internet, and has no third-party code.
- It builds with nothing but the Android SDK (`build-apk.sh`), so the build is reproducible anywhere.
- **All game logic is platform-free** (`core` package, no `android.*` imports). The same code runs on Android and in a desktop JVM harness, which made thorough automated testing possible without a device.

```
            ┌────────────── core (pure Java) ──────────────┐
 Android ──▶│ Game ── state machine, loop, input dispatch   │◀── Desktop harness
 MainActivity│  ├ PlayerController  ├ WeaponManager         │    GameTests (bot)
 GameView    │  ├ EnemyManager      ├ XpManager             │    Screens / AssetGen
 AndroidGfx  │  ├ EnemyAI           ├ UpgradeManager        │    DesktopGfx (Java2D)
 AndroidAudio│  ├ CombatManager     ├ WaveManager           │
 AndroidStorage ├ BossManager       ├ UIManager             │
            │  ├ AudioManager      ├ SaveManager           │
            │  ├ AchievementManager├ SettingsManager       │
            │  └ WorldRenderer, ParticleSystem, Camera, ... │
            └── interfaces: Gfx, Audio, Storage, Platform ──┘
```

## Systems (one class each)
| # | System | Class | Responsibility |
|---|---|---|---|
| 1 | Player Controller | `PlayerController`, `Player`, `Joystick` | movement from floating joystick, regen, invulnerability, NOVA ability |
| 2 | Enemy Manager | `EnemyManager` | pooled spawn, integration, separation, contact damage, despawn |
| 3 | Enemy AI | `EnemyAI` | chase, zig-zag, charge (wind-up → dash → rest), shooter (keep distance + fire), swarm |
| 4 | Combat Manager | `CombatManager` | projectiles, enemy bullets, crits, armor, knockback, kills, explosions, area damage |
| 5 | Weapon Manager | `WeaponManager`, `Weapon`, `WeaponType` | 8 weapons: projectile, parallel, spread, piercing, explosive, orbit, chain, area |
| 6 | XP & Leveling | `XpManager` | immediate XP on kill, rising requirements, queued level-ups |
| 7 | Upgrade Manager | `UpgradeManager` | 3 distinct weighted cards, 4 rarities, stat/weapon cards, apply |
| 8 | Wave & Difficulty | `WaveManager` + curves in `Config` | spawn rate, batch size, population cap, HP/damage/speed scaling, enemy unlock times |
| 9 | Boss Manager | `BossManager` | schedule (5:00, +5 min), warning, 3 bosses × multi-state attack patterns, rewards |
| 10 | UI Manager | `UIManager` | menu, HUD, pause, level-up, game over, settings, statistics, toasts, announcements |
| 11 | Audio Manager | `AudioManager` + `SoundSynth` | volumes, per-sound rate limiting; all 16 SFX and the music loop are synthesized in code |
| 12 | Save Manager | `SaveManager` | best score, games, kills, time, highest level, bosses, achievement flags |
| 13 | Achievement Manager | `AchievementManager` | 12 achievements, toast queue, permanent bonuses |
| 14 | Settings Manager | `SettingsManager` | all settings, persisted |

Version 2 systems:

- `LootManager`, `Loot`, `GroundItem`: rarity tiers, makers, parts, elements and traits; drops, chests, pick-up.
- `WorldMap`: Open World, Arena and Corridor modes; regions, obstacle grid, steering, chests, fog of war.
- `PetManager`: companions.
- `ShopManager`: coins and permanent upgrades.
- `MemeSounds`: comedic sound layer.
- `DevOptions`: developer menu.
- `WeaponManager`: now a 5-slot hotbar.
- `CombatManager`: adds element matchups, shields and armor, and enemy-bullet interception.

Supporting classes: `Config` (all balance numbers in one place), `Strings` (EN/FA localisation), `WorldRenderer`, `ParticleSystem`, `Camera`, `SpatialGrid`, `EntityList` (object pool), `Shapes`, `MathUtil`.

## Performance techniques
- **Object pooling:** `EntityList<T>` preallocates enemies (600), projectiles (500), enemy bullets (400), particles (1200) and texts (90). Nothing is allocated per entity during play.
- **Spatial hash grid** (80-unit cells) for every collision, separation, blade, chain and area query, instead of O(n²) checks.
- Deferred removal (dead flag + one compaction pass per frame) keeps indices stable while systems run.
- **View culling** for all world drawing. Paint, Path and RectF objects are reused in `AndroidGfx`.
- **Adaptive effects:** quality presets cap particles (250/600/1100), and the particle budget shrinks automatically if frames stay slow. Sound effects are rate-limited.
- **Battery:** optional 30 FPS mode, the loop stops when the app is backgrounded (Choreographer callbacks removed), and music pauses.
- Variable frame time with up to 4 fixed sub-steps keeps collisions stable on slow frames.
- Measured: simulation with 360 enemies plus all weapons runs at **0.13 ms/frame** on the desktop JVM.

## Resolution independence
Everything uses a virtual coordinate system 720 units wide. Height follows the screen's aspect ratio. `Game.resize()` computes the scale, and UI layout adapts to height (tested from 480×854 to 1600×2560 tablets). Orientation is locked to portrait.

## Saving
`Storage` interface → `SharedPreferences` on Android. Saved at game over, on achievement unlock, on settings change, and in `onPause`. Data survives app restarts (verified with Robolectric relaunch tests).

## Adding content
- New enemy: add an `EnemyType` row to `Config.ENEMIES` (shape, stats, spawn weight, unlock time, behaviour) plus a name in `Strings`.
- New weapon: add a `WeaponType` row and a `kind` (or a new case in `WeaponManager`).
- New upgrade or achievement: add a constant and its effect in `UpgradeManager` / `AchievementManager`.
- New language: add a column in `Strings`.
