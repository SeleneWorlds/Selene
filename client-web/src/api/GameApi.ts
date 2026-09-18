export type ClientGamePreTickListener = () => void;

export interface GameApi {
  addPreTickListener: (listener: ClientGamePreTickListener) => () => void;
  setWindowAspectRatio: (width: number, height: number) => void;
  clearWindowAspectRatio: () => void;
  setOffscreenRendering: (width: number, height: number) => void;
  setNativeRendering: () => void;
}
