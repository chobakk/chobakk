import * as THREE from 'three';
import { FBXLoader } from 'three/addons/loaders/FBXLoader.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import * as SkeletonUtils from 'three/addons/utils/SkeletonUtils.js';

const M = '/models/oga/';
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
renderer.setPixelRatio(1);
renderer.outputColorSpace = THREE.SRGBColorSpace;
const fbx = new FBXLoader(), gltf = new GLTFLoader(), tex = new THREE.TextureLoader();
const cache = {};
async function loadFBX(p) { if (!cache[p]) cache[p] = await fbx.loadAsync(M + p); return cache[p]; }
async function loadGLB(p) { if (!cache[p]) cache[p] = await gltf.loadAsync(M + p); return cache[p]; }

function scene() {
  const s = new THREE.Scene();
  s.add(new THREE.HemisphereLight(0xdfe8ff, 0x2a2018, 1.6));
  const key = new THREE.DirectionalLight(0xfff2dd, 2.6); key.position.set(2, 4, 3); s.add(key);
  const rim = new THREE.DirectionalLight(0x8fb4ff, 1.2); rim.position.set(-3, 2, -2); s.add(rim);
  return s;
}

function findBone(root, re) { let b = null; root.traverse(o => { if (!b && o.isBone && re.test(o.name)) b = o; }); return b; }

/** Build a character instance: Kenney rig + skin + optional variant tweaks. */
async function character(spec) {
  const base = await loadFBX(spec.pack + 'Model/characterMedium.fbx');
  const obj = SkeletonUtils.clone(base);
  const map = await tex.loadAsync(M + spec.pack + 'Skins/' + spec.skin + '.png');
  map.colorSpace = THREE.SRGBColorSpace; map.flipY = true;
  obj.traverse(o => {
    if (o.isMesh) {
      o.material = new THREE.MeshStandardMaterial({ map, roughness: 0.85, metalness: 0.0, color: new THREE.Color(spec.tint || 0xffffff) });
      o.frustumCulled = false;
    }
  });
  obj.scale.multiply(new THREE.Vector3(...(spec.scale || [1, 1, 1])));
  const animFile = await loadFBX(spec.pack + 'Animations/' + (spec.anim || 'run') + '.fbx');
  const clip = animFile.animations[0];
  const mixer = new THREE.AnimationMixer(obj);
  mixer.clipAction(clip).play();
  const group = new THREE.Group(); group.add(obj);
  // accessories
  const box = new THREE.Box3().setFromObject(obj); const h = box.max.y - box.min.y;
  if (spec.shield) {
    const sh = new THREE.Mesh(new THREE.BoxGeometry(h * 0.42, h * 0.55, h * 0.03), new THREE.MeshStandardMaterial({ color: 0x9cc8ff, transparent: true, opacity: 0.55, roughness: 0.2, metalness: 0.3 }));
    sh.position.set(0, h * 0.5, h * 0.2); group.add(sh);
    const rimS = new THREE.Mesh(new THREE.BoxGeometry(h * 0.44, h * 0.04, h * 0.04), new THREE.MeshStandardMaterial({ color: 0x263238 }));
    rimS.position.set(0, h * 0.77, h * 0.2); group.add(rimS);
  }
  const head = findBone(obj, /head/i);
  if (spec.helmet && head) {
    const hel = new THREE.Mesh(new THREE.SphereGeometry(1, 16, 10, 0, Math.PI * 2, 0, Math.PI / 2), new THREE.MeshStandardMaterial({ color: 0x4b5a33, roughness: 0.7 }));
    hel.scale.setScalar(h * 0.14 / obj.scale.y); hel.position.set(0, h * 0.07 / obj.scale.y, 0); head.add(hel);
  }
  return { group, obj, mixer, clip, height: h, spec };
}

