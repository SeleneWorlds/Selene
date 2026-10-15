# Selene UI SDKs

- `selene`: framework independent TypeScript contract for the browser client bundle UI API. No Vue dependency. `selene/mock` supplies a development mock.
- `selene-vue`: Vue 3 `useSelene()`, `seleneKey`, and `provideSelene(app, api)`. Re-exports core types and the mock for convenient migration.

These are npm packages for browser bundle UIs, not a server connection library for Node.js. The client passes the API to the bundle's `mount(root, selene)` export.

Build the core before the Vue SDK, then install/build the consuming UI:

```sh
cd sdk/selene
npm install
npm run build
cd ../selene-vue
npm install
npm run build
npm test
```

The repository consumers use local `file:` dependencies. Keep both SDK `dist` directories built before installing consumers. Rebuild SDKs after changes. Generated files are ignored; `npm pack` runs the package build and includes JavaScript and declarations. Both packages are ESM. Vue and the core SDK are peers of `selene-vue` so applications share their instances. Registry publication is not configured.

```ts
import type { SeleneUiApi } from "selene";
import { createApp } from "vue";
import { provideSelene } from "selene-vue";
import App from "./App.vue";

export function mount(root: ShadowRoot, api: SeleneUiApi) {
  const app = createApp(App);
  provideSelene(app, api);
  app.mount(root.querySelector("#app")!);
}
```

In components, import `useSelene` from `selene-vue`. In plain TypeScript modules, import types from `selene`. For standalone development, call `createMockSeleneUiApi()` from `selene/mock`. The mock uses browser storage, serves assets/registries from the development server, logs outgoing payloads, and returns empty world state; it does not emulate a game server.

The shared symbol uses `Symbol.for` so independently built component libraries agree on injection identity. When bundling shared components, deduplicate Vue in Vite (`resolve.dedupe: ['vue']`). Existing bundle `selene.ts` files remain as compatibility re-exports with no API definitions or mock implementations.

`BundleUiManager` implements the shared contract. Update the contract, mock, and `SELENE_UI_API_VERSION` together when changing the host API.
