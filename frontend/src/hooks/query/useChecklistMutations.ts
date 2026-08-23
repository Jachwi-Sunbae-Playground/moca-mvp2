import { useMutation } from '@tanstack/react-query';
import {
  assignActiveChecklist,
  createChecklistV11,
  removeChecklist,
  updateChecklistV11,
} from '../../apis/checklistApi';
import type { CreateChecklistV11RequestDto, UpdateChecklistV11RequestDto } from '../../apis/dtos/ChecklistDto';
import { checklistQueryKeys } from '../../app/checklistQueryKeys';
import { propertyQueryKeys } from '../../app/propertyQueryKeys';
import { queryClient } from '../../app/queryClient';
import type { ChecklistStage } from '../../types/Checklist';
import type { PublicConfig } from '../../types/PublicConfig';

const invalidateChecklistAggregates = async () =>
  Promise.all([
    queryClient.invalidateQueries({ queryKey: checklistQueryKeys.lists() }),
    queryClient.invalidateQueries({ queryKey: checklistQueryKeys.details() }),
  ]);

export const useCreateChecklist = (config: PublicConfig) =>
  useMutation({
    mutationFn: (request: CreateChecklistV11RequestDto) => createChecklistV11(config, request),
    retry: false,
    onSuccess: async (detail) => {
      queryClient.setQueryData(checklistQueryKeys.detail(detail.checklistId), detail);
      await queryClient.invalidateQueries({ queryKey: checklistQueryKeys.lists() });
    },
  });

export const useUpdateChecklist = (config: PublicConfig, checklistId: number) =>
  useMutation({
    mutationFn: (request: UpdateChecklistV11RequestDto) => updateChecklistV11(config, checklistId, request),
    retry: false,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: checklistQueryKeys.detail(checklistId), exact: true });
    },
    onSuccess: async (detail) => {
      queryClient.setQueryData(checklistQueryKeys.detail(checklistId), detail);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: checklistQueryKeys.lists() }),
        queryClient.invalidateQueries({ queryKey: propertyQueryKeys.details() }),
      ]);
    },
  });

export const useRemoveChecklist = (config: PublicConfig, checklistId: number) =>
  useMutation({
    mutationFn: () => removeChecklist(config, checklistId),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: checklistQueryKeys.detail(checklistId), exact: true });
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: checklistQueryKeys.lists() }),
        queryClient.invalidateQueries({ queryKey: propertyQueryKeys.details() }),
      ]);
      queryClient.removeQueries({ queryKey: checklistQueryKeys.detail(checklistId), exact: true });
    },
  });

export const useAssignActiveChecklist = (config: PublicConfig, propertyId: number, stage: ChecklistStage) =>
  useMutation({
    mutationFn: (checklistId: number | 'SYSTEM_DEFAULT') =>
      assignActiveChecklist(
        config,
        propertyId,
        stage,
        checklistId === 'SYSTEM_DEFAULT'
          ? { sourceType: 'SYSTEM_DEFAULT', checklistId: null }
          : { sourceType: 'USER', checklistId },
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: propertyQueryKeys.detail(propertyId), exact: true }),
        queryClient.invalidateQueries({ queryKey: propertyQueryKeys.checklists(propertyId), exact: true }),
        queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() }),
        invalidateChecklistAggregates(),
      ]);
    },
  });
