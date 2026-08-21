import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { restoreAuthentication } from './app/authStore';

const rootElement = document.getElementById('root');

if (rootElement === null) {
  throw new Error('React 루트 요소를 찾을 수 없습니다.');
}

const startApplication = async () => {
  restoreAuthentication();

  if (__ENABLE_MSW__) {
    const { startBrowserMocking } = await import('./mocks/browser');
    await startBrowserMocking();
  }

  createRoot(rootElement).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
};

void startApplication();
