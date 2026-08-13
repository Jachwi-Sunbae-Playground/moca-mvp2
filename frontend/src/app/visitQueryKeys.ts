import { authenticationQueryKey } from './queryClient';

export const visitQueryKeys = {
  all: [...authenticationQueryKey, 'visits'] as const,
  lists: () => [...visitQueryKeys.all, 'list'] as const,
  propertyLists: (propertyId: number) => [...visitQueryKeys.lists(), propertyId] as const,
  list: (propertyId: number, size = 20) => [...visitQueryKeys.propertyLists(propertyId), { size }] as const,
  details: () => [...visitQueryKeys.all, 'detail'] as const,
  detail: (visitId: number) => [...visitQueryKeys.details(), visitId] as const,
};

export const visitMutationKeys = {
  itemUpdates: (visitId: number) => [...visitQueryKeys.detail(visitId), 'item-update'] as const,
  itemUpdate: (visitId: number, visitItemId: number) =>
    [...visitMutationKeys.itemUpdates(visitId), visitItemId] as const,
  statusUpdate: (visitId: number, visitItemId: number) =>
    [...visitMutationKeys.itemUpdate(visitId, visitItemId), 'status'] as const,
  memoUpdate: (visitId: number, visitItemId: number) =>
    [...visitMutationKeys.itemUpdate(visitId, visitItemId), 'memo'] as const,
};