/** Arms forward (zombie reach / aiming pose) applied after the animation each frame. */
function reachPose(obj, amount, spread) {
  const L = findBone(obj, /(left).*(upper|arm)|arm.*l(eft)?$|^upperarm_l|leftarm$/i) || findBone(obj, /arm.?l/i);
  const R = findBone(obj, /(right).*(upper|arm)|arm.*r(ight)?$|^upperarm_r|rightarm$/i) || findBone(obj, /arm.?r/i);
  for (const [b, s] of [[L, 1], [R, -1]]) {
    if (!b) continue;
    b.rotation.x += -amount;
    b.rotation.z += s * spread;
  }
}

function cameraFor(h, view, w, hgt) {
  const aspect = w / hgt;
  if (view === 'top') {
    const c = new THREE.OrthographicCamera(-h * 0.55 * aspect, h * 0.55 * aspect, h * 0.55, -h * 0.55, 0.01, h * 20);
    c.position.set(0, h * 6, h * 2.2); c.lookAt(0, h * 0.45, 0); return c;
  }
  const c = new THREE.OrthographicCamera(-h * 0.62 * aspect, h * 0.62 * aspect, h * 1.12, -h * 0.12, 0.01, h * 20);
  c.position.set(0, h * 1.2, h * 5); c.lookAt(0, h * 0.5, 0); return c;
}

/**
 * Character atlas: rows 0..7 = billboard directions (row d = facing d*45 deg clockwise from the camera),
 * row 8 = top-down (facing screen-up). 8 columns = walk frames.
 */
window.renderCharacter = async function (spec) {
  const W = spec.cellW || 112, H = spec.cellH || 140, F = spec.frames || 8;
  const c = await character(spec);
  const sc = scene(); sc.add(c.group);
  const out = document.createElement('canvas'); out.width = W * F; out.height = H * 9;
  const ctx = out.getContext('2d');
  renderer.setSize(W, H);
  const h = c.height * (spec.frame || 1);
  for (let row = 0; row < 9; row++) {
    const top = row === 8;
    const cam = cameraFor(h, top ? 'top' : 'side', W, H);
    for (let f = 0; f < F; f++) {
      c.mixer.setTime((f / F) * c.clip.duration);
      if (spec.reach !== undefined) reachPose(c.obj, spec.reach, spec.spread || 0);
      // facing: model faces +z (towards the camera) at yaw 0
      c.group.rotation.set(spec.crawl ? 0 : 0, 0, 0);
      c.group.rotation.y = top ? Math.PI : -row * Math.PI / 4;
      if (spec.crawl) { c.obj.rotation.x = 1.25; c.obj.position.set(0, c.height * 0.1, -c.height * 0.25); }
      if (spec.bob) c.group.position.y = Math.sin(f / F * Math.PI * 2) * c.height * 0.02;
      renderer.render(sc, cam);
      ctx.drawImage(renderer.domElement, f * W, row * H);
    }
  }
  return out.toDataURL('image/png');
};

