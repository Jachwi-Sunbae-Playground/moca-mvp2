import type { PublicConfig } from '../types/PublicConfig';
import type { ChecklistPresetType, ChecklistStage, LegacyChecklistDetail } from '../types/Checklist';
import { ApiError, apiRequest } from './apiClient';
import {
  parseActiveChecklist,
  parseCheckItemPage,
  parseChecklistDetail,
  parseChecklistPage,
  parseChecklistPreset,
  parseCreatedChecklist,
  parseNoChecklistContent,
} from './checklistParsers';
import type {
  AssignActiveChecklistRequestDto,
  CreateChecklistV11RequestDto,
  CreateChecklistRequestDto,
  UpdateChecklistV11RequestDto,
  UpdateChecklistRequestDto,
} from './dtos/ChecklistDto';

const toLegacyChecklistDetail = (detail: Awaited<ReturnType<typeof fetchChecklistDetail>>): LegacyChecklistDetail => {
  const items = detail.items.map((item) => {
    if (item.origin !== 'PROVIDED') {
      throw new ApiError({ kind: 'invalid-response' });
    }
    return {
      checkItemId: item.sourceCheckItemId,
      stage: detail.stage,
      question: item.question,
      guide: item.guide,
    };
  });
  return { ...detail, items };
};

export const fetchCheckItems = (
  config: PublicConfig,
  input: { stage: ChecklistStage; query: string; page: number; size?: number },
  signal?: AbortSignal,
) => {
  const search = new URLSearchParams({ stage: input.stage, page: String(input.page), size: String(input.size ?? 20) });
  const query = input.query.trim();
  if (query.length > 0) search.set('query', query);
  return apiRequest({
    config,
    path: `/api/check-items?${search}`,
    signal,
    parseData: (value) => {
      const result = parseCheckItemPage(value);
      if (result.content.some((item) => item.stage !== input.stage)) throw new Error('stage');
      return result;
    },
  });
};

export const fetchChecklistPreset = (
  config: PublicConfig,
  stage: ChecklistStage,
  selectedPreset: ChecklistPresetType,
  signal?: AbortSignal,
) => {
  const search = new URLSearchParams({ stage, presetType: selectedPreset });
  return apiRequest({
    config,
    path: `/api/checklist-presets?${search}`,
    signal,
    parseData: (value) => {
      const result = parseChecklistPreset(value);
      if (result.stage !== stage || result.presetType !== selectedPreset) throw new Error('preset');
      return result;
    },
  });
};

export const fetchChecklists = (
  config: PublicConfig,
  input: { stage: ChecklistStage; page: number; size?: number },
  signal?: AbortSignal,
) => {
  const search = new URLSearchParams({ stage: input.stage, page: String(input.page), size: String(input.size ?? 20) });
  return apiRequest({
    config,
    path: `/api/checklists?${search}`,
    signal,
    parseData: (value) => {
      const result = parseChecklistPage(value);
      if (result.content.some((checklist) => checklist.stage !== input.stage)) throw new Error('stage');
      return result;
    },
  });
};

export const createChecklistV11 = (config: PublicConfig, body: CreateChecklistV11RequestDto) =>
  apiRequest({
    config,
    path: '/api/checklists',
    method: 'POST',
    body,
    parseData: (value) => {
      const result = parseCreatedChecklist(value);
      if (result.stage !== body.stage) throw new Error('stage');
      return result;
    },
  });

/** @deprecated PROVIDED 전용 v1.0 화면 호환 요청이다. */
export const createChecklist = (config: PublicConfig, body: CreateChecklistRequestDto) =>
  apiRequest({
    config,
    path: '/api/checklists',
    method: 'POST',
    body,
    parseData: (value) => {
      const result = parseCreatedChecklist(value);
      if (result.stage !== body.stage) throw new Error('stage');
      return result;
    },
  });

export const fetchChecklistDetail = (config: PublicConfig, checklistId: number, signal?: AbortSignal) =>
  apiRequest({
    config,
    path: `/api/checklists/${checklistId}`,
    signal,
    parseData: (value) => {
      const result = parseChecklistDetail(value);
      if (result.checklistId !== checklistId) throw new Error('checklistId');
      return result;
    },
  });

/** @deprecated PROVIDED 전용 v1.0 편집 화면에서만 사용한다. CUSTOM은 무손실로 표현할 수 없어 거부한다. */
export const fetchLegacyChecklistDetail = async (
  config: PublicConfig,
  checklistId: number,
  signal?: AbortSignal,
): Promise<LegacyChecklistDetail> => {
  const detail = await fetchChecklistDetail(config, checklistId, signal);
  return toLegacyChecklistDetail(detail);
};

export const updateChecklistV11 = (config: PublicConfig, checklistId: number, body: UpdateChecklistV11RequestDto) =>
  apiRequest({
    config,
    path: `/api/checklists/${checklistId}`,
    method: 'PUT',
    body,
    parseData: (value) => {
      const result = parseChecklistDetail(value);
      if (result.checklistId !== checklistId) throw new Error('checklistId');
      return result;
    },
  });

/** @deprecated PROVIDED 전용 v1.0 화면 호환 요청이다. */
export const updateChecklist = async (
  config: PublicConfig,
  checklistId: number,
  body: UpdateChecklistRequestDto,
): Promise<LegacyChecklistDetail> => {
  const detail = await apiRequest({
    config,
    path: `/api/checklists/${checklistId}`,
    method: 'PUT',
    body,
    parseData: (value) => {
      const result = parseChecklistDetail(value);
      if (result.checklistId !== checklistId) throw new Error('checklistId');
      return result;
    },
  });
  return toLegacyChecklistDetail(detail);
};

export const removeChecklist = (config: PublicConfig, checklistId: number) =>
  apiRequest({
    config,
    path: `/api/checklists/${checklistId}`,
    method: 'DELETE',
    parseData: parseNoChecklistContent,
  });

export const assignActiveChecklist = (
  config: PublicConfig,
  propertyId: number,
  stage: ChecklistStage,
  body: AssignActiveChecklistRequestDto,
) =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/active-checklists/${stage}`,
    method: 'PUT',
    body,
    parseData: (value) => {
      const result = parseActiveChecklist(value);
      if (result.propertyId !== propertyId || result.stage !== stage || result.checklistId !== body.checklistId) {
        throw new Error('active-checklist');
      }
      return result;
    },
  });

export const removeActiveChecklist = (config: PublicConfig, propertyId: number, stage: ChecklistStage) =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/active-checklists/${stage}`,
    method: 'DELETE',
    parseData: parseNoChecklistContent,
  });
