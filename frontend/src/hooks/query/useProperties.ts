import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import {
  fetchProperties,
  fetchPropertyChecklistOverview,
  fetchPropertyChecklistDetail,
  fetchPropertyDetail,
  fetchPropertyMemo,
} from '../../apis/propertyApi';
import { fetchPropertyPhotos } from '../../apis/photoApi';
import { propertyQueryKeys } from '../../app/propertyQueryKeys';
import type { PublicConfig } from '../../types/PublicConfig';

export const usePropertyList = (config: PublicConfig) =>
  useInfiniteQuery({
    queryKey: propertyQueryKeys.list(''),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => fetchProperties(config, { query: '', page: pageParam, size: 20 }, signal),
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
  });

export const getPropertyDetailQueryOptions = (config: PublicConfig, propertyId: number) => ({
  queryKey: propertyQueryKeys.detail(propertyId),
  queryFn: ({ signal }: { signal: AbortSignal }) => fetchPropertyDetail(config, propertyId, signal),
});

export const usePropertyDetail = (config: PublicConfig, propertyId: number) =>
  useQuery(getPropertyDetailQueryOptions(config, propertyId));

export const usePropertyMemo = (config: PublicConfig, propertyId: number) =>
  useQuery({
    queryKey: propertyQueryKeys.memo(propertyId),
    queryFn: ({ signal }) => fetchPropertyMemo(config, propertyId, signal),
  });

export const usePropertyChecklistOverview = (config: PublicConfig, propertyId: number) =>
  useQuery({
    queryKey: propertyQueryKeys.checklists(propertyId),
    queryFn: ({ signal }) => fetchPropertyChecklistOverview(config, propertyId, signal),
  });

export const usePropertyChecklistDetail = (config: PublicConfig, propertyId: number, propertyChecklistId: number) =>
  useQuery({
    queryKey: propertyQueryKeys.checklist(propertyId, propertyChecklistId),
    queryFn: ({ signal }) => fetchPropertyChecklistDetail(config, propertyId, propertyChecklistId, signal),
  });

export const usePropertyPhotos = (config: PublicConfig, propertyId: number) =>
  useQuery({
    queryKey: propertyQueryKeys.photos(propertyId),
    queryFn: ({ signal }) => fetchPropertyPhotos(config, propertyId, signal),
  });
