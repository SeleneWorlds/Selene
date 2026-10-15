import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

function moduleUrl(path, replacements = {}) {
  let source = readFileSync(new URL(path, import.meta.url), 'utf8');
  let js = ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
  } }).outputText;
  for (const [from, to] of Object.entries(replacements)) js = js.replaceAll(from, to);
  return `data:text/javascript;base64,${Buffer.from(js).toString('base64')}`;
}
const modelUrl = moduleUrl('../src/ui/KeyboardControls.ts');
const { KeyboardControls, parseBinding, bindingMatches } = await import(modelUrl);
const chord = (key, modifiers = {}) => ({ key, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false, ...modifiers });
const keyEvent = (key, modifiers = {}) => ({ ...chord(key, modifiers), repeat: false,
  preventDefault() { this.defaultPrevented = true; }, stopImmediatePropagation() { this.stopped = true; } });
const admin = { id: 'admin.toggle', label: 'Admin menu', defaultBinding: 'F2', allowInText: true };
function storage(initial = {}) {
  let value = JSON.stringify(initial);
  return { getItem: () => value, setItem: (_key, next) => { value = next; } };
}

test('rebinding and unbinding replace defaults and survive a page reload', () => {
  const store = storage();
  const controls = new KeyboardControls(store);
  controls.registerBinding(admin);
  controls.setBinding(admin.id, 'Ctrl+F7');
  assert.equal(controls.matchesBinding(admin.id, chord('F2')), false);
  assert.equal(controls.matchesBinding(admin.id, chord('F7', { ctrlKey: true })), true);
  const restored = new KeyboardControls(store);
  restored.registerBinding(admin);
  assert.equal(restored.matchesBinding(admin.id, chord('F7', { ctrlKey: true })), true);
  restored.setBinding(admin.id, null);
  const unbound = new KeyboardControls(store);
  unbound.registerBinding(admin);
  assert.equal(unbound.matchesBinding(admin.id, chord('F2')), false);
  unbound.reset(admin.id);
  assert.equal(unbound.matchesBinding(admin.id, chord('F2')), true);
});

test('conflicts require unbinding first and Ctrl+, stays available', () => {
  const controls = new KeyboardControls();
  controls.registerBinding(admin);
  controls.registerBinding({ id: 'editor', label: 'Editor', defaultBinding: 'F1' });
  assert.throws(() => controls.setBinding('editor', 'F2'), /Admin menu/);
  assert.throws(() => controls.setBinding('editor', 'Ctrl+,'), /reserved/);
  controls.setBinding(admin.id, null);
  controls.setBinding('editor', 'F2');
  assert.throws(() => controls.reset(admin.id), /Editor/);
  controls.reset();
  assert.equal(controls.matchesBinding(admin.id, chord('F2')), true);
  assert.equal(controls.matchesBinding('editor', chord('F1')), true);
});

test('exact modifiers separate admin from chat macros and normalize letter case', () => {
  assert.equal(bindingMatches(parseBinding('F2'), chord('F2', { ctrlKey: true })), false);
  assert.equal(bindingMatches(parseBinding('Ctrl+a'), chord('A', { ctrlKey: true })), true);
  assert.equal(bindingMatches(parseBinding('Ctrl+a'), chord('A', { ctrlKey: true, shiftKey: true })), false);
  const controls = new KeyboardControls();
  controls.registerBinding({ id: 'macro', label: 'Macro', defaultBinding: 'F3' });
  assert.throws(() => controls.setBinding('macro', 'Shift+ArrowUp'), /Move north/);
  assert.throws(() => controls.setBinding('move.up', 'Shift+w'), /turn/);
});

test('dispatch respects focused text, action availability, repeats, and an open client menu', () => {
  const controls = new KeyboardControls();
  let calls = 0;
  controls.registerBinding({ id: 'select', label: 'Text shortcut', defaultBinding: 'Ctrl+m' }, () => { calls++; });
  assert.equal(controls.dispatch(keyEvent('m', { ctrlKey: true }), true), false);
  controls.registerBinding(admin, () => { calls++; });
  const pressed = keyEvent('F2');
  assert.equal(controls.dispatch(pressed, true), true);
  assert.equal(calls, 1);
  assert.equal(pressed.stopped, true);
  controls.dispatch({ ...keyEvent('F2'), repeat: true }, false);
  assert.equal(calls, 1);
  controls.blocked = true;
  assert.equal(controls.dispatch(keyEvent('F2'), false), false);
  controls.blocked = false;
  controls.registerBinding({ id: 'unavailable', label: 'Unavailable', defaultBinding: 'F9' }, () => false);
  assert.equal(controls.dispatch(keyEvent('F9'), false), false);
});

