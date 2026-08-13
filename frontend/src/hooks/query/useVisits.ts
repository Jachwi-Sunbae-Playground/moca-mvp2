import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { fetchPropertyVisits, fetchVisitDetail } from '../../apis/visitApi';
import { visitQueryKeys } from '../../app/visitQueryKeys';
import type { PublicConfig } from '../../types/PublicConfig';

export const usePropertyVisits = (config: PublicConfig, propertyId: number) =>
  useInfiniteQuery({
    queryKey: visitQueryKeys.list(propertyId),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => fetchPropertyVisits(config, propertyId, pageParam, 20, signal),
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
  });

export const getVisitDetailQueryOptions = (config: PublicConfig, visitId: number) => ({
  queryKey: visitQueryKeys.detail(visitId),
  queryFn: ({ signal }: { signal: AbortSignal }) => fetchVisitDetail(config, visitId, signal),
});

export const useVisitDetail = (config: PublicConfig, visitId: number) =>
  useQuery(getVisitDetailQueryOptions(config, visitId));
