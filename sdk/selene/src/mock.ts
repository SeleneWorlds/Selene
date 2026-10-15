import {
  SELENE_UI_API_VERSION,
  type SeleneUiApi,
  type VisualDefinition,
} from "./index.js";

export const createMockSeleneUiApi = (): SeleneUiApi => {
  let locale = navigator.language.replace(/-/g, "_");
  const localeListeners = new Set<(locale: string) => void>();
  const shortcutMatchers = new Map<string, (event: KeyboardEvent) => boolean>();
  const passthroughKeys = new Map<string, number>();

  return {
    apiVersion: SELENE_UI_API_VERSION,
    launch: {
      getParameters: () =>
        Object.fromEntries(new URL(window.location.href).searchParams),
    },
    http: {
      request: async (path, payload = {}, method = "GET") => {
        const url = new URL(path, window.location.href);
        if (method === "GET") {
          for (const [key, value] of Object.entries(payload)) {
            if (value !== undefined && value !== null) {
              url.searchParams.set(key, String(value));
            }
          }
        }
        const response = await fetch(url, {
          method,
          headers: { "Content-Type": "application/json" },
          ...(method === "GET" ? {} : { body: JSON.stringify(payload) }),
        });
        if (!response.ok) {
          throw new Error(`HTTP operation failed: ${response.status}`);
        }
        return response.json();
      },
    },
    registries: {
      search: async (registry, query, lookup = false) => {
        const response = await fetch(
          `/client/registries/${encodeURIComponent(registry.includes(":") ? registry : `selene:${registry}`)}`,
        );
        if (response.status === 404) {
          return null;
        }
        if (!response.ok) {
          throw new Error(`Registry lookup failed: ${response.status}`);
        }
        const snapshot = (await response.json()) as {
          entries: Record<string, Record<string, unknown>>;
        };
        const options = Object.entries(snapshot.entries)
          .flatMap(([value, fields]) => {
            const metadata =
              (fields.metadata as Record<string, unknown> | undefined) ?? {};
            const label = String(metadata.name ?? fields.name ?? value);
            const matches = lookup
              ? value.toLowerCase() === query.toLowerCase()
              : label.toLowerCase().includes(query.toLowerCase()) ||
                value.toLowerCase().includes(query.toLowerCase());
            const visual =
              typeof metadata.visual === "string"
                ? metadata.visual
                : typeof fields.visual === "string"
                  ? fields.visual
                  : registry === "entities" || registry === "selene:entities"
                    ? value
                    : null;
            return matches ? [{ value, label, visual }] : [];
          })
          .sort((a, b) => (a.label < b.label ? -1 : a.label > b.label ? 1 : 0))
          .slice(0, 50);
        return { registry, query, lookup, options };
      },
    },
    resolveAsset: async (path) => `/${path.replace(/^client\/ui\/dist\//, "")}`,
    i18n: {
      get: () => null,
      format: () => null,
      hasKey: () => false,
    },
    preferences: {
      getLocale: () => locale,
      setLocale: (value) => {
        locale = value;
        for (const listener of localeListeners) {
          listener(locale);
        }
      },
      onLocaleChanged: (listener) => {
        localeListeners.add(listener);
        return () => localeListeners.delete(listener);
      },
    },
    visuals: {
      getDefinition: async (identifier) => {
        const response = await fetch("/client/registries/selene:visuals");
        const snapshot = (await response.json()) as {
          entries?: Record<string, VisualDefinition>;
        };
        const definition = snapshot.entries?.[identifier];
        if (!definition) {
          throw new Error(`Visual not found: ${identifier}`);
        }
        return definition;
      },
    },
    storage: {
      load: async (key) =>
        window.localStorage.getItem(`selene.bundle.dev.${key}`),
      save: async (key, value) =>
        window.localStorage.setItem(`selene.bundle.dev.${key}`, value),
    },
    ui: {
      setBundleVisible: () => undefined,
    },
    input: {
      matchesBinding: (id, event) => shortcutMatchers.get(id)?.(event) ?? false,
      registerBinding: (definition, callback) => {
        const matches = (event: KeyboardEvent) => {
          const parts = definition.defaultBinding?.split("+");
          return (
            !!parts &&
            event.key.toLowerCase() === parts[parts.length - 1].toLowerCase() &&
            event.ctrlKey === parts.includes("Ctrl") &&
            event.altKey === parts.includes("Alt") &&
            event.metaKey === parts.includes("Meta") &&
            (definition.allowShift ||
              event.shiftKey === parts.includes("Shift"))
          );
        };
        shortcutMatchers.set(definition.id, matches);
        const listener = (event: KeyboardEvent) => {
          if (
            !event.repeat &&
            matches(event) &&
            callback &&
            callback(event) !== false
          ) {
            event.preventDefault();
            event.stopImmediatePropagation();
          }
        };
        window.addEventListener("keydown", listener, true);
        return () => {
          shortcutMatchers.delete(definition.id);
          window.removeEventListener("keydown", listener, true);
        };
      },
      captureKeys: () => () => undefined,
      captureText: () => () => undefined,
      passThroughKeys: (...keys) => {
        const uniqueKeys = new Set(keys);
        uniqueKeys.forEach((key) =>
          passthroughKeys.set(key, (passthroughKeys.get(key) ?? 0) + 1),
        );
        let active = true;
        return () => {
          if (!active) {
            return;
          }
          active = false;
          uniqueKeys.forEach((key) => {
            const count = passthroughKeys.get(key)! - 1;
            if (count) {
              passthroughKeys.set(key, count);
            } else {
              passthroughKeys.delete(key);
            }
          });
        };
      },
      isPassthroughKey: (key) => passthroughKeys.has(key),
      hasEditableFocus: () => {
        const active = document.activeElement;
        return (
          active instanceof HTMLInputElement ||
          active instanceof HTMLTextAreaElement ||
          active instanceof HTMLSelectElement ||
          (active instanceof HTMLElement && active.isContentEditable)
        );
      },
      onScroll: () => () => undefined,
      onPointerDown: () => () => undefined,
      onPointerMove: () => () => undefined,
      onPointerUp: () => () => undefined,
    },
    network: {
      onConnected: (callback) => {
        callback();
        return () => undefined;
      },
      sendToServer: (payloadId, payload) =>
        console.info("[Selene UI]", payloadId, payload),
      onPayload: () => () => undefined,
    },
    world: {
      setTileOcclusionEnabled: () => undefined,
      hasTileAt: async () => false,
      setLightingEnabled: async () => undefined,
      isLightingEnabled: () => true,
      setTileGridVisible: async () => undefined,
      setCameraZoom: async (zoom) => zoom,
      getCameraCoordinate: () => ({ x: 0, y: 0, z: 0 }),
      getCameraPosition: async () => ({ x: 0, y: 0 }),
      setCameraPosition: async () => ({ x: 0, y: 0, z: 0 }),
      setViewport: () => undefined,
      getControlledEntity: () => null,
      getMapTiles: () => [],
      projectCoordinate: () => ({ x: 0, y: 0 }),
      getEntitiesAt: async () => [],
      projectEntity: () => null,
      onCameraCoordinateChanged: () => () => undefined,
      onMapChanged: () => () => undefined,
    },
  };
};
