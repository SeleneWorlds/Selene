import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('../src/ui/BundleUiManager.ts', import.meta.url), 'utf8');
const start = source.indexOf('request: async (path: string');
const end = source.indexOf('\n        },', start);
const method = source.slice(start + 'request: '.length, end + '\n        }'.length);
const compiled = ts.transpileModule(`const request = ${method};`, {
  compilerOptions: { target: ts.ScriptTarget.ES2022 },
}).outputText;

test('schema GET passes the bridge and preserves provider query parameters', async () => {
  const calls = [];
  const request = new Function('resolveServerUrl', 'fetch', `${compiled}\nreturn request;`).call(
    { options: { serverApiUrl: 'https://example.test', authToken: 'test-token' } },
    (base, path) => base + path,
    async (url, options) => {
      calls.push({ url, options });
      return { ok: true, json: async () => ({ actions: { type: 'array' } }) };
    },
  );
  assert.deepEqual(await request('/scripts/schema', { module: 'test.provider', functionName: 'schema' }),
    { actions: { type: 'array' } });
  assert.equal(calls[0].url.searchParams.get('module'), 'test.provider');
  assert.equal(calls[0].url.searchParams.get('functionName'), 'schema');
  assert.equal(calls[0].options.headers.Authorization, 'Bearer test-token');
  for (const [path, method] of [['/scripts/schema', 'POST'], ['/scripts/schema/extra', 'GET'], ['/scripts/%2e%2e', 'GET']]) {
    await assert.rejects(request(path, {}, method), /Invalid resource HTTP path or method/);
  }
  assert.equal(calls.length, 1);
});

test('native bridge also explicitly allows schema GET requests', () => {
  const native = readFileSync(new URL('../../client/src/main/kotlin/com/seleneworlds/client/ui/cef/CefUiBridge.kt', import.meta.url), 'utf8');
  const getRules = native.slice(native.indexOf('"GET" -> listOf('), native.indexOf('"POST" -> listOf('));
  assert.ok(getRules.includes('"/scripts/schema"'));
});
