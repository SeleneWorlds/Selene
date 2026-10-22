import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

function moduleUrl(path, replacements = {}) {
  const source = readFileSync(new URL(path, import.meta.url), 'utf8');
  let js = ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
  } }).outputText;
  for (const [from, to] of Object.entries(replacements)) js = js.replaceAll(from, to);
  return `data:text/javascript;base64,${Buffer.from(js).toString('base64')}`;
}

const schemaUrl = moduleUrl('../src/data/ClientRegistrySchemas.ts', { "'zod'": JSON.stringify(import.meta.resolve('zod')) });
const renderGridUrl = moduleUrl('../src/core/RenderGrid.ts', { '@/data/ClientRegistrySchemas': schemaUrl });
const projectionUrl = moduleUrl('../src/core/WorldProjection.ts', { './RenderGrid': renderGridUrl });
const registryUrl = moduleUrl('../src/data/ClientRegistryLoader.ts', { './ClientRegistrySchemas': schemaUrl });
const { ClientGrid } = await import(moduleUrl('../src/core/ClientGrid.ts', {
  '@/core/WorldProjection': projectionUrl,
  '@/data/ClientRegistryLoader': registryUrl,
  './RenderGrid': renderGridUrl,
}));
const { ClientCamera } = await import(moduleUrl('../src/core/ClientCamera.ts', { '@/core/WorldProjection': projectionUrl }));
const iso = await import(moduleUrl('../src/renderer/IsoProjection.ts', {
  '@/core/WorldProjection': projectionUrl, '@/core/RenderGrid': renderGridUrl,
}));
const { validateRegistryEntries } = await import(schemaUrl);
const projection = await import(projectionUrl);

const defaults = { tileWidth: 76, tileHeight: 37, tileStepX: 38, tileStepY: 19, tileStepZ: 114, zSortScale: 500, rowSortScale: 50 };
const large = { tileWidth: 152, tileHeight: 74, tileStepX: 76, tileStepY: 38, tileStepZ: 228, zSortScale: 600, rowSortScale: 60 };

function createGrid(renderEntries) {
  return new ClientGrid({ hash: '', registries: {
    'selene:grids': { entries: {
      'test:default': { directions: [] }, 'test:large': { directions: [] },
    } },
    'selene:render_grids': { entries: validateRegistryEntries('selene:render_grids', renderEntries) },
  } });
}

test('active render grid drives projection, picking, camera conversion, dimensions, and sorting', () => {
  const grid = createGrid({ 'test:default': defaults, 'test:large': large });
  const camera = new ClientCamera();
  const coordinate = { x: 4, y: -2, z: 1 };
  for (const [identifier, definition] of [['test:large', large], ['test:default', defaults]]) {
    grid.applyGrid(identifier);
    const expected = { x: 2 * definition.tileStepX, y: -(6 * definition.tileStepY + definition.tileStepZ) };
    assert.deepEqual(projection.projectCoordinate(coordinate), expected);
    assert.deepEqual(grid.screenToCoordinate(expected.x, expected.y, coordinate.z), coordinate);
    camera.setCoordinate(coordinate);
    assert.deepEqual(camera.getPosition(), expected);
    assert.deepEqual(camera.setPosition(expected), coordinate);
    assert.equal(iso.TILE_WIDTH, definition.tileWidth);
    assert.equal(iso.TILE_HEIGHT, definition.tileHeight);
    assert.equal(iso.ROW_SORT_SCALE, definition.rowSortScale);
    assert.equal(iso.Z_SORT_SCALE, definition.zSortScale);
    assert.equal(iso.getSortLayer(coordinate, 3), (6 - definition.zSortScale) * definition.rowSortScale - 3);
  }
});

test('render grid validation supplies native defaults and rejects unusable projection values', () => {
  assert.deepEqual(validateRegistryEntries('render_grids', { 'test:default': {} })['test:default'], defaults);
  for (const invalid of [{ tileStepX: 0 }, { tileStepY: -1 }, { tileHeight: 0 }, { rowSortScale: 0 }]) {
    assert.throws(() => validateRegistryEntries('selene:render_grids', { 'test:bad': invalid }), /Invalid client registry data/);
  }
});

test('missing render definitions fail without changing the active projection', () => {
  const grid = createGrid({ 'test:default': defaults });
  grid.applyGrid('test:default');
  assert.throws(() => grid.applyGrid('test:large'), /Missing render grid definition/);
  assert.equal(iso.TILE_WIDTH, defaults.tileWidth);
});


test('render grid changes notify layout consumers and can be unsubscribed', async () => {
  const { onRenderGridChanged } = await import(renderGridUrl);
  const grid = createGrid({ 'test:default': defaults, 'test:large': large });
  let calls = 0;
  const release = onRenderGridChanged(() => { calls++; });
  grid.applyGrid('test:large');
  assert.equal(calls, 1);
  release();
  grid.applyGrid('test:default');
  assert.equal(calls, 1);
});
