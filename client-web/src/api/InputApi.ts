export type ClientInputType = 'keyboard' | 'mouse';
export type ClientKeyboardInputListener = () => void;
export type ClientMouseInputListener = (screenX: number, screenY: number) => void;

export interface InputApi {
  bindContinuousAction: (type: ClientInputType, input: string, callback: ClientKeyboardInputListener) => void;
  bindAction: (
    type: ClientInputType,
    input: string,
    keyboardCallback: ClientKeyboardInputListener,
    mouseCallback: ClientMouseInputListener,
  ) => void;
  bindPressAction: (
    type: ClientInputType,
    input: string,
    keyboardCallback: ClientKeyboardInputListener,
    mouseCallback: ClientMouseInputListener,
  ) => void;
  bindReleaseAction: (
    type: ClientInputType,
    input: string,
    keyboardCallback: ClientKeyboardInputListener,
    mouseCallback: ClientMouseInputListener,
  ) => void;
  isKeyPressed: (key: string) => boolean;
  isMousePressed: (button: string) => boolean;
  getMousePosition: () => { x: number; y: number };
}
