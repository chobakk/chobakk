// Renders all sprite sheets. Usage: node run.js <outDir> <jsonSpecFile>
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const [out, specFile] = process.argv.slice(2);
  const specs = JSON.parse(fs.readFileSync(specFile, 'utf8'));
  fs.mkdirSync(out, { recursive: true });
  const b = await chromium.launch({ args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  const p = await b.newPage();
  p.on('console', m => { if (m.type() === 'error') console.log('page error:', m.text()); });
  p.on('pageerror', e => console.log('pageerror:', e.message));
  await p.goto('http://127.0.0.1:8765/render/index.html');
  await p.waitForFunction(() => window.ready === true, null, { timeout: 30000 });
  const save = (name, url) => fs.writeFileSync(path.join(out, name), Buffer.from(url.split(',')[1], 'base64'));
  for (const s of specs) {
    const t0 = Date.now();
    if (s.kind === 'char') save(s.name + '.png', await p.evaluate(sp => window.renderCharacter(sp), s));
    else if (s.kind === 'creature') save(s.name + '.png', await p.evaluate(sp => window.renderCreature(sp), s));
    else { const r = await p.evaluate(sp => window.renderGun(sp), s); save(s.name + '_side.png', r[0]); save(s.name + '_top.png', r[1]); save(s.name + '_fp.png', r[2]); console.log(s.name, 'materials', r[3]); }
    console.log('rendered', s.name, (Date.now() - t0) + 'ms');
  }
  await b.close();
})();
