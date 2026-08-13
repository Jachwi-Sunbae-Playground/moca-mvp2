import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { unstable_HistoryRouter as HistoryRouter, UNSAFE_createBrowserHistory } from 'react-router-dom';
import { createGuardedHistory } from './guardedHistory';
import { queryClient } from './queryClient';

type AppProvidersProps = {
  children: ReactNode;
};

let browserHistory: ReturnType<typeof createGuardedHistory> | null = null;

const getBrowserHistory = () => {
  browserHistory ??= createGuardedHistory(UNSAFE_createBrowserHistory({ v5Compat: true }));
  return browserHistory;
};

const AppProviders = ({ children }: AppProvidersProps) => {
  const history = getBrowserHistory();

  return (
    <QueryClientProvider client={queryClient}>
      <HistoryRouter history={history}>{children}</HistoryRouter>
    </QueryClientProvider>
  );
};

export default AppProviders;
