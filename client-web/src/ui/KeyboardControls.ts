export interface ShortcutDefinition {
  id: string;
  label: string;
  category?: string;
  defaultBinding: string | null;
  allowInText?: boolean;
  allowShift?: boolean;
}

export interface KeyChord {
  key: string;
  ctrlKey: boolean;
  shiftKey: boolean;
  altKey: boolean;
  metaKey: boolean;
}

export interface Shortcut extends ShortcutDefinition {
  callback?: (event: KeyboardEvent) => boolean | void;
}

interface BindingStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
}

const storageKey = 'selene.client.controls.v1';
const fixedBindings = [
  { binding: 'Ctrl+,', label: 'Client menu' },
  { binding: 'F5', label: 'Reload web client' },
  { binding: 'Ctrl+Enter', label: 'Run selected admin action', allowShift: true },
  { binding: 'Enter', label: 'Send chat message' },
  { binding: 'Shift+Enter', label: 'Chat line break' },
  { binding: 'Ctrl+a', label: 'Select chat input', allowShift: true },
  { binding: 'Meta+a', label: 'Select chat input', allowShift: true },
  { binding: 'Ctrl+s', label: 'Save editor changes' },
  { binding: 'Meta+s', label: 'Save editor changes' },
  { binding: 'Escape', label: 'Game menu' },
];

function reservedBinding(binding: KeyChord | null): string | undefined {
  if (binding?.key === 'Escape') return 'Game menu';
  if (binding?.key === 'a' && !binding.altKey && (binding.ctrlKey || binding.metaKey)) return 'Select chat input';
  if (binding?.key === 's' && !binding.altKey && !binding.shiftKey && (binding.ctrlKey || binding.metaKey)) return 'Save editor changes';
  return binding ? fixedBindings.find(fixed =>
    bindingMatches(parseBinding(fixed.binding), binding, fixed.allowShift))?.label : undefined;
}
export const movementControls: Readonly<Record<string, string>> = {
  ArrowUp: 'move.up', ArrowDown: 'move.down', ArrowLeft: 'move.left', ArrowRight: 'move.right',
};
export const normalizeControlKey = (key: string): string =>
  key === ' ' ? 'Space' : key === '+' ? 'Plus' : key.length === 1 ? key.toLowerCase() : key;

export function parseBinding(value: string | null): KeyChord | null {
  if (value === null) return null;
  if (typeof value !== 'string' || !value) throw new Error('Invalid key binding.');
  const parts = value.split('+');
  const key = normalizeControlKey(parts.pop()!);
  if (!key || ['Control', 'Shift', 'Alt', 'Meta', 'Unidentified', 'Dead'].includes(key) ||
      parts.some(part => !['Ctrl', 'Shift', 'Alt', 'Meta'].includes(part)) || new Set(parts).size !== parts.length) {
    throw new Error('Choose a key, optionally with Ctrl, Shift, Alt or Meta.');
  }
  return { key, ctrlKey: parts.includes('Ctrl'), shiftKey: parts.includes('Shift'),
    altKey: parts.includes('Alt'), metaKey: parts.includes('Meta') };
}

export function formatBinding(binding: KeyChord | null): string | null {
  if (!binding) return null;
  return [binding.ctrlKey && 'Ctrl', binding.shiftKey && 'Shift', binding.altKey && 'Alt',
    binding.metaKey && 'Meta', normalizeControlKey(binding.key)].filter(Boolean).join('+');
}

export function bindingMatches(binding: KeyChord | null, event: KeyChord, allowShift = false): boolean {
  return binding !== null && binding.key === normalizeControlKey(event.key) &&
    binding.ctrlKey === event.ctrlKey && (allowShift || binding.shiftKey === event.shiftKey) &&
    binding.altKey === event.altKey && binding.metaKey === event.metaKey;
}

export class KeyboardControls {
  private readonly actions = new Map<string, Shortcut>();
  private overrides: Record<string, string | null> = {};
  private readonly listeners = new Set<() => void>();
  private readonly editableFocusSources = new Set<() => boolean>();
  blocked = false;

  constructor(private readonly storage?: BindingStorage) {
    try {
      const saved: unknown = JSON.parse(storage?.getItem(storageKey) ?? '{}');
      if (saved && typeof saved === 'object' && !Array.isArray(saved)) {
        for (const [id, value] of Object.entries(saved)) {
          try {
            if (typeof value === 'string' || value === null) {
              this.overrides[id] = formatBinding(parseBinding(value));
            }
          } catch { /* Ignore invalid entries while retaining other saved choices. */ }
        }
      }
    } catch (error) { console.warn('Could not load controls.', error); }
    for (const [key, id] of Object.entries(movementControls)) {
      const label = { ArrowUp: 'Move north', ArrowDown: 'Move south', ArrowLeft: 'Move west', ArrowRight: 'Move east' }[key]!;
      this.registerBinding({ id, label, category: 'Movement', defaultBinding: key, allowShift: true });
    }
  }

