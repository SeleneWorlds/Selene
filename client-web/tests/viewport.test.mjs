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
  const host = { clientWidth: hostWidth, clientHeight: hostHeight,
    getBoundingClientRect: () => ({ left: 0, top: 0 }) };
  const app = { canvas: { style: {} }, renderer: { resize() {} } };
  const ui = { style: {} };
  const viewport = new PixiViewport(app, host, ui, { setViewport() {} }, { update() {} }, () => {});
  viewport.setAspectRatio(4, 3);
  viewport.setLogicalRenderSize(width, height);
  viewport.setFitToScreen(fit);
  viewport.initialize();
  return { viewport, host, canvas: app.canvas.style, ui: ui.style };
}

test('large render surfaces fit while UI retains screen pixels and world pointers use logical pixels', () => {
  for (const fit of [false, true]) {
    const { viewport, canvas, ui } = setup(1200, 900, 2048, 1536, fit);
    assert.equal(canvas.width, '1200px');
    assert.equal(canvas.height, '900px');
    assert.equal(ui.transform, 'none');
    assert.equal(ui.width, '1200px');
    assert.equal(ui.height, '900px');
    assert.deepEqual(viewport.screenToLogical(600, 450), { x: 1024, y: 768 });
  }
});

test('Fit on Screen controls enlargement of smaller render surfaces', () => {
  const { viewport, canvas, ui } = setup(1600, 1200, 1024, 768, false);
  assert.equal(canvas.width, '1024px');
  assert.equal(canvas.height, '768px');
  assert.equal(canvas.left, '288px');
  assert.equal(canvas.top, '216px');
  assert.equal(ui.left, canvas.left);
  assert.equal(ui.top, canvas.top);
  assert.equal(ui.width, canvas.width);
  assert.equal(ui.height, canvas.height);
  assert.deepEqual(viewport.screenToLogical(800, 600), { x: 512, y: 384 });
  viewport.setFitToScreen(true);
  assert.equal(canvas.width, '1600px');
  assert.equal(canvas.height, '1200px');
  assert.equal(ui.width, canvas.width);
  assert.equal(ui.height, canvas.height);
  assert.equal(ui.transform, 'none');
});

test('changing world resolution leaves dialog pixels and UI dimensions unchanged', () => {
  const { viewport, ui } = setup(1024, 768, 1024, 768, true);
  const original = { ...ui };
  viewport.setLogicalRenderSize(2048, 1536);
  assert.deepEqual(ui, original);
  assert.deepEqual(viewport.screenToLogical(350, 200), { x: 700, y: 400 });
  viewport.clearLogicalRenderSize();
  assert.deepEqual(ui, original);
});

test('shrinking preserves proportions in wide and tall windows', () => {
  for (const [width, height] of [[1200, 600], [600, 1200]]) {
    const { canvas } = setup(width, height, 1771, 1328, false);
    assert.ok(parseFloat(canvas.width) <= width);
    assert.ok(parseFloat(canvas.height) <= height);
    assert.ok(Math.abs(parseFloat(canvas.width) / parseFloat(canvas.height) - 1771 / 1328) < 1e-9);
  }
});

test('world overlay projection converts render pixels to fixed CSS pixels', () => {
  // 2048x1536 render surface displayed at 1024x768: using render pixels directly
  // positions a gizmo at twice the distance of its matching rendered tile.
  const { viewport, host } = setup(1200, 768, 2048, 1536, false);
  host.getBoundingClientRect = () => ({ left: 20, top: 40 });
  const coordinate = { x: 1180, y: 670 };
  const screen = viewport.logicalToScreen(coordinate);
  assert.deepEqual(screen, { x: 20 + 88 + 590, y: 40 + 335 });
  assert.deepEqual(viewport.screenToLogical(screen.x, screen.y), coordinate);
  const neighbor = viewport.logicalToScreen({ x: coordinate.x + 128, y: coordinate.y + 64 });
  assert.equal(neighbor.x - screen.x, 64);
  assert.equal(neighbor.y - screen.y, 32);
  viewport.setFitToScreen(true);
  assert.deepEqual(viewport.logicalToScreen(coordinate), screen);
});

test('render-to-screen conversion also handles enlarged viewports', () => {
  const { viewport } = setup(1600, 1200, 1024, 768, true);
  assert.deepEqual(viewport.logicalToScreen({ x: 512, y: 384 }), { x: 800, y: 600 });
});