/** Boss / creature from a GLB with its own animation (idle) + procedural stride. */
window.renderCreature = async function (spec) {
  const W = spec.cellW || 96, H = spec.cellH || 120, F = spec.frames || 8;
  const g = await loadGLB(spec.file);
  const obj = SkeletonUtils.clone(g.scene);
  obj.traverse(o => { if (o.isMesh) { o.frustumCulled = false; o.material = o.material.clone(); if (spec.tint) o.material.color = new THREE.Color(spec.tint); } });
  obj.scale.set(...(spec.scale || [1, 1, 1]));
  const mixer = new THREE.AnimationMixer(obj);
  const clip = g.animations[0];
  if (clip) mixer.clipAction(clip).play();
  const group = new THREE.Group(); group.add(obj);
  const sc = scene(); sc.add(group);
  if (clip) mixer.setTime(0);
  // measure with a big level camera, then frame
  const E = 20;
  const camS = new THREE.OrthographicCamera(-E, E, E, -E, 0.01, 400); camS.position.set(0, 0, 100); camS.lookAt(0, 0, 0);
  group.rotation.y = spec.yaw || 0;
  const mb = await measure(sc, camS, 400, 400);
  const unit = 2 * E / 400;
  const y0w = -E + mb.y0 * unit, y1w = -E + (mb.y1 + 1) * unit, x0w = -E + mb.x0 * unit, x1w = -E + (mb.x1 + 1) * unit;
  obj.position.y -= y0w;    // feet on the ground
  const hgt = y1w - y0w, aspect = W / H;
  let span = hgt * 1.12;
  const halfW = Math.max(Math.abs(x0w), Math.abs(x1w)) * 1.05;
  if (span * aspect < halfW * 2) span = halfW * 2 / aspect;
  const out = document.createElement('canvas'); out.width = W * F; out.height = H * 9;
  const ctx = out.getContext('2d');
  renderer.setSize(W, H);
  for (let row = 0; row < 9; row++) {
    const top = row === 8;
    let cam;
    if (top) { const tw = span * 0.55; cam = new THREE.OrthographicCamera(-tw, tw, tw * H / W, -tw * H / W, 0.01, 400); cam.position.set(0, 60, 18); cam.lookAt(0, hgt * 0.35, 0); }
    else { cam = new THREE.OrthographicCamera(-span * aspect / 2, span * aspect / 2, span * 0.96, -span * 0.04, 0.01, 400); cam.position.set(0, 0, 100); cam.lookAt(0, 0, 0); }
    for (let f = 0; f < F; f++) {
      if (clip) mixer.setTime((f / F) * clip.duration);
      group.rotation.set(0, (top ? Math.PI : -row * Math.PI / 4) + (spec.yaw || 0), 0);
      group.position.y = Math.abs(Math.sin(f / F * Math.PI * 2)) * hgt * 0.04;
      group.rotation.z = Math.sin(f / F * Math.PI * 2) * 0.07;
      renderer.setSize(W, H);
      renderer.render(sc, cam);
      ctx.drawImage(renderer.domElement, f * W, row * H);
    }
  }
  return out.toDataURL('image/png');
};

