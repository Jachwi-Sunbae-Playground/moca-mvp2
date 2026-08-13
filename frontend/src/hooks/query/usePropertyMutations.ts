import { useMutation } from '@tanstack/react-query';
import type {
  PropertyInputDto,
  SavePropertyPreVisitMemoRequestDto,
  UpdatePropertyRequestDto,
} from '../../apis/dtos/PropertyDto';
import { removePropertyPhoto, uploadPropertyPhoto } from '../../apis/photoApi';
import { createProperty, removeProperty, savePropertyPreVisitMemo, updateProperty } from '../../apis/propertyApi';
import { propertyQueryKeys } from '../../app/propertyQueryKeys';
import { queryClient } from '../../app/queryClient';
import type { PublicConfig } from '../../types/PublicConfig';
import type { PropertyBasicInfo, PropertyDetail } from '../../types/Property';

export const useCreateProperty = (config: PublicConfig) =>
  useMutation({
    mutationFn: (request: PropertyInputDto) => createProperty(config, request),
    onSuccess: async () => queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() }),
  });

export const useUpdateProperty = (config: PublicConfig, propertyId: number) =>
  useMutation({
    mutationFn: (request: UpdatePropertyRequestDto) => updateProperty(config, propertyId, request),
    onSuccess: async (updated: PropertyBasicInfo) => {
      queryClient.setQueryData<PropertyDetail>(propertyQueryKeys.detail(propertyId), (current) =>
        current === undefined
          ? current
          : {
              ...current,
              name: updated.name,
              depositAmount: updated.depositAmount,
              monthlyRentAmount: updated.monthlyRentAmount,
              discoverySource: updated.discoverySource,
              updatedAt: updated.updatedAt,
              lastActivityAt: updated.updatedAt,
            },
      );
      await queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() });
    },
  });

export const useSavePropertyPreVisitMemo = (config: PublicConfig, propertyId: number) =>
  useMutation({
    mutationFn: (request: SavePropertyPreVisitMemoRequestDto) => savePropertyPreVisitMemo(config, propertyId, request),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: propertyQueryKeys.detail(propertyId), exact: true });
    },
    onSuccess: async (memo) => {
      queryClient.setQueryData<PropertyDetail>(propertyQueryKeys.detail(propertyId), (current) =>
        current === undefined
          ? current
          : {
              ...current,
              memo: { ...memo, content: memo.additionalMemo },
              updatedAt: memo.savedAt ?? current.updatedAt,
              lastActivityAt: memo.savedAt ?? current.lastActivityAt,
            },
      );
      await queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() });
    },
  });

export const useRemoveProperty = (config: PublicConfig, propertyId: number) =>
  useMutation({
    mutationFn: () => removeProperty(config, propertyId),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: propertyQueryKeys.detail(propertyId) });
      await queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() });
    },
  });

const invalidatePhotoAggregates = async (propertyId: number) => {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: propertyQueryKeys.photos(propertyId), exact: true }),
    queryClient.invalidateQueries({ queryKey: propertyQueryKeys.detail(propertyId), exact: true }),
    queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() }),
  ]);
};

export const useUploadPropertyPhoto = (config: PublicConfig, propertyId: number) =>
  useMutation({
    mutationFn: (file: File) => uploadPropertyPhoto(config, propertyId, file),
    onSuccess: async () => invalidatePhotoAggregates(propertyId),
  });

export const useRemovePropertyPhoto = (config: PublicConfig, propertyId: number) =>
  useMutation({
    mutationFn: (photoId: number) => removePropertyPhoto(config, propertyId, photoId),
    onSuccess: async (_, photoId) => {
      queryClient.removeQueries({ queryKey: propertyQueryKeys.photoContent(propertyId, photoId), exact: true });
      await invalidatePhotoAggregates(propertyId);
    },
    onError: async (error) => {
      if (error instanceof Error && 'code' in error && error.code === 'PHOTO_NOT_FOUND') {
        await queryClient.invalidateQueries({ queryKey: propertyQueryKeys.photos(propertyId) });
      }
    },
  });