  registerBinding(definition: ShortcutDefinition, callback?: Shortcut['callback']): () => void {
    if (this.actions.has(definition.id)) throw new Error(`Duplicate control: ${definition.id}`);
    const reserved = reservedBinding(parseBinding(definition.defaultBinding));
    if (reserved) throw new Error(`This key combination is reserved for ${reserved}.`);
    const action: Shortcut = { ...definition, ...(callback ? { callback } : {}) };
    this.actions.set(action.id, action);
    this.changed();
    return () => {
      if (this.actions.get(action.id) === action) { this.actions.delete(action.id); this.changed(); }
    };
  }

  list(): Shortcut[] {
    return [...this.actions.values()].sort((a, b) =>
      (a.category ?? 'Other').localeCompare(b.category ?? 'Other') || a.label.localeCompare(b.label, undefined, { numeric: true }));
  }

  binding(action: ShortcutDefinition): KeyChord | null {
    return parseBinding(Object.prototype.hasOwnProperty.call(this.overrides, action.id) ? this.overrides[action.id]! : action.defaultBinding);
  }

  matchesBinding(id: string, event: KeyChord): boolean {
    const action = this.actions.get(id);
    return !!action && bindingMatches(this.binding(action), event, action.allowShift);
  }

  conflicts(id: string, value: string | null): Shortcut[] {
    const binding = parseBinding(value);
    if (!binding) return [];
    const action = this.actions.get(id);
    return this.list().filter(other => other.id !== id &&
      bindingMatches(this.binding(other), binding, action?.allowShift || other.allowShift));
  }

  setBinding(id: string, value: string | null): void {
    if (!this.actions.has(id)) throw new Error('Unknown control.');
    const binding = parseBinding(value);
    const reserved = reservedBinding(binding);
    if (reserved) throw new Error(`This key combination is reserved for ${reserved}.`);
    if (id.startsWith('move.') && binding?.shiftKey) throw new Error('Shift is used to turn. Choose a movement binding without Shift.');
    const conflicts = this.conflicts(id, value);
    if (conflicts.length) throw new Error(`Already used by ${conflicts.map(action => action.label).join(', ')}. Unbind it first.`);
    this.persist({ ...this.overrides, [id]: formatBinding(binding) });
  }

  reset(id?: string): void {
    if (id) {
      const action = this.actions.get(id);
      if (!action) throw new Error('Unknown control.');
      const conflicts = this.conflicts(id, action.defaultBinding);
      if (conflicts.length) throw new Error(`Default is used by ${conflicts.map(other => other.label).join(', ')}. Unbind it first.`);
      const overrides = { ...this.overrides };
      delete overrides[id];
      this.persist(overrides);
    } else this.persist({});
  }

  isMovementKey(key: string, editableFocused = false): boolean {
    return this.list().some(action => action.id.startsWith('move.') &&
      this.binding(action)?.key === normalizeControlKey(key) && (!editableFocused || key.startsWith('Arrow')) &&
      !this.binding(action)?.ctrlKey && !this.binding(action)?.altKey && !this.binding(action)?.metaKey);
  }

  isMovementEvent(event: KeyChord, editableFocused: boolean): boolean {
    return this.list().some(action => action.id.startsWith('move.') &&
      (!editableFocused || this.binding(action)?.key.startsWith('Arrow')) &&
      bindingMatches(this.binding(action), event, true));
  }

  movementPressed(logicalKey: string, events: Iterable<KeyChord>, editableFocused: boolean): boolean {
    if (this.blocked) return false;
    const action = this.actions.get(movementControls[logicalKey] ?? '');
    if (!action) return false;
    const binding = this.binding(action);
    if (!binding || (editableFocused && !binding.key.startsWith('Arrow'))) return false;
    for (const event of events) if (bindingMatches(binding, event, true)) return true;
    return false;
  }

  dispatch(event: KeyboardEvent, editableFocused: boolean): boolean {
    if (this.blocked) return false;
    for (const action of this.actions.values()) {
      if (!action.callback || (!action.allowInText && editableFocused) ||
        !bindingMatches(this.binding(action), event, action.allowShift)) continue;
      if (event.repeat || action.callback(event) !== false) {
        event.preventDefault(); event.stopImmediatePropagation();
        return true;
      }
    }
    return false;
  }

  subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  registerEditableFocusSource(source: () => boolean): () => void {
    this.editableFocusSources.add(source);
    return () => this.editableFocusSources.delete(source);
  }

  hasEditableFocus(): boolean {
    return [...this.editableFocusSources].some(source => source());
  }

  private persist(overrides: Record<string, string | null>): void {
    // Keep active bindings unchanged if storage fails, so the UI can report it.
    this.storage?.setItem(storageKey, JSON.stringify(overrides));
    this.overrides = overrides;
    this.changed();
  }

  private changed(): void { for (const listener of this.listeners) listener(); }
}
