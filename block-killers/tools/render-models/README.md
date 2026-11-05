# Model renderer (offline)

The game draws pre-rendered sprites of real 3D models. This folder turns the CC0 model packs into
the sprite sheets in `app/src/main/assets/sprites/`.

1. Download the model packs (all CC0, see `CREDITS.md`) into `<work>/models/oga/`
   (Kenney Animated Characters 1-3, Kenney Blaster Kit 2.1, Quaternius Low Poly Animated Guns, OpenGameArt "Darsh").
2. Copy this folder to `<work>/render/`, run `npm install` there (three.js 0.160, plus `playwright` for headless Chromium).
3. From `<work>` run `python3 -m http.server 8765`.
4. `cd render && node run.js out-prod prod.json` renders all characters (8 directions x 8 walk frames + top view)
   and guns (side, top, first-person) with WebGL in headless Chromium (SwiftShader).
5. `python3 pack.py out-prod ../path/to/app/src/main/assets/sprites` quantizes and packs them and writes `sprites.json`.

`prod.json` maps each zombie type / survivor / boss / gun to a model, pose, skin, tint and scale.
Poses are procedural (bone rotations on the Kenney rig); framing is automatic from the measured silhouette.
