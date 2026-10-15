import assert from 'node:assert/strict';
import test from 'node:test';
import { createApp } from 'vue';
import { provideSelene, useSelene, seleneKey, SELENE_UI_API_VERSION } from '../dist/index.js';
import { createMockSeleneUiApi } from 'selene/mock';

test('independently loaded SDK copies share injection identity', async () => {
  const copy = await import('../dist/index.js?second-copy');
  assert.equal(copy.seleneKey, seleneKey);
  const app = createApp({});
  const api = createMockSeleneUiApi();
  provideSelene(app, api);
  assert.equal(app.runWithContext(() => copy.useSelene()), api);
});

test('missing provider gives an actionable error', () => {
  assert.throws(() => createApp({}).runWithContext(useSelene), /Selene API was not provided/);
});

test('development mock matches API version and releases overlapping key claims', () => {
  const api = createMockSeleneUiApi();
  assert.equal(api.apiVersion, SELENE_UI_API_VERSION);
  const first = api.input.passThroughKeys('Escape', 'Escape');
  const second = api.input.passThroughKeys('Escape');
  first();
  first();
  assert.equal(api.input.isPassthroughKey('Escape'), true);
  second();
  assert.equal(api.input.isPassthroughKey('Escape'), false);
  assert.deepEqual(api.world.getMapTiles(), []);
});

test('locale subscription can be removed', () => {
  const api = createMockSeleneUiApi();
  const received = [];
  const unsubscribe = api.preferences.onLocaleChanged(locale => received.push(locale));
  api.preferences.setLocale('de_DE');
  unsubscribe();
  api.preferences.setLocale('en_US');
  assert.deepEqual(received, ['de_DE']);
});
