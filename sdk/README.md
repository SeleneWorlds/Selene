# Selene UI SDKs

- `selene`: framework independent TypeScript contract for the browser client bundle UI API. No Vue dependency. `selene/mock` supplies a development mock.
- `selene-vue`: Vue 3 `useSelene()`, `seleneKey`, and `provideSelene(app, api)`. Re-exports core types and the mock for convenient migration.

These are npm packages for browser bundle UIs, not a server connection library for Node.js. The client passes the API to the bundle's `mount(root, selene)` export.

Install the SDKs directly from GitHub with pnpm (11.5.2 is pinned in the consuming projects):

```sh
pnpm add 'selene@github:SeleneWorlds/Selene#11e318c90a397d0d0191bf9567e0dce97efceed4&path:/sdk/selene'
pnpm add 'selene-vue@github:SeleneWorlds/Selene#main&path:/sdk/selene-vue'
```

[pnpm's `path:` selector](https://pnpm.io/package-sources#install-from-a-subdirectory-of-a-git-repository) installs a package from a repository subdirectory. These Git specifiers require pnpm; npm does not support this subdirectory syntax. Run `pnpm install` and `pnpm build` in consuming UI projects. No adjacent Selene checkout or prebuilt SDK `dist` directories are needed.

The core dependency is pinned to a GitHub commit. The Vue SDK follows `main` so the GitHub dependency fix can be adopted once pushed. After publishing this change, run `pnpm install` in each bundle to regenerate its lockfile, then pin the Vue dependency to the resulting commit as well for a fixed source revision. Do not use the old npm lockfile for SDK installation. Existing bundle lockfiles still describe local SDK paths until this first successful GitHub install; frozen installs require refreshed lockfiles.

Git dependencies run the package's `prepack` build automatically. Each consuming project's `pnpm-workspace.yaml` explicitly allows builds for `selene` and `selene-vue`. Keep these settings when copying the dependency declarations to another UI project:

```yaml
allowBuilds:
  selene: true
  selene-vue: true
```

For SDK development:

```sh
cd sdk/selene
pnpm install
pnpm build
cd ../selene-vue
pnpm install
pnpm test
```

The Vue SDK's development dependency also comes from GitHub. Both packages are ESM and ship JavaScript and declarations. Vue and the core SDK are peers of `selene-vue` so applications share their instances. Registry publication is not required.

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
