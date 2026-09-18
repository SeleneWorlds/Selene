import type { InputApi } from '@/api/InputApi';
import { LuaMultiReturn } from 'wasmoon';
import { LuaArguments } from './LuaArguments';
import type { LuaRuntime } from './LuaRuntime';

export async function registerInputLuaModule(runtime: LuaRuntime, input: InputApi): Promise<void> {
  const bindContinuousAction = (type: unknown, inputName: unknown, callback: unknown) => {
    const args = new LuaArguments('selene.input.bindContinuousAction');

    input.bindContinuousAction(
      args.inputType(type, 'type'),
      args.string(inputName, 'input'),
      args.function(callback, 'callback'),
    );
  };
  const bindAction = createActionBinder('selene.input.bindAction', (...args) => input.bindAction(...args));
  const bindPressAction = createActionBinder('selene.input.bindPressAction', (...args) => input.bindPressAction(...args));
  const bindReleaseAction = createActionBinder('selene.input.bindReleaseAction', (...args) => input.bindReleaseAction(...args));
  const isKeyPressed = (key: unknown) => {
    const args = new LuaArguments('selene.input.isKeyPressed');

    return input.isKeyPressed(args.string(key, 'key'));
  };
  const isMousePressed = (button: unknown) => {
    const args = new LuaArguments('selene.input.isMousePressed');

    return input.isMousePressed(args.string(button, 'button'));
  };
  const getMousePosition = () => {
    const position = input.getMousePosition();

    return LuaMultiReturn.of(position.x, position.y);
  };

  await runtime.preloadModule('selene.input', () => ({
    KEYBOARD: 'keyboard',
    MOUSE: 'mouse',
    bindContinuousAction,
    bindAction,
    bindPressAction,
    bindReleaseAction,
    isKeyPressed,
    isMousePressed,
    getMousePosition,
  }));
}

function createActionBinder(
  context: string,
  bind: InputApi['bindAction'],
): (type: unknown, inputName: unknown, callback: unknown) => void {
  return (type: unknown, inputName: unknown, callback: unknown) => {
    const args = new LuaArguments(context);
    const inputType = args.inputType(type, 'type');
    const checkedCallback = args.function(callback, 'callback');

    bind(
      inputType,
      args.string(inputName, 'input'),
      checkedCallback,
      (screenX, screenY) => checkedCallback(screenX, screenY),
    );
  };
}
