import { isChecklistPresetType, isChecklistStage } from '../constants/checklist';
import type {
  ActiveChecklist,
  CheckItem,
  CheckItemPage,
  ChecklistDetail,
  ChecklistItem,
  ChecklistPage,
  ChecklistPreset,
  ChecklistStage,
  ChecklistSummary,
  CreatedChecklist,
} from '../types/Checklist';
import {
  readArray,
  readBoolean,
  readInteger,
  readNullableInteger,
  readNullableString,
  readRecord,
  readString,
  readUtcDateTime,
} from './responseParsers';

const parseStage = (value: unknown): ChecklistStage => {
  if (!isChecklistStage(value)) throw new Error('체크리스트 단계 응답이 올바르지 않습니다.');
  return value;
};

const parseCheckItem = (value: unknown): CheckItem => {
  const item = readRecord(value);
  return {
    checkItemId: readInteger(item, 'checkItemId', 1),
    stage: parseStage(item.stage),
    question: readString(item, 'question', { maximumCodePoints: 200 }),
    guide: readNullableString(item, 'guide'),
  };
};

const parsePage = (value: unknown) => {
  const result = readRecord(value);
  return {
    result,
    content: readArray(result, 'content'),
    page: readInteger(result, 'page'),
    size: readInteger(result, 'size', 1),
    totalElements: readInteger(result, 'totalElements'),
    totalPages: readInteger(result, 'totalPages'),
    hasNext: readBoolean(result, 'hasNext'),
  };
};

export const parseCheckItemPage = (value: unknown): CheckItemPage => {
  const metadata = parsePage(value);
  return {
    content: metadata.content.map(parseCheckItem),
    page: metadata.page,
    size: metadata.size,
    totalElements: metadata.totalElements,
    totalPages: metadata.totalPages,
    hasNext: metadata.hasNext,
  };
};

export const parseChecklistPreset = (value: unknown): ChecklistPreset => {
  const result = readRecord(value);
  const resultStage = parseStage(result.stage);
  if (!isChecklistPresetType(result.presetType)) throw new Error('프리셋 응답이 올바르지 않습니다.');

  return {
    presetType: result.presetType,
    stage: resultStage,
    items: readArray(result, 'items').map((value) => {
      const item = readRecord(value);
      const parsedItem = parseCheckItem({ ...item, stage: resultStage });
      return { ...parsedItem, order: readInteger(item, 'order') };
    }),
  };
};

const parseChecklistSummary = (value: unknown): ChecklistSummary => {
  const result = readRecord(value);
  return {
    checklistId: readInteger(result, 'checklistId', 1),
    name: readString(result, 'name'),
    stage: parseStage(result.stage),
    itemCount: readInteger(result, 'itemCount'),
    assignedPropertyCount: readInteger(result, 'assignedPropertyCount'),
    updatedAt: readUtcDateTime(result, 'updatedAt'),
  };
};

export const parseChecklistPage = (value: unknown): ChecklistPage => {
  const metadata = parsePage(value);
  return {
    content: metadata.content.map(parseChecklistSummary),
    page: metadata.page,
    size: metadata.size,
    totalElements: metadata.totalElements,
    totalPages: metadata.totalPages,
    hasNext: metadata.hasNext,
  };
};

const parseChecklistItem = (value: unknown): ChecklistItem => {
  const item = readRecord(value);
  const checklistItemId = readInteger(item, 'checklistItemId', 1);
  const origin = readString(item, 'origin');
  const sourceCheckItemId = readNullableInteger(item, 'sourceCheckItemId', 1);
  const checkItemId = readNullableInteger(item, 'checkItemId', 1);
  const question = readString(item, 'question', { maximumCodePoints: 200 });
  const guide = readNullableString(item, 'guide');
  const order = readInteger(item, 'order', 1);

  if (origin === 'PROVIDED') {
    if (sourceCheckItemId === null || (checkItemId !== null && checkItemId !== sourceCheckItemId)) {
      throw new Error('PROVIDED 항목 출처 응답이 올바르지 않습니다.');
    }
    return { checklistItemId, origin, sourceCheckItemId, checkItemId, question, guide, order };
  }

  if (origin === 'CUSTOM') {
    if (sourceCheckItemId !== null || checkItemId !== null || guide !== null) {
      throw new Error('CUSTOM 항목 출처 응답이 올바르지 않습니다.');
    }
    return { checklistItemId, origin, sourceCheckItemId, checkItemId, question, guide, order };
  }

  throw new Error('체크리스트 항목 origin 응답이 올바르지 않습니다.');
};

export const parseChecklistDetail = (value: unknown): ChecklistDetail => {
  const result = readRecord(value);
  const items = readArray(result, 'items').map(parseChecklistItem);
  const itemCount = readInteger(result, 'itemCount');
  if (items.length !== itemCount) throw new Error('체크리스트 항목 수 응답이 올바르지 않습니다.');

  return {
    checklistId: readInteger(result, 'checklistId', 1),
    name: readString(result, 'name'),
    stage: parseStage(result.stage),
    items,
    itemCount,
    assignedPropertyCount: readInteger(result, 'assignedPropertyCount'),
    createdAt: readUtcDateTime(result, 'createdAt'),
    updatedAt: readUtcDateTime(result, 'updatedAt'),
  };
};

export const parseCreatedChecklist = (value: unknown): CreatedChecklist => parseChecklistDetail(value);

export const parseActiveChecklist = (value: unknown): ActiveChecklist => {
  const result = readRecord(value);
  return {
    propertyId: readInteger(result, 'propertyId', 1),
    stage: parseStage(result.stage),
    checklistId: readInteger(result, 'checklistId', 1),
    name: readString(result, 'name'),
    itemCount: readInteger(result, 'itemCount'),
  };
};

export const parseNoChecklistContent = (value: unknown): undefined => {
  if (value !== undefined) throw new Error('본문 없는 응답이 필요합니다.');
  return undefined;
};