const _mc = document.createElement('canvas');
async function measureAlpha(sc, cam, W, H) {
  renderer.setSize(W, H); renderer.render(sc, cam);
  const url = renderer.domElement.toDataURL('image/png');
  const img = new Image(); img.src = url; await img.decode();
  _mc.width = W; _mc.height = H;
  const cx = _mc.getContext('2d', { willReadFrequently: true });
  cx.clearRect(0, 0, W, H); cx.drawImage(img, 0, 0);
  return cx.getImageData(0, 0, W, H).data;
}
const GUNMAT = [[/lightwood/i, 0x9c6b3f], [/wood|brown|handle|grip/i, 0x5d3a22], [/yellow|gold|brass/i, 0xc9a227], [/orange/i, 0xc06a2a], [/tip/i, 0xb87333], [/red/i, 0x8e2a2a], [/green/i, 0x4a6b3a], [/darkermetal|darkmetal|barrel|muzzle|trigger|magazine/i, 0x2b3338], [/black|dark/i, 0x1f2427], [/metal|grey|gray|steel|silver/i, 0x7b8a94]];
function sc0(obj) { const s = scene(); s.add(obj); return s; }
/** Gun renders: [0] side icon (muzzle right), [1] top-down (muzzle right), [2] first-person view. */
window.renderGun = async function (spec) {
  let obj;
  if (spec.file.endsWith('.glb')) obj = SkeletonUtils.clone((await loadGLB(spec.file)).scene);
  else obj = (await loadFBX(spec.file)).clone(true);
  const names = [];
  obj.traverse(o => {
    if (!o.isMesh) return;
    o.frustumCulled = false;
    const ms = Array.isArray(o.material) ? o.material : [o.material];
    const nm = ms.map((m, k) => {
      names.push(m.name);
      let c = null;
      if (spec.recolor) { c = 0x455a64; for (const [re, col] of GUNMAT) if (re.test(m.name)) { c = col; break; } if (c === 0x455a64 && k % 2 === 1) c = 0x2f3b42; }
      const mm = new THREE.MeshStandardMaterial({ map: m.map || null, color: c !== null ? new THREE.Color(c) : (m.color || new THREE.Color(0xffffff)), roughness: 0.45, metalness: spec.recolor ? 0.55 : 0.1 });
      return mm;
    });
    o.material = Array.isArray(o.material) ? nm : nm[0];
  });
  // Some FBX guns are skinned, so rotating the model is ignored by skinning: orient the CAMERAS instead.
  const sc = scene(); sc.add(obj);
  obj.updateMatrixWorld(true);
  const bb = new THREE.Box3().setFromObject(obj, true), sz = bb.getSize(new THREE.Vector3()), ctr = bb.getCenter(new THREE.Vector3());
  const ax = [new THREE.Vector3(1, 0, 0), new THREE.Vector3(0, 1, 0), new THREE.Vector3(0, 0, 1)];
  const dims = [sz.x, sz.y, sz.z];
  const order = [0, 1, 2].sort((i, j) => dims[j] - dims[i]);
  let A = ax[order[0]].clone(), U = ax[order[1]].clone();
  if (order[0] !== 1 && order[1] !== 1 && dims[1] > 0) U = ax[1].clone();   // prefer world-up as "up" when it is not the barrel
  if (spec.upAxis) U = new THREE.Vector3(...spec.upAxis);
  const L = dims[order[0]], Hgt = Math.abs(U.x) * sz.x + Math.abs(U.y) * sz.y + Math.abs(U.z) * sz.z;
  const Wid = sz.x + sz.y + sz.z - L - Hgt;
  function ortho(dirFrom, up, halfW, halfH) {
    const c = new THREE.OrthographicCamera(-halfW, halfW, halfH, -halfH, 0.001, L * 40);
    c.position.copy(ctr).addScaledVector(dirFrom, L * 8); c.up.copy(up); c.lookAt(ctr); return c;
  }
  // side camera: looking along the thin axis, so that screen-right == +A
  let N = new THREE.Vector3().crossVectors(A, U).normalize();   // camera sits at +N, looking -N: right = (-N) x U = A x? check below
  const right = new THREE.Vector3().crossVectors(N.clone().negate(), U);
  if (right.dot(A) < 0) N.negate();
  const hH = Math.max(L * 0.26, Hgt * 0.62), hW = hH * 2;
  // muzzle detection: thinner end of the side silhouette should be on the right
  let cam = ortho(N, U, hW, hH);
  const NN = 200, px = await measureAlpha(sc, cam, NN, NN / 2);
  const colH = x => { let c = 0; for (let y = 0; y < NN / 2; y++) if (px[(y * NN + x) * 4 + 3] > 8) c++; return c; };
  let minX = NN, maxX = 0; for (let x = 0; x < NN; x++) if (colH(x) > 0) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); }
  const seg = Math.max(2, Math.floor((maxX - minX) * 0.18));
  let left = 0, right2 = 0; for (let k = 0; k < seg; k++) { left += colH(minX + k); right2 += colH(maxX - k); }
  if ((left < right2) !== !!spec.flip) { A.negate(); N.negate(); }
  const results = [];
  cam = ortho(N, U, hW, hH);
  renderer.setSize(256, 128); renderer.render(sc, cam); results.push(renderer.domElement.toDataURL('image/png'));
  // top-down: from +U, screen-right = +A
  const tH = Math.max(L * 0.26, Wid * 0.62), tW = tH * 2;
  let topUp = N.clone().negate();
  let tc = ortho(U, topUp, tW, tH);
  const tr = new THREE.Vector3().crossVectors(U.clone().negate(), topUp);
  if (tr.dot(A) < 0) { topUp.negate(); tc = ortho(U, topUp, tW, tH); }
  renderer.setSize(256, 128); renderer.render(sc, tc); results.push(renderer.domElement.toDataURL('image/png'));
  // first person: behind the stock, to the gun's right and above -> gun sits bottom-right, barrel towards the centre
  const pc = new THREE.PerspectiveCamera(spec.fov || 52, 1, 0.001, L * 50);
  const fpDist = spec.fpDist || 1;
  pc.position.copy(ctr).addScaledVector(A, -L * 0.95 * fpDist).addScaledVector(U, Hgt * 0.75 + L * 0.08).addScaledVector(N, -L * 0.34 * fpDist);
  pc.up.copy(U);
  pc.lookAt(ctr.clone().addScaledVector(A, L * 0.9).addScaledVector(U, L * 0.02).addScaledVector(N, L * 0.12));
  renderer.setSize(512, 512); renderer.render(sc, pc); results.push(renderer.domElement.toDataURL('image/png'));
  results.push(JSON.stringify(names));
  return results;
};
window.ready = true;
window.inspect = async function (spec) {
  let obj;
  if (spec.file.endsWith('.glb')) obj = (await loadGLB(spec.file)).scene; else obj = await loadFBX(spec.file);
  obj.updateMatrixWorld(true);
  const bones = []; obj.traverse(o => { if (o.isBone) bones.push(o.name); });
  const b1 = new THREE.Box3().setFromObject(obj), b2 = new THREE.Box3().setFromObject(obj, true);
  return { bones: bones.slice(0, 60), scale: obj.scale.toArray(), box: [b1.min.toArray(), b1.max.toArray()], precise: [b2.min.toArray(), b2.max.toArray()] };
};
window.axisTest = async function () {
  const base = await loadFBX('x_kenney_animated-characters-1/Model/characterMedium.fbx');
  const map = await tex.loadAsync(M + 'x_kenney_animated-characters-1/Skins/zombieA.png'); map.colorSpace = THREE.SRGBColorSpace;
  const W = 160, H = 200;
  const tests = [['none',null,0,0],['LeftArm','x',1.2],['LeftArm','y',1.2],['LeftArm','z',1.2],['LeftUpLeg','x',0.8],['LeftUpLeg','z',0.8],['LeftLeg','x',0.8],['Spine','x',0.5]];
  const out = document.createElement('canvas'); out.width = W * tests.length; out.height = H * 2; const ctx = out.getContext('2d');
  renderer.setSize(W, H);
  for (let i = 0; i < tests.length; i++) {
    const obj = SkeletonUtils.clone(base);
    obj.traverse(o => { if (o.isMesh) { o.material = new THREE.MeshStandardMaterial({ map }); o.frustumCulled = false; } });
    const [bn, ax, v] = tests[i];
    if (bn !== 'none') { const b = findBone(obj, new RegExp('^' + bn + '$')); b.rotation[ax] += v; }
    const sc = scene(); sc.add(obj);
    for (let row = 0; row < 2; row++) {
      const h = 380, cam = new THREE.OrthographicCamera(-h * 0.6, h * 0.6, h * 1.1, -h * 0.4, 1, h * 20);
      if (row === 0) cam.position.set(0, h * 0.5, h * 5); else cam.position.set(h * 5, h * 0.5, 0);
      cam.lookAt(0, h * 0.5, 0);
      renderer.render(sc, cam); ctx.drawImage(renderer.domElement, i * W, row * H);
    }
  }
  return out.toDataURL();
};
// ---------------------------------------------------------------- procedural animation (Kenney rig)
function bones(obj) {
  const n = {};
  obj.traverse(o => { if (o.isBone) n[o.name] = o; });
  if (!obj.userData.rest) { obj.userData.rest = {}; for (const k in n) obj.userData.rest[k] = n[k].rotation.clone(); }
  return n;
}
function resetPose(obj, b) { for (const k in b) b[k].rotation.copy(obj.userData.rest[k]); }
/** style: walk (zombie shamble), run (zombie sprint), crawl, aim (survivor). ph in [0, 2pi) */
function pose(obj, style, ph, armF) {
  const b = bones(obj); resetPose(obj, b);
  const s = Math.sin(ph), c = Math.cos(ph);
  const stride = style === 'run' ? 0.75 : (style === 'aim' ? 0.5 : 0.38);
  if (b.LeftUpLeg) { b.LeftUpLeg.rotation.x += s * stride; b.RightUpLeg.rotation.x -= s * stride; }
  if (b.LeftLeg) { b.LeftLeg.rotation.x += Math.max(0, s) * stride * 1.3; b.RightLeg.rotation.x += Math.max(0, -s) * stride * 1.3; }
  const hunch = style === 'run' ? 0.42 : (style === 'aim' ? 0.08 : 0.28);
  if (b.Spine) b.Spine.rotation.x += hunch;
  if (b.Head) { b.Head.rotation.z += style === 'aim' ? 0 : 0.22 + 0.06 * s; b.Head.rotation.x += style === 'aim' ? -0.05 : -0.15; }
  // arms: rotate from T-pose to straight forward (y), then lower slightly / sway
  const f = armF === undefined ? 1.45 : armF;
  if (b.LeftArm) {
    // Kenney rig: left arm swings forward around y(-), right arm around z(+)
    const sway = style === 'aim' ? 0 : 0.1 * s;
    b.LeftArm.rotation.y += -f + sway;
    b.RightArm.rotation.z += f + sway;
    const drop = style === 'run' ? 0.2 + 0.25 * s : (style === 'aim' ? 0.1 : 0.15);
    b.LeftArm.rotation.x -= drop; b.RightArm.rotation.x += drop;
    if (style === 'aim') { b.LeftArm.rotation.y += 0.35; b.RightArm.rotation.z -= 0.15; }
    if (style === 'crawl') { b.LeftArm.rotation.x -= 0.4 * s; b.RightArm.rotation.x -= 0.4 * s; }
  }
}

