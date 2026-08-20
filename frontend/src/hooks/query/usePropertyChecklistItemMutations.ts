import { useMutation } from '@tanstack/react-query';
import { updatePropertyChecklistItemMemo, updatePropertyChecklistItemStatus } from '../../apis/propertyApi';
import { propertyQueryKeys } from '../../app/propertyQueryKeys';
import { queryClient } from '../../app/queryClient';
import type { PropertyChecklistDetail, PropertyChecklistItemStatus } from '../../types/Property';
import type { PublicConfig } from '../../types/PublicConfig';

const updateChecklistItem = (
  propertyId: number,
  propertyChecklistId: number,
  itemId: number,
  update: (item: PropertyChecklistDetail['items'][number]) => PropertyChecklistDetail['items'][number],
) => {
  queryClient.setQueryData<PropertyChecklistDetail>(
    propertyQueryKeys.checklist(propertyId, propertyChecklistId),
    (current) =>
      current === undefined
        ? current
        : { ...current, items: current.items.map((item) => (item.itemId === itemId ? update(item) : item)) },
  );
  void queryClient.invalidateQueries({ queryKey: propertyQueryKeys.checklists(propertyId), exact: true });
};

export const usePropertyChecklistItemStatusMutation = (
  config: PublicConfig,
  propertyId: number,
  propertyChecklistId: number,
) =>
  useMutation({
    mutationFn: ({ itemId, status }: { itemId: number; status: PropertyChecklistItemStatus }) =>
      updatePropertyChecklistItemStatus(config, propertyId, propertyChecklistId, itemId, status),
    onSuccess: ({ itemId, status }) => {
      updateChecklistItem(propertyId, propertyChecklistId, itemId, (item) => ({ ...item, status }));
    },
  });

export const usePropertyChecklistItemMemoMutation = (
  config: PublicConfig,
  propertyId: number,
  propertyChecklistId: number,
) =>
  useMutation({
    mutationFn: ({ itemId, memo }: { itemId: number; memo: string }) =>
      updatePropertyChecklistItemMemo(config, propertyId, propertyChecklistId, itemId, memo),
    onSuccess: ({ itemId, memo }) => {
      updateChecklistItem(propertyId, propertyChecklistId, itemId, (item) => ({ ...item, memo }));
    },
  });
