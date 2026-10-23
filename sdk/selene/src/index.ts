export const SELENE_UI_API_VERSION = 22;

export type ClientNetworkPayload = Record<string, unknown>;
export interface Coordinate {
  x: number;
  y: number;
  z: number;
}
export interface MapTile extends Coordinate {
  tileIds: readonly number[];
  visualMetadata: Readonly<Record<string, unknown>>;
}
export interface WorldEntity {
  networkId: number;
  coordinate: Coordinate;
  tags: readonly string[];
  visual?: string | undefined;
  draggable: boolean;
  getComponent(name: string): unknown;
}
export interface SelenePointerEvent {
  clientX: number;
  clientY: number;
  button: number;
  shiftKey: boolean;
  coordinate: Coordinate;
}

export interface SeleneScrollEvent {
  amountY: number;
  clientX: number;
  clientY: number;
  coordinate: Coordinate;
}

export interface SeleneUiApi {
  readonly apiVersion: number;
  readonly launch: { getParameters(): Readonly<Record<string, string>> };
  readonly http: {
    request(
      path: string,
      payload?: ClientNetworkPayload,
      method?: "GET" | "POST" | "PUT",
    ): Promise<unknown>;
  };
  readonly registries: {
    search(
      registry: string,
      query: string,
      lookup?: boolean,
    ): Promise<Record<string, unknown> | null>;
  };
  readonly resolveAsset: (path: string) => Promise<string>;
  readonly i18n: {
    get(key: string, locale?: string): string | null;
    format(
      key: string,
      parameters?: Record<string, unknown>,
      locale?: string,
    ): string | null;
    hasKey(key: string, locale?: string): boolean;
  };
  readonly preferences: {
    getLocale(): string;
    setLocale(locale: string): void;
    onLocaleChanged(listener: (locale: string) => void): () => void;
  };
  readonly visuals: {
    getDefinition(identifier: string): Promise<VisualDefinition>;
  };
  readonly storage: {
    load(key: string): Promise<string | null>;
    save(key: string, value: string): Promise<void>;
  };
  readonly ui: {
    setBundleVisible(bundle: string, visible: boolean): void;
  };
  readonly input: {
    registerBinding(
      definition: {
        id: string;
        label: string;
        category?: string | undefined;
        defaultBinding: string | null;
        allowInText?: boolean | undefined;
        allowShift?: boolean | undefined;
      },
      callback?: (event: KeyboardEvent) => boolean | void,
    ): () => void;
    matchesBinding(id: string, event: KeyboardEvent): boolean;
    captureKeys(...keys: string[]): () => void;
    captureText(): () => void;
    passThroughKeys(...keys: string[]): () => void;
    isPassthroughKey(key: string): boolean;
    hasEditableFocus(): boolean;
    onScroll(callback: (event: SeleneScrollEvent) => void): () => void;
    onPointerDown(callback: (event: SelenePointerEvent) => void): () => void;
    onPointerMove(callback: (event: SelenePointerEvent) => void): () => void;
    onPointerUp(callback: (event: SelenePointerEvent) => void): () => void;
  };
  readonly network: {
    sendToServer(payloadId: string, payload?: ClientNetworkPayload): void;
    onPayload(
      payloadId: string,
      callback: (payload: ClientNetworkPayload) => void,
    ): () => void;
    onConnected(callback: () => void): () => void;
  };
  readonly world: {
    onRenderGridChanged(listener: () => void): () => void;
    getRenderGrid(): {
      tileWidth: number;
      tileHeight: number;
      tileStepX: number;
      tileStepY: number;
      tileStepZ: number;
    };
    setOffscreenRendering(width: number, height: number): void;
    setTileOcclusionEnabled(enabled: boolean): void;
    hasTileAt(coordinate: Coordinate): Promise<boolean>;
    setLightingEnabled(enabled: boolean): Promise<void>;
    isLightingEnabled(): boolean;
    setTileGridVisible(visible: boolean): Promise<void>;
    setCameraZoom(zoom: number): Promise<number>;
    getCameraCoordinate(): Coordinate;
    getCameraPosition(): Promise<{ x: number; y: number }>;
    setCameraPosition(position: { x: number; y: number }): Promise<Coordinate>;
    setViewport(x: number, y: number, width: number, height: number): void;
    getControlledEntity(): WorldEntity | null;
    getMapTiles(
      coordinate?: Coordinate,
      width?: number,
      height?: number,
    ): MapTile[];
    /** Convert offscreen render pixels to browser client pixels, including display scale and letterboxing. */
    logicalToScreen(point: { x: number; y: number }): { x: number; y: number };
    /** Project a world coordinate into offscreen render pixels. */
    projectCoordinate(coordinate: Coordinate): { x: number; y: number };
    getEntitiesAt(coordinate: Coordinate): Promise<WorldEntity[]>;
    projectEntity(networkId: number): { x: number; y: number } | null;
    onCameraCoordinateChanged(
      callback: (coordinate: Coordinate) => void,
    ): () => void;
    onMapChanged(
      callback: (coordinate: Coordinate, width: number, height: number) => void,
    ): () => void;
  };
}

export interface VisualFrameDefinition {
  texture?: string | undefined;
  duration?: number | undefined;
  speed?: number | undefined;
  offsetX?: number | undefined;
  offsetY?: number | undefined;
  flipX?: boolean | undefined;
  flipY?: boolean | undefined;
}

export interface VisualDefinition extends VisualFrameDefinition {
  text?: string | undefined;
  align?: string | undefined;
  animator?: string | undefined;
  sortLayerOffset?: number | undefined;
  surfaceOffsetY?: number | undefined;
  occlusionFade?: boolean | undefined;
  metadata?: Record<string, unknown> | undefined;
  type?: string | undefined;
  textures?: string[] | undefined;
  frames?: Array<string | VisualFrameDefinition> | undefined;
  animations?:
    | Record<
        string,
        VisualFrameDefinition & {
          textures?: string[] | undefined;
          frames?: Array<string | VisualFrameDefinition>;
        }
      >
    | undefined;
  layers?: VisualDefinition[] | undefined;
  instanced?: boolean | undefined;
}
