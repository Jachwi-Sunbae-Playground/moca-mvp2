import { QueryClient } from '@tanstack/react-query';

export const authenticationQueryKey = ['authentication'] as const;
export const currentMemberQueryKey = [...authenticationQueryKey, 'current-member'] as const;

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});
