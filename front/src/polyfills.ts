(window as any).global = window;

// Fix for Buffer
(window as any).Buffer = (window as any).Buffer || require('buffer').Buffer;

// Fix for process
(window as any).process = {
  env: { DEBUG: undefined },
  version: '',
  nextTick: (callback: Function) => setTimeout(callback, 0)
};