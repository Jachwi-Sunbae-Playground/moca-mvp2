import type { ChecklistPresetType, ChecklistStage } from '../types/Checklist';
import { authenticationQueryKey } from './queryClient';

export const checklistQueryKeys = {
  all: [...authenticationQueryKey, 'checklists'] as const,
  catalog: () => [...checklistQueryKeys.all, 'catalog'] as const,
  checkItems: (stage: ChecklistStage, query: string) =>
    [...checklistQueryKeys.catalog(), 'check-items', { stage, query: query.trim(), size: 20 }] as const,
  activeCheckItems: (stage: ChecklistStage) =>
    [...checklistQueryKeys.catalog(), 'active-check-items', { stage, size: 100 }] as const,
  presets: () => [...checklistQueryKeys.catalog(), 'presets'] as const,
  preset: (stage: ChecklistStage, presetType: ChecklistPresetType) =>
    [...checklistQueryKeys.presets(), { stage, presetType }] as const,
  lists: () => [...checklistQueryKeys.all, 'list'] as const,
  list: (stage: ChecklistStage) => [...checklistQueryKeys.lists(), { stage, size: 20 }] as const,
  details: () => [...checklistQueryKeys.all, 'detail'] as const,
  detail: (checklistId: number) => [...checklistQueryKeys.details(), checklistId] as const,
};