/** Measure the rendered silhouette and return a camera frame that fits it with a margin. */
async function measure(sc, cam, W, H) {
  const px = await measureAlpha(sc, cam, W, H);
  let x0 = W, x1 = -1, y0 = H, y1 = -1;
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (px[(y * W + x) * 4 + 3] > 8) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y; }
  return x1 < 0 ? null : { x0, x1, y0, y1 };
}

window.renderCharacter = async function (spec) {
  const W = spec.cellW || 112, H = spec.cellH || 140, F = spec.frames || 8;
  const base = await loadFBX(spec.pack + 'Model/characterMedium.fbx');
  const obj = SkeletonUtils.clone(base);
  const map = await tex.loadAsync(M + spec.pack + 'Skins/' + spec.skin + '.png');
  map.colorSpace = THREE.SRGBColorSpace;
  obj.traverse(o => { if (o.isMesh) { o.material = new THREE.MeshStandardMaterial({ map, roughness: 0.85, color: new THREE.Color(spec.tint || 0xffffff) }); o.frustumCulled = false; } });
  obj.scale.set(...(spec.scale || [1, 1, 1]));
  const group = new THREE.Group(); group.add(obj);
  const h = 376 * (spec.scale ? spec.scale[1] : 1);
  const b = bones(obj);
  if (spec.shield) {
    const sh = new THREE.Mesh(new THREE.BoxGeometry(150, 190, 10), new THREE.MeshStandardMaterial({ color: 0xb3d4ff, transparent: true, opacity: 0.6, roughness: 0.15, metalness: 0.4 }));
    sh.position.set(0, h * 0.45, 95); group.add(sh);
    const band = new THREE.Mesh(new THREE.BoxGeometry(154, 14, 14), new THREE.MeshStandardMaterial({ color: 0x263238 })); band.position.set(0, h * 0.45 + 95, 95); group.add(band);
    const txt = new THREE.Mesh(new THREE.BoxGeometry(110, 18, 12), new THREE.MeshStandardMaterial({ color: 0xffffff })); txt.position.set(0, h * 0.42, 101); group.add(txt);
  }
  if (spec.helmet && b.Head) {
    const hel = new THREE.Mesh(new THREE.SphereGeometry(62, 20, 12, 0, Math.PI * 2, 0, Math.PI / 1.8), new THREE.MeshStandardMaterial({ color: 0x4b5a33, roughness: 0.6 }));
    hel.position.set(0, 52, 4); b.Head.add(hel);
  }
  if (spec.gun) { // survivor: a rifle held in front
    const gunG = new THREE.Group();
    const m1 = new THREE.MeshStandardMaterial({ color: 0x263238, roughness: 0.5, metalness: 0.6 });
    const body = new THREE.Mesh(new THREE.BoxGeometry(18, 26, 150), m1); body.position.set(0, 0, 60); gunG.add(body);
    const brl = new THREE.Mesh(new THREE.CylinderGeometry(5, 5, 90, 10), m1); brl.rotation.x = Math.PI / 2; brl.position.set(0, 6, 170); gunG.add(brl);
    const mag = new THREE.Mesh(new THREE.BoxGeometry(14, 40, 18), m1); mag.position.set(0, -30, 70); gunG.add(mag);
    gunG.position.set(-18, h * 0.6, 40); group.add(gunG);
  }
  const sc = scene(); sc.add(group);
  const out = document.createElement('canvas'); out.width = W * F; out.height = H * 9;
  const ctx = out.getContext('2d');
  // frame from the measured silhouette of the front view
  pose(obj, spec.style || 'walk', 0, spec.armF);
  if (spec.crawl) { obj.rotation.x = 1.25; obj.position.set(0, 30, -170); }
  let camS = new THREE.OrthographicCamera(-h, h, h * 1.4, -h * 0.6, 1, h * 30); camS.position.set(0, 0, h * 6); camS.lookAt(0, 0, 0);
  // measure the widest view too (side-on silhouettes are longer for crawlers)
  const mb = (await measure(sc, camS, 200, 200)) || { x0: 40, x1: 160, y0: 60, y1: 160 };
  group.rotation.y = -Math.PI / 2;
  const mb2 = (await measure(sc, camS, 200, 200)) || mb;
  group.rotation.y = 0;
  const unit = 2 * h / 200;
  const y0w = -0.6 * h + Math.min(mb.y0, mb2.y0) * unit, y1w = -0.6 * h + (Math.max(mb.y1, mb2.y1) + 1) * unit;
  const halfW = Math.max(Math.abs(-h + mb.x0 * unit), Math.abs(-h + (mb.x1 + 1) * unit), Math.abs(-h + mb2.x0 * unit), Math.abs(-h + (mb2.x1 + 1) * unit));
  const aspect = W / H;
  let span = (y1w - y0w) * 1.1;
  if (span * aspect < halfW * 2.1) span = halfW * 2.1 / aspect;
  const bottom = y0w - (y1w - y0w) * 0.04;
  for (let row = 0; row < 9; row++) {
    const top = row === 8;
    let cam;
    if (top) { const tw = h * 0.62 * (spec.topScale || 1); cam = new THREE.OrthographicCamera(-tw, tw, tw, -tw, 1, h * 30); cam.position.set(0, h * 6, h * 1.8); cam.lookAt(0, h * 0.35, 0); }
    else { cam = new THREE.OrthographicCamera(-span * aspect / 2, span * aspect / 2, bottom + span, bottom, 1, h * 30); cam.position.set(0, 0, h * 6); cam.lookAt(0, 0, 0); }
    for (let f = 0; f < F; f++) {
      pose(obj, spec.style || 'walk', f / F * Math.PI * 2, spec.armF);
      if (spec.crawl) { obj.rotation.x = 1.25; obj.position.set(0, 30, -170); }
      group.rotation.set(0, top ? Math.PI : -row * Math.PI / 4, 0);
      renderer.setSize(W, H);
      renderer.render(sc, cam);
      ctx.drawImage(renderer.domElement, f * W, row * H);
    }
  }
  return out.toDataURL('image/png');
};

