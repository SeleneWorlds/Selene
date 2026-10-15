import { inject, type App, type InjectionKey } from "vue";
import type { SeleneUiApi } from "selene";

export * from "selene";
export { createMockSeleneUiApi } from "selene/mock";

// Stable across independently built bundles and shared component libraries.
export const seleneKey: InjectionKey<SeleneUiApi> = Symbol.for("selene.ui.api");

export function provideSelene(app: App, api: SeleneUiApi): void {
  app.provide(seleneKey, api);
}

export function useSelene(): SeleneUiApi {
  const api = inject(seleneKey);
  if (!api) throw new Error("Selene API was not provided.");
  return api;
}