test('failed storage leaves active bindings unchanged and invalid saved keys are ignored', () => {
  const controls = new KeyboardControls({ getItem: () => '{"admin.toggle":"Ctrl+Ctrl+x","move.up":null}',
    setItem() { throw new Error('Storage full'); } });
  controls.registerBinding(admin);
  assert.equal(controls.matchesBinding(admin.id, chord('F2')), true);
  assert.equal(controls.matchesBinding('move.up', chord('ArrowUp')), false);
  assert.throws(() => controls.setBinding(admin.id, 'F7'), /Storage full/);
  assert.equal(controls.matchesBinding(admin.id, chord('F2')), true);
});

test('disposal releases a shortcut while retaining its saved binding for remount', () => {
  const controls = new KeyboardControls();
  const dispose = controls.registerBinding(admin);
  controls.setBinding(admin.id, 'F7');
  dispose();
  assert.equal(controls.matchesBinding(admin.id, chord('F7')), false);
  controls.registerBinding(admin);
  assert.equal(controls.matchesBinding(admin.id, chord('F7')), true);
});

test('common hardcoded combinations cannot be assigned', () => {
  const controls = new KeyboardControls();
  controls.registerBinding(admin);
  assert.equal(controls.matchesBinding(admin.id, chord('F2')), true);
  for (const binding of ['F5', 'Ctrl+Enter', 'Ctrl+Shift+Enter', 'Enter', 'Shift+Enter',
    'Ctrl+a', 'Meta+a', 'Ctrl+s', 'Meta+s', 'Escape', 'Shift+Escape']) {
    assert.throws(() => controls.setBinding(admin.id, binding), /reserved/);
    assert.throws(() => controls.registerBinding({ id: 'fixed', label: 'Fixed', defaultBinding: binding }), /reserved/);
  }
  assert.equal(controls.list().some(action => ['admin.execute', 'chat.send', 'chat.select', 'editor.save', 'game.menu'].includes(action.id)), false);
});

let fixtureIndex = 0;
async function inputFixture() {
  const singletonUrl = `data:text/javascript;base64,${Buffer.from(
    `import { KeyboardControls } from '${modelUrl}'; export const keyboardControls = new KeyboardControls(); // ${fixtureIndex++}`,
  ).toString('base64')}`;
  const { keyboardControls: controls } = await import(singletonUrl);
  const { BundleUiInputClaims } = await import(moduleUrl('../src/ui/BundleUiInputClaims.ts', {
    './controls': singletonUrl,
  }));
  const { BrowserInputManager } = await import(moduleUrl('../src/core/BrowserInputManager.ts', {
    '@/ui/controls': singletonUrl, '@/ui/KeyboardControls': modelUrl,
  }));
  const listeners = new Map();
  globalThis.window = { addEventListener: (type, listener) => listeners.set(type, listener) };
  globalThis.document = { activeElement: null };
  globalThis.HTMLElement = class {};
  globalThis.HTMLInputElement = class extends HTMLElement {};
  globalThis.HTMLTextAreaElement = class extends HTMLElement {};
  globalThis.HTMLSelectElement = class extends HTMLElement {};
  const claims = new BundleUiInputClaims();
  const host = { addEventListener() {}, tabIndex: 0 };
  const input = new BrowserInputManager(host, claims);
  const send = (type, key, code) => listeners.get(type)({ ...keyEvent(key), code });
  return { controls, claims, input, send };
}

test('rebound movement reaches Lua polling, preserves Shift-turning, and releases correctly', async () => {
  const { controls, claims, input, send } = await inputFixture();
  claims.captureText();
  controls.setBinding('move.up', 'w');
  let moves = 0;
  input.bindContinuousAction('keyboard', 'Up', () => { moves++; });
  send('keydown', 'ArrowUp', 'ArrowUp');
  input.update();
  assert.equal(moves, 0);
  send('keydown', 'w', 'KeyW');
  input.update();
  assert.equal(moves, 1);
  assert.equal(input.isKeyPressed('Up'), true);
  send('keydown', 'Shift', 'ShiftLeft');
  assert.equal(input.isKeyPressed('Up'), true);
  assert.equal(input.isKeyPressed('L-Shift'), true);
  send('keyup', 'W', 'KeyW');
  assert.equal(input.isKeyPressed('Up'), false);
});

test('focused text accepts movement letters and opening the menu clears held keys', async () => {
  const { controls, claims, input, send } = await inputFixture();
  controls.setBinding('move.up', 'w');
  send('keydown', 'w', 'KeyW');
  assert.equal(input.isKeyPressed('Up'), true);
  const closedRoot = { host: new HTMLElement(), activeElement: new HTMLInputElement() };
  claims.registerRoot(closedRoot);
  assert.equal(controls.hasEditableFocus(), true);
  assert.equal(input.isKeyPressed('Up'), false);
  assert.equal(claims.isPassthroughKey('w'), false);
  closedRoot.activeElement = null;
  controls.blocked = true;
  input.update();
  controls.blocked = false;
  assert.equal(input.isKeyPressed('Up'), false);
  send('keydown', 'w', 'KeyW');
  controls.setBinding('move.up', null);
  assert.equal(input.isKeyPressed('Up'), false);
});