window.armTest = async function () {
  const base = await loadFBX('x_kenney_animated-characters-1/Model/characterMedium.fbx');
  const map = await tex.loadAsync(M + 'x_kenney_animated-characters-1/Skins/zombieA.png'); map.colorSpace = THREE.SRGBColorSpace;
  const W = 140, H = 180;
  const combos = [[-1.45, 1.45, 'y'], [1.45, -1.45, 'y'], [-1.45, -1.45, 'y'], [1.45, 1.45, 'y'], [-1.3, 1.3, 'z'], [1.3, -1.3, 'z']];
  const out = document.createElement('canvas'); out.width = W * combos.length; out.height = H * 2; const ctx = out.getContext('2d');
  renderer.setSize(W, H);
  for (let i = 0; i < combos.length; i++) {
    const obj = SkeletonUtils.clone(base);
    obj.traverse(o => { if (o.isMesh) { o.material = new THREE.MeshStandardMaterial({ map }); o.frustumCulled = false; } });
    const b = bones(obj);
    const [l, r, ax] = combos[i];
    b.LeftArm.rotation[ax] += l; b.RightArm.rotation[ax] += r;
    const sc = scene(); sc.add(obj);
    for (let row = 0; row < 2; row++) {
      const h = 380, cam = new THREE.OrthographicCamera(-h * 0.6, h * 0.6, h * 1.05, -h * 0.1, 1, h * 20);
      if (row === 0) cam.position.set(0, 0, h * 5); else cam.position.set(h * 5, 0, 0);
      cam.lookAt(0, 0, 0);
      renderer.render(sc, cam); ctx.drawImage(renderer.domElement, i * W, row * H);
    }
  }
  return out.toDataURL();
};

