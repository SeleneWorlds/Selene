import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('../src/ui/settings.ts', import.meta.url), 'utf8');
const js = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
} }).outputText;
let moduleId = 0;
async function settingsWith(stored) {
  let saved = stored;
  globalThis.window = { localStorage: {
    getItem: () => saved,
    setItem: (_key, value) => { saved = value; },
  } };
  const settings = await import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}#${moduleId++}`);
  await settings.initializeSettings();
  return { settings, stored: () => JSON.parse(saved) };
}

test('older settings default smooth lighting to off and retain display preferences', async () => {
  const { settings } = await settingsWith(JSON.stringify({ fitToScreen: false, tileAnimations: false }));
  assert.equal(settings.loadSettings().smoothLighting, false);
  assert.equal(settings.loadSettings().fitToScreen, false);
  assert.equal(settings.loadSettings().tileAnimations, false);
});

test('smooth lighting persists both toggle states across reloads', async () => {
  const first = await settingsWith(null);
  first.settings.saveSettings({ ...first.settings.loadSettings(), smoothLighting: true });
  assert.equal(first.stored().smoothLighting, true);
  const reloaded = await settingsWith(JSON.stringify(first.stored()));
  assert.equal(reloaded.settings.loadSettings().smoothLighting, true);
  reloaded.settings.saveSettings({ ...reloaded.settings.loadSettings(), smoothLighting: false });
  assert.equal(reloaded.stored().smoothLighting, false);
});

test('invalid persisted smooth lighting values use the default', async () => {
  const { settings } = await settingsWith(JSON.stringify({ smoothLighting: 'true' }));
  assert.equal(settings.loadSettings().smoothLighting, false);
});
