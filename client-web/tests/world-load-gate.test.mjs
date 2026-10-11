import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

async function sourceModule(filename) {
  const source = readFileSync(new URL(filename, import.meta.url), 'utf8');
  const js = ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
  } }).outputText;
  return import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}`);
}
const { WorldLoadGate } = await sourceModule('../src/renderer/WorldLoadGate.ts');
const { decodeGamePacket } = await sourceModule('../src/networking/GameProtocol.ts');

test('login stays black across gaps in the stream and until textures and attachments finish', () => {
  const gate = new WorldLoadGate();
  assert.equal(gate.update(false), true);
  gate.setTransferState(false, true);
  assert.equal(gate.update(false), true); // Empty local queue between network packets.
  gate.setTransferState(true, false);
  assert.equal(gate.update(true), true); // Server finished, textures still preparing.
  assert.equal(gate.update(false), false);
});

test('normal chunk refreshes stay visible while teleports hide again', () => {
  const gate = new WorldLoadGate();
  gate.setTransferState(true, false);
  assert.equal(gate.update(false), false);
  gate.setTransferState(false, false);
  assert.equal(gate.update(true), false);
  gate.setTransferState(false, true);
  assert.equal(gate.update(false), true);
  gate.setTransferState(false, false); // The next server tick refreshes the window.
  assert.equal(gate.update(false), true);
  gate.setTransferState(true, false);
  assert.equal(gate.update(true), true);
  assert.equal(gate.update(false), false);
});

test('an empty destination can finish and a second teleport supersedes completion', () => {
  const gate = new WorldLoadGate();
  gate.setTransferState(true, false);
  gate.setTransferState(false, true);
  assert.equal(gate.update(false), true);
  gate.setTransferState(true, false);
  assert.equal(gate.update(false), false);
});

test('map sync state packet carries completion and blackout flags', () => {
  assert.deepEqual(decodeGamePacket(Uint8Array.from([26, 0, 1]).buffer),
    { type: 'mapSyncState', complete: false, hideWorld: true });
  assert.deepEqual(decodeGamePacket(Uint8Array.from([26, 1, 0]).buffer),
    { type: 'mapSyncState', complete: true, hideWorld: false });
});
