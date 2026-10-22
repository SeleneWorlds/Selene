import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('../src/renderer/PixiViewport.ts', import.meta.url), 'utf8');
const js = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
} }).outputText;
const { PixiViewport } = await import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}`);

globalThis.ResizeObserver = class { observe() {} };

function setup(hostWidth, hostHeight, width, height, fit) {
  const host = { clientWidth: hostWidth, clientHeight: hostHeight };
  const app = { canvas: { style: {} }, renderer: { resize() {} } };
  const ui = { style: {} };
  const viewport = new PixiViewport(app, host, ui, { setViewport() {} }, { update() {} }, () => {});
  viewport.setAspectRatio(4, 3);
  viewport.setLogicalRenderSize(width, height);
  viewport.setFitToScreen(fit);
  viewport.initialize();
  return { viewport, canvas: app.canvas.style, ui: ui.style };
}

test('large render surfaces always fit, with matching UI scale and pointer conversion', () => {
  for (const fit of [false, true]) {
    const { viewport, canvas, ui } = setup(1200, 900, 2048, 1536, fit);
    assert.equal(canvas.width, '1200px');
    assert.equal(canvas.height, '900px');
    assert.equal(ui.transform, `scale(${1200 / 2048})`);
    assert.deepEqual(viewport.screenToLogical(600, 450), { x: 1024, y: 768 });
  }
});

test('Fit on Screen controls enlargement of smaller render surfaces', () => {
  const { viewport, canvas } = setup(1600, 1200, 1024, 768, false);
  assert.equal(canvas.width, '1024px');
  assert.equal(canvas.height, '768px');
  assert.equal(canvas.left, '288px');
  assert.equal(canvas.top, '216px');
  assert.deepEqual(viewport.screenToLogical(800, 600), { x: 512, y: 384 });
  viewport.setFitToScreen(true);
  assert.equal(canvas.width, '1600px');
  assert.equal(canvas.height, '1200px');
});

test('shrinking preserves proportions in wide and tall windows', () => {
  for (const [width, height] of [[1200, 600], [600, 1200]]) {
    const { canvas } = setup(width, height, 1771, 1328, false);
    assert.ok(parseFloat(canvas.width) <= width);
    assert.ok(parseFloat(canvas.height) <= height);
    assert.ok(Math.abs(parseFloat(canvas.width) / parseFloat(canvas.height) - 1771 / 1328) < 1e-9);
  }
});
