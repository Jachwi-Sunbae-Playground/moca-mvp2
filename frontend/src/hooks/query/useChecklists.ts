import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { fetchCheckItems, fetchChecklistDetail, fetchChecklistPreset, fetchChecklists } from '../../apis/checklistApi';
import { checklistQueryKeys } from '../../app/checklistQueryKeys';
import type { ChecklistPresetType, ChecklistStage } from '../../types/Checklist';
import type { PublicConfig } from '../../types/PublicConfig';

export const useChecklistList = (config: PublicConfig, stage: ChecklistStage) =>
  useInfiniteQuery({
    queryKey: checklistQueryKeys.list(stage),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => fetchChecklists(config, { stage, page: pageParam }, signal),
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
  });

export const useCheckItemSearch = (config: PublicConfig, stage: ChecklistStage, query: string, enabled = true) =>
  useInfiniteQuery({
    queryKey: checklistQueryKeys.checkItems(stage, query),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) => fetchCheckItems(config, { stage, query, page: pageParam }, signal),
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
    enabled,
  });

export const useActiveCheckItems = (config: PublicConfig, stage: ChecklistStage) =>
  useQuery({
    queryKey: checklistQueryKeys.activeCheckItems(stage),
    queryFn: ({ signal }) => fetchCheckItems(config, { stage, query: '', page: 0, size: 100 }, signal),
  });

export const useChecklistPreset = (
  config: PublicConfig,
  stage: ChecklistStage,
  presetType: ChecklistPresetType,
  enabled: boolean,
) =>
  useQuery({
    queryKey: checklistQueryKeys.preset(stage, presetType),
    queryFn: ({ signal }) => fetchChecklistPreset(config, stage, presetType, signal),
    enabled,
  });

export const useChecklistDetail = (config: PublicConfig, checklistId: number) =>
  useQuery({
    queryKey: checklistQueryKeys.detail(checklistId),
    queryFn: ({ signal }) => fetchChecklistDetail(config, checklistId, signal),
  });
