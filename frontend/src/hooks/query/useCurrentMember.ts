import { useQuery } from '@tanstack/react-query';
import { fetchCurrentMember } from '../../apis/memberApi';
import { currentMemberQueryKey } from '../../app/queryClient';
import type { PublicConfig } from '../../types/PublicConfig';

export const getCurrentMemberQueryOptions = (config: PublicConfig) => ({
  queryKey: currentMemberQueryKey,
  queryFn: ({ signal }: { signal: AbortSignal }) => fetchCurrentMember(config, signal),
  staleTime: 5 * 60 * 1_000,
});

export const useCurrentMember = (config: PublicConfig, isEnabled = true) =>
  useQuery({
    ...getCurrentMemberQueryOptions(config),
    enabled: isEnabled,
  });
