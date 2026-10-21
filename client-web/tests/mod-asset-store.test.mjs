import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('../src/core/services/ModAssetStore.ts', import.meta.url), 'utf8')
  .replace("import { unzip } from 'fflate';", 'const unzip = () => {};')
  .replace("import { validateRegistryEntries } from '@/data/ClientRegistrySchemas';", 'const validateRegistryEntries = (_name, entries) => entries;');
const js = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
} }).outputText;
const store = await import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}`);

test('disabled mods retain files and priority, survive reload, and stop overriding assets and registries', async () => {
  let mods = [{ id: 'top', name: 'Top', fileCount: 2 }, { id: 'base', name: 'Base', fileCount: 1 }];
  const path = 'common/data/example/items/test.json';
  const files = new Map([
    ['top', new Map([[path, new Blob(['{"value":"top"}'])], ['top-only.png', new Blob(['image'])]])],
    ['base', new Map([[path, new Blob(['{"value":"base"}'])]])],
  ]);
  const request = (result) => {
    const req = { result };
    queueMicrotask(() => req.onsuccess?.());
    return req;
  };
  const database = { transaction() {
    const tx = { objectStore(name) { return {
      get(key) { return request(name === 'metadata' ? structuredClone(mods) : { blob: files.get(key[0]).get(key[1]) }); },
      getAllKeys() { return request([...files].flatMap(([id, entries]) => [...entries.keys()].map((p) => [id, p]))); },
      put(value) { mods = structuredClone(value); queueMicrotask(() => tx.oncomplete?.()); },
    }; } };
    return tx;
  } };
  const original = globalThis.indexedDB;
  globalThis.indexedDB = { open: () => request(database) };
  try {
    await store.initializeModAssets();
    assert.equal(store.getInstalledMods()[0].enabled, true); // Existing installations stay enabled.
    await store.setModEnabled('top', false);
    await store.initializeModAssets();
    assert.deepEqual(store.getInstalledMods().map((m) => [m.id, m.enabled, m.fileCount]), [['top', false, 2], ['base', true, 1]]);
    assert.equal(store.hasModAsset('top-only.png'), false);
    assert.deepEqual(store.listModAssetPaths(), [path]);
    assert.equal(await store.getModAsset('top-only.png'), null);
    assert.equal(await (await store.getModAsset(path)).text(), '{"value":"base"}');
    const snapshot = { registries: { items: { entries: {} } } };
    assert.equal((await store.applyModRegistryOverrides(snapshot)).registries.items.entries['example:test'].value, 'base');
    await store.setModEnabled('top', true);
    assert.equal(store.hasModAsset('top-only.png'), true);
    assert.equal((await store.applyModRegistryOverrides(snapshot)).registries.items.entries['example:test'].value, 'top');
    assert.deepEqual(snapshot.registries.items.entries, {});
  } finally {
    globalThis.indexedDB = original;
  }
});
