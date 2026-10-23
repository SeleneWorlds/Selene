import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('../src/renderer/LightingEnvironment.ts', import.meta.url), 'utf8');
const js = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
} }).outputText;
const { LightingEnvironment } = await import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}`);

function environment() {
  const lighting = new LightingEnvironment();
  lighting.setAmbientLight('outdoors', { red: 0, green: 0, blue: 0 });
  return lighting;
}
const white = { red: 1, green: 1, blue: 1 };

test('light reaches zero continuously with a gentle slope at its radius', () => {
  for (const radius of [0.25, 1, 4, 16, 32]) {
    const lighting = environment();
    lighting.setTileLight({ x: 0, y: 0, z: 0 }, { ...white, radius, intensity: 1 });
    const at = distance => lighting.getColor({ x: distance, y: 0, z: 0 }).red;
    assert.equal(at(radius), 0);
    assert.equal(at(radius + 0.001), 0);
    assert.ok(at(radius * (1 - 0.001)) < 0.000004, 'no residual brightness at the cutoff');
    assert.ok(at(radius * (1 - 0.0005)) < at(radius * (1 - 0.001)) * 0.26,
      'the outer slope flattens toward zero');
  }
});

test('falloff preserves source brightness and decreases monotonically', () => {
  const lighting = environment();
  lighting.setTileLight({ x: 0, y: 0, z: 0 }, { red: 0.8, green: 0.4, blue: 0.2, radius: 5, intensity: 0.5 });
  assert.deepEqual(lighting.getColor({ x: 0, y: 0, z: 0 }), { red: 0.4, green: 0.2, blue: 0.1 });
  let previous = 1;
  for (let i = 0; i <= 100; i++) {
    const color = lighting.getColor({ x: i / 20, y: 0, z: 0 });
    assert.ok(color.red <= previous);
    assert.ok(color.red >= 0);
    previous = color.red;
  }
});

test('moving an entity light across a sample radius does not cause a brightness pop', () => {
  const lighting = environment();
  const sample = { x: 16, y: 0, z: 0 };
  const light = { ...white, radius: 4, intensity: 1 };
  let previous = null;
  for (let i = 0; i <= 100; i++) {
    lighting.setEntityLight(1, { x: 11.99 + i * 0.0002, y: 0, z: 0 }, light);
    const brightness = lighting.getColor(sample).red;
    if (previous !== null) assert.ok(Math.abs(brightness - previous) < 0.000001);
    previous = brightness;
  }
  assert.ok(previous > 0);
});

test('light cutoff retains ambient color', () => {
  const lighting = environment();
  const ambient = { red: 0.2, green: 0.1, blue: 0.05 };
  lighting.setAmbientLight('outdoors', ambient);
  lighting.setTileLight({ x: 0, y: 0, z: 0 }, { ...white, radius: 3, intensity: 1 });
  assert.deepEqual(lighting.getColor({ x: 3, y: 0, z: 0 }), ambient);
  assert.deepEqual(lighting.getColor({ x: 3.01, y: 0, z: 0 }), ambient);
});
