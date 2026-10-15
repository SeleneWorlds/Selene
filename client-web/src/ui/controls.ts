import { KeyboardControls } from './KeyboardControls';

function storage(): Storage | undefined {
  try { return window.localStorage; }
  catch (error) { console.warn('Controls storage is unavailable.', error); return undefined; }
}

export const keyboardControls = new KeyboardControls(storage());
