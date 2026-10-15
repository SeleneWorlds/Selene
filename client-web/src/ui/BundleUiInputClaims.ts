import { keyboardControls } from './controls';
import type { ShortcutDefinition } from './KeyboardControls';

export class BundleUiInputClaims {
  private readonly capturedKeys = new Map<string, number>();
  private readonly passthroughKeys = new Map<string, number>();
  private readonly roots = new Set<ShadowRoot>();
  private textCaptureCount = 0;

  constructor() { keyboardControls.registerEditableFocusSource(() => this.hasEditableFocus()); }

  registerBinding(definition: ShortcutDefinition, callback?: (event: KeyboardEvent) => boolean | void): () => void {
    return keyboardControls.registerBinding(definition, callback);
  }

  matchesBinding(id: string, event: KeyboardEvent): boolean {
    return keyboardControls.matchesBinding(id, event);
  }

  captureKeys(...keys: string[]): () => void {
    return this.registerKeys(this.capturedKeys, keys);
  }

  captureText(): () => void {
    this.textCaptureCount += 1;
    let active = true;
    return () => {
      if (!active) return;
      active = false;
      this.textCaptureCount -= 1;
    };
  }

  passThroughKeys(...keys: string[]): () => void {
    return this.registerKeys(this.passthroughKeys, keys);
  }

  isPassthroughKey(key: string): boolean {
    return this.passthroughKeys.has(key) || keyboardControls.isMovementKey(key, this.hasEditableFocus());
  }

  isProtectedKey(key: string): boolean {
    return key === 'F5';
  }

  consumes(event: KeyboardEvent): boolean {
    if (keyboardControls.blocked || event.defaultPrevented) return true;
    if (keyboardControls.isMovementEvent(event, this.hasEditableFocus())) return false;
    if (this.isPassthroughKey(event.key)) return false;
    return this.hasEditableFocus() || this.capturedKeys.has(event.key) ||
      (this.textCaptureCount > 0 && event.key.length === 1);
  }

  consumesPointer(event: MouseEvent): boolean {
    const path = event.composedPath();
    for (const root of this.roots) {
      if (path.includes(root.host)) return true;
    }
    return false;
  }

  registerRoot(root: ShadowRoot): () => void {
    this.roots.add(root);
    return () => this.roots.delete(root);
  }

  private registerKeys(registry: Map<string, number>, keys: string[]): () => void {
    const uniqueKeys = new Set(keys);
    for (const key of uniqueKeys) {
      if (!key) throw new Error('Input keys must be non-empty strings.');
      registry.set(key, (registry.get(key) ?? 0) + 1);
    }
    let active = true;
    return () => {
      if (!active) return;
      active = false;
      for (const key of uniqueKeys) {
        const count = registry.get(key)! - 1;
        if (count) registry.set(key, count); else registry.delete(key);
      }
    };
  }

  hasEditableFocus(): boolean {
    const active = document.activeElement;
    if (active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement ||
      active instanceof HTMLSelectElement || (active instanceof HTMLElement && active.isContentEditable)) return true;
    for (const root of this.roots) {
      const active = root.activeElement;
      if (active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement ||
        active instanceof HTMLSelectElement ||
        (active instanceof HTMLElement && active.isContentEditable)) return true;
    }
    return false;
  }
}
