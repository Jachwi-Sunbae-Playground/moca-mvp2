import { setupWorker } from 'msw/browser';
import { setAuthentication } from '../app/authStore';
import { handlers } from './handlers';

const worker = setupWorker(...handlers);

export const startBrowserMocking = async () => {
  await worker.start({
    onUnhandledRequest: 'bypass',
    serviceWorker: { url: '/mockServiceWorker.js' },
  });

  setAuthentication({
    accessToken: 'local-msw-access-token',
    tokenType: 'Bearer',
    expiresIn: 60 * 60 * 8,
  });
};
