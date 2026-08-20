import { isChecklistStage } from '../constants/checklist';
import type {
  ActiveChecklist,
  CheckItem,
  CheckItemPage,
  ChecklistDetail,
  ChecklistPage,
  ChecklistPreset,
  ChecklistStage,
  ChecklistSummary,
  CreatedChecklist,
} from '../types/Checklist';
import { readArray, readBoolean, readInteger, readNullableInteger, readRecord, readString } from './responseParsers';

const unknownDate = '1970-01-01T00:00:00Z';

const parseStage = (value: unknown): ChecklistStage => {
  if (!isChecklistStage(value)) throw new Error('체크리스트 단계 응답이 올바르지 않습니다.');
  return value;
};

const parseItemType = (value: unknown): CheckItem['itemType'] => {
  if (value !== 'CORE' && value !== 'OPTIONAL') throw new Error('체크 항목 유형 응답이 올바르지 않습니다.');
  return value;
};

const parseCheckItem = (value: unknown): CheckItem => {
  const item = readRecord(value);
  return {
    checkItemId: readInteger(item, 'id', 1),
    stage: parseStage(item.stage),
    itemType: parseItemType(item.itemType),
    question: readString(item, 'question', { maximumCodePoints: 200 }),
    guide: null,
  };
};

export const parseCheckItemPage = (value: unknown): CheckItemPage => {
  const result = readRecord(value);
  const stage = parseStage(result.stage);
  const items = readArray(result, 'items').map(parseCheckItem);
  const totalElements = readInteger(result, 'totalCount');
  if (items.some((item) => item.stage !== stage) || items.length !== totalElements) {
    throw new Error('체크 항목 집계 응답이 올바르지 않습니다.');
  }
  return {
    content: items,
    page: 0,
    size: Math.max(1, items.length),
    totalElements,
    totalPages: totalElements === 0 ? 0 : 1,
    hasNext: false,
  };
};

/** 원룸 프리셋은 최종 API에 없으며, 체크 항목 조회 결과를 화면 시작 구성으로만 변환한다. */
export const toChecklistPreset = (stage: ChecklistStage, items: CheckItem[]): ChecklistPreset => ({
  presetType: 'ONE_ROOM',
  stage,
  items: items.map((item, order) => ({ ...item, order })),
});

const parseChecklistSummary = (value: unknown): ChecklistSummary => {
  const result = readRecord(value);
  return {
    checklistId: readInteger(result, 'id', 1),
    name: readString(result, 'name'),
    stage: parseStage(result.stage),
    itemCount: readInteger(result, 'itemCount'),
    assignedPropertyCount: 0,
    updatedAt: unknownDate,
  };
};

export const parseChecklistPage = (value: unknown): ChecklistPage => {
  const result = readRecord(value);
  const content = readArray(result, 'items').map(parseChecklistSummary);
  const totalElements = readInteger(result, 'totalCount');
  if (content.length !== totalElements) throw new Error('체크리스트 집계 응답이 올바르지 않습니다.');
  return {
    content,
    page: 0,
    size: Math.max(1, content.length),
    totalElements,
    totalPages: totalElements === 0 ? 0 : 1,
    hasNext: false,
  };
};

const parseChecklistItem = (value: unknown) => {
  const item = readRecord(value);
  const systemCheckItemId = readInteger(item, 'systemCheckItemId', 1);
  return {
    checklistItemId: systemCheckItemId,
    origin: 'PROVIDED' as const,
    sourceCheckItemId: systemCheckItemId,
    checkItemId: systemCheckItemId,
    itemType: parseItemType(item.itemType),
    question: readString(item, 'question', { maximumCodePoints: 200 }),
    guide: null,
    order: readInteger(item, 'displayOrder', 1),
    active: 'active' in item ? readBoolean(item, 'active') : true,
  };
};

export const parseChecklistDetail = (value: unknown): ChecklistDetail => {
  const result = readRecord(value);
  const items = readArray(result, 'items')
    .map(parseChecklistItem)
    .sort((a, b) => a.order - b.order);
  const itemCount = readInteger(result, 'itemCount');
  if (items.length !== itemCount) throw new Error('체크리스트 항목 수 응답이 올바르지 않습니다.');
  return {
    checklistId: readInteger(result, 'id', 1),
    name: readString(result, 'name'),
    stage: parseStage(result.stage),
    items,
    itemCount,
    assignedPropertyCount: 0,
    createdAt: unknownDate,
    updatedAt: unknownDate,
  };
};

export const parseCreatedChecklist = (value: unknown): CreatedChecklist => parseChecklistDetail(value);

export const parseActiveChecklist = (value: unknown): ActiveChecklist => {
  const result = readRecord(value);
  const items = readArray(result, 'items');
  return {
    propertyChecklistId: readInteger(result, 'id', 1),
    propertyId: readInteger(result, 'propertyId', 1),
    stage: parseStage(result.stage),
    checklistId: readNullableInteger(result, 'sourceChecklistId', 1),
    name: readString(result, 'checklistName'),
    itemCount: items.length,
  };
};

export const parseNoChecklistContent = (value: unknown): undefined => {
  if (value !== undefined) throw new Error('본문 없는 응답이 필요합니다.');
  return undefined;
};