window.armTest2 = async function () {
  const base = await loadFBX('x_kenney_animated-characters-1/Model/characterMedium.fbx');
  const map = await tex.loadAsync(M + 'x_kenney_animated-characters-1/Skins/zombieA.png'); map.colorSpace = THREE.SRGBColorSpace;
  const W = 110, H = 150;
  const combos = [];
  for (const bn of ['LeftArm', 'RightArm']) for (const ax of ['x', 'y', 'z']) for (const sg of [1, -1]) combos.push([bn, ax, sg * 1.3]);
  const out = document.createElement('canvas'); out.width = W * combos.length; out.height = H * 2; const ctx = out.getContext('2d');
  renderer.setSize(W, H);
  for (let i = 0; i < combos.length; i++) {
    const obj = SkeletonUtils.clone(base);
    obj.traverse(o => { if (o.isMesh) { o.material = new THREE.MeshStandardMaterial({ map, color: 0xffffff }); o.frustumCulled = false; } });
    const b = bones(obj);
    const [bn, ax, v] = combos[i];
    b[bn].rotation[ax] += v;
    // mark the left hand red so we can tell the arms apart
    const sc = scene(); sc.add(obj);
    const mk = new THREE.Mesh(new THREE.SphereGeometry(20), new THREE.MeshBasicMaterial({ color: 0xff0000 })); b.LeftHand.add(mk);
    for (let row = 0; row < 2; row++) {
      const h = 380, cam = new THREE.OrthographicCamera(-h * 0.6, h * 0.6, h * 0.6, -h * 0.6, 1, h * 20);
      if (row === 0) { cam.position.set(0, h * 0.6, h * 5); cam.lookAt(0, h * 0.6, 0); }
      else { cam.position.set(0, h * 5, 0.01); cam.up.set(0, 0, -1); cam.lookAt(0, 0, 0); }
      renderer.render(sc, cam); ctx.drawImage(renderer.domElement, i * W, row * H);
    }
  }
  return out.toDataURL();
};
