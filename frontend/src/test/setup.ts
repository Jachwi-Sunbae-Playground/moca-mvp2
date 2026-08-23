import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { webcrypto } from 'node:crypto';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { resetAuthenticationForTests } from '../app/authStore';
import { server } from './server';

configure({ asyncUtilTimeout: 5_000 });

Object.defineProperty(globalThis, 'crypto', {
  configurable: true,
  value: webcrypto,
});

Object.defineProperty(window, 'matchMedia', {
  configurable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }),
});

let objectUrlSequence = 0;
Object.defineProperty(URL, 'createObjectURL', {
  configurable: true,
  value: vi.fn(() => `blob:test-photo-${objectUrlSequence++}`),
});
Object.defineProperty(URL, 'revokeObjectURL', {
  configurable: true,
  value: vi.fn(),
});

HTMLDialogElement.prototype.showModal = function showModal() {
  this.setAttribute('open', '');
};

HTMLDialogElement.prototype.close = function close() {
  this.removeAttribute('open');
  this.dispatchEvent(new Event('close'));
};

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

afterEach(() => {
  cleanup();
  server.resetHandlers();
  resetAuthenticationForTests();
  window.sessionStorage.clear();
  window.localStorage.clear();
  window.history.replaceState(null, '', '/');
  vi.mocked(URL.createObjectURL).mockClear();
  vi.mocked(URL.revokeObjectURL).mockClear();
});

afterAll(() => server.close());
