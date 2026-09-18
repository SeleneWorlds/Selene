import type {
  InputApi,
  ClientInputType,
  ClientKeyboardInputListener,
  ClientMouseInputListener,
} from '@/api/InputApi';
import type { BundleUiInputClaims } from '@/ui/BundleUiInputClaims';

type InputKey = `${ClientInputType}:${string}`;

const keyboardAliases: Record<string, string> = {
  Up: 'ArrowUp',
  Down: 'ArrowDown',
  Left: 'ArrowLeft',
  Right: 'ArrowRight',
  Enter: 'Enter',
  Escape: 'Escape',
  Space: ' ',
  Tab: 'Tab',
  Backspace: 'Backspace',
  Delete: 'Delete',
  'L-Shift': 'ShiftLeft',
  'R-Shift': 'ShiftRight',
  'L-Ctrl': 'ControlLeft',
  'R-Ctrl': 'ControlRight',
  'L-Alt': 'AltLeft',
  'R-Alt': 'AltRight',
};

const mouseAliases: Record<string, number> = {
  left: 0,
  middle: 1,
  right: 2,
  back: 3,
  forward: 4,
};

export class BrowserInputManager implements InputApi {
  private readonly continuousActions = new Map<InputKey, ClientKeyboardInputListener>();
  private readonly keyboardActions = new Map<string, ClientKeyboardInputListener>();
  private readonly keyboardPressActions = new Map<string, ClientKeyboardInputListener>();
  private readonly keyboardReleaseActions = new Map<string, ClientKeyboardInputListener>();
  private readonly mouseActions = new Map<number, ClientMouseInputListener>();
  private readonly mousePressActions = new Map<number, ClientMouseInputListener>();
  private readonly mouseReleaseActions = new Map<number, ClientMouseInputListener>();
  private readonly pressedKeys = new Set<string>();
  private readonly pressedMouseButtons = new Set<number>();

  private mouseX = 0;
  private mouseY = 0;

  constructor(private readonly inputHost: HTMLElement, private readonly uiInput: BundleUiInputClaims) {
    window.addEventListener('keydown', this.handleKeyDown, true);
    window.addEventListener('keyup', this.handleKeyUp, true);
    this.inputHost.addEventListener('mousedown', this.handleMouseDown);
    this.inputHost.addEventListener('mouseup', this.handleMouseUp);
    this.inputHost.addEventListener('mousemove', this.handleMouseMove);
    this.inputHost.addEventListener('contextmenu', this.preventContextMenu);
    this.inputHost.tabIndex = this.inputHost.tabIndex >= 0 ? this.inputHost.tabIndex : 0;
  }

  bindContinuousAction(type: ClientInputType, input: string, callback: ClientKeyboardInputListener): void {
    this.continuousActions.set(getInputKey(type, this.normalizeInput(type, input)), callback);
  }

  bindAction(
    type: ClientInputType,
    input: string,
    keyboardCallback: ClientKeyboardInputListener,
    mouseCallback: ClientMouseInputListener,
  ): void {
    if (type === 'keyboard') {
      this.keyboardActions.set(normalizeKeyboardKey(input), keyboardCallback);
      return;
    }

    this.mouseActions.set(normalizeMouseButton(input), mouseCallback);
  }

  bindPressAction(
    type: ClientInputType,
    input: string,
    keyboardCallback: ClientKeyboardInputListener,
    mouseCallback: ClientMouseInputListener,
  ): void {
    if (type === 'keyboard') {
      this.keyboardPressActions.set(normalizeKeyboardKey(input), keyboardCallback);
      return;
    }

    this.mousePressActions.set(normalizeMouseButton(input), mouseCallback);
  }

  bindReleaseAction(
    type: ClientInputType,
    input: string,
    keyboardCallback: ClientKeyboardInputListener,
    mouseCallback: ClientMouseInputListener,
  ): void {
    if (type === 'keyboard') {
      this.keyboardReleaseActions.set(normalizeKeyboardKey(input), keyboardCallback);
      return;
    }

    this.mouseReleaseActions.set(normalizeMouseButton(input), mouseCallback);
  }

  isKeyPressed(key: string): boolean {
    return this.pressedKeys.has(normalizeKeyboardKey(key));
  }

  isMousePressed(button: string): boolean {
    return this.pressedMouseButtons.has(normalizeMouseButton(button));
  }

  getMousePosition(): { x: number; y: number } {
    return { x: this.mouseX, y: this.mouseY };
  }

  update(): void {
    for (const [inputKey, action] of this.continuousActions) {
      const [type, input] = inputKey.split(':', 2) as [ClientInputType, string];

      if ((type === 'keyboard' && this.pressedKeys.has(input)) || (type === 'mouse' && this.pressedMouseButtons.has(Number(input)))) {
        action();
      }
    }
  }

  private readonly handleKeyDown = (event: KeyboardEvent): void => {
    if (this.uiInput.consumes(event)) return;
    const key = normalizeKeyboardEvent(event);
    const wasPressed = this.pressedKeys.has(key);

    this.pressedKeys.add(key);
    if (!wasPressed) {
      this.keyboardActions.get(key)?.();
      this.keyboardPressActions.get(key)?.();
    }
    if (this.uiInput.isProtectedKey(event.key)) event.stopImmediatePropagation();
  };

  private readonly handleKeyUp = (event: KeyboardEvent): void => {
    if (this.uiInput.consumes(event)) return;
    const key = normalizeKeyboardEvent(event);

    this.pressedKeys.delete(key);
    this.keyboardReleaseActions.get(key)?.();
    if (this.uiInput.isProtectedKey(event.key)) event.stopImmediatePropagation();
  };

  private readonly handleMouseDown = (event: MouseEvent): void => {
    this.updateMousePosition(event);
    this.inputHost.focus();
    this.pressedMouseButtons.add(event.button);
    this.mousePressActions.get(event.button)?.(this.mouseX, this.mouseY);
  };

  private readonly handleMouseUp = (event: MouseEvent): void => {
    this.updateMousePosition(event);
    this.pressedMouseButtons.delete(event.button);
    this.mouseActions.get(event.button)?.(this.mouseX, this.mouseY);
    this.mouseReleaseActions.get(event.button)?.(this.mouseX, this.mouseY);
  };

  private readonly handleMouseMove = (event: MouseEvent): void => {
    this.updateMousePosition(event);
  };

  private readonly preventContextMenu = (event: MouseEvent): void => {
    event.preventDefault();
  };

  private normalizeInput(type: ClientInputType, input: string): string {
    return type === 'keyboard' ? normalizeKeyboardKey(input) : String(normalizeMouseButton(input));
  }

  private updateMousePosition(event: MouseEvent): void {
    const rect = this.inputHost.getBoundingClientRect();

    this.mouseX = Math.round(event.clientX - rect.left);
    this.mouseY = Math.round(event.clientY - rect.top);
  }
}

function normalizeKeyboardEvent(event: KeyboardEvent): string {
  return event.code || event.key;
}

function normalizeKeyboardKey(key: string): string {
  const trimmedKey = key.trim();

  return keyboardAliases[trimmedKey] ?? trimmedKey;
}

function normalizeMouseButton(button: string): number {
  const normalizedButton = button.trim().toLowerCase();
  const buttonCode = mouseAliases[normalizedButton];

  if (buttonCode === undefined) {
    throw new Error(`Unknown mouse button: ${button}`);
  }

  return buttonCode;
}

function getInputKey(type: ClientInputType, input: string): InputKey {
  return `${type}:${input}`;
}
