import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { fetchProperties, fetchPropertyDetail } from '../../apis/propertyApi';
import { fetchPropertyPhotos } from '../../apis/photoApi';
import { propertyQueryKeys } from '../../app/propertyQueryKeys';
import type { PublicConfig } from '../../types/PublicConfig';

export const usePropertyList = (config: PublicConfig, query: string) =>
  useInfiniteQuery({
    queryKey: propertyQueryKeys.list(query),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => fetchProperties(config, { query, page: pageParam, size: 20 }, signal),
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
  });

export const getPropertyDetailQueryOptions = (config: PublicConfig, propertyId: number) => ({
  queryKey: propertyQueryKeys.detail(propertyId),
  queryFn: ({ signal }: { signal: AbortSignal }) => fetchPropertyDetail(config, propertyId, signal),
});

export const usePropertyDetail = (config: PublicConfig, propertyId: number) =>
  useQuery(getPropertyDetailQueryOptions(config, propertyId));

export const usePropertyPhotos = (config: PublicConfig, propertyId: number) =>
  useQuery({
    queryKey: propertyQueryKeys.photos(propertyId),
    queryFn: ({ signal }) => fetchPropertyPhotos(config, propertyId, signal),
  });
