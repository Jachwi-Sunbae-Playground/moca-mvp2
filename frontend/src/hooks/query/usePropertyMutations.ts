import { useMutation } from '@tanstack/react-query';
import type {
  PropertyInputDto,
  SavePropertyMemoDocumentRequestDto,
  UpdatePropertyRequestDto,
} from '../../apis/dtos/PropertyDto';
import { removePropertyPhoto, setRepresentativePropertyPhoto, uploadPropertyPhoto } from '../../apis/photoApi';
import { createProperty, removeProperty, savePropertyMemoDocument, updateProperty } from '../../apis/propertyApi';
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
              maintenanceFeeAmount: updated.maintenanceFeeAmount,
              discoverySource: updated.discoverySource,
              updatedAt: updated.updatedAt ?? current.updatedAt,
              lastActivityAt: updated.updatedAt ?? current.lastActivityAt,
            },
      );
      await queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() });
    },
  });

export const useSavePropertyMemoDocument = (config: PublicConfig, propertyId: number) =>
  useMutation({
    mutationFn: (request: SavePropertyMemoDocumentRequestDto) => savePropertyMemoDocument(config, propertyId, request),
    onSuccess: (memo) => {
      queryClient.setQueryData(propertyQueryKeys.memo(propertyId), memo);
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

export const useSetRepresentativePropertyPhoto = (config: PublicConfig, propertyId: number) =>
  useMutation({
    mutationFn: (photoId: number) => setRepresentativePropertyPhoto(config, propertyId, photoId),
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
