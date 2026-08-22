Object.freeze({
    attachStyles(root, disposers) {
        const mirrors = new Map();
        const anchor = document.createComment('Vite development styles');
        root.prepend(anchor);
        const synchronize = () => {
            const current = new Set(document.head.querySelectorAll('style[data-vite-dev-id]'));
            for (const style of current) {
                let mirror = mirrors.get(style);
                if (!mirror) {
                    mirror = style.cloneNode(true);
                    mirrors.set(style, mirror);
                    root.insertBefore(mirror, anchor);
                } else if (mirror.textContent !== style.textContent) {
                    mirror.textContent = style.textContent;
                }
            }
            for (const [style, mirror] of mirrors) {
                if (current.has(style)) continue;
                mirror.remove();
                mirrors.delete(style);
            }
        };
        const observer = new MutationObserver(synchronize);
        observer.observe(document.head, {subtree: true, childList: true, characterData: true});
        synchronize();
        disposers.push(() => {
            observer.disconnect();
            for (const mirror of mirrors.values()) mirror.remove();
            mirrors.clear();
            anchor.remove();
        });
    }
})
