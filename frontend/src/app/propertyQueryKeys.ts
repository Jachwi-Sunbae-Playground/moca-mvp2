import { authenticationQueryKey } from './queryClient';

export const propertyQueryKeys = {
  all: [...authenticationQueryKey, 'properties'] as const,
  lists: () => [...propertyQueryKeys.all, 'list'] as const,
  list: (query: string) => [...propertyQueryKeys.lists(), { query: query.trim(), size: 20 }] as const,
  details: () => [...propertyQueryKeys.all, 'detail'] as const,
  detail: (propertyId: number) => [...propertyQueryKeys.details(), propertyId] as const,
  memo: (propertyId: number) => [...propertyQueryKeys.detail(propertyId), 'memo'] as const,
  checklists: (propertyId: number) => [...propertyQueryKeys.detail(propertyId), 'checklists'] as const,
  checklist: (propertyId: number, propertyChecklistId: number) =>
    [...propertyQueryKeys.checklists(propertyId), propertyChecklistId] as const,
  photos: (propertyId: number) => [...propertyQueryKeys.detail(propertyId), 'photos'] as const,
  photoContents: (propertyId: number) => [...propertyQueryKeys.photos(propertyId), 'content'] as const,
  photoContent: (propertyId: number, photoId: number) =>
    [...propertyQueryKeys.photoContents(propertyId), photoId] as const,
};
