import { isChecklistStage } from '../constants/checklist';
import { isVisitItemStatus, isVisitStatus } from '../constants/visit';
import type {
  VisitCompletionResult,
  VisitDetail,
  VisitItemMemoUpdateResult,
  VisitItemOrigin,
  VisitItemStatusUpdateResult,
  VisitListItem,
  VisitPage,
  VisitSnapshotItem,
  VisitStageSnapshot,
  VisitSummary,
} from '../types/Visit';
import {
  readArray,
  readBoolean,
  readInteger,
  readNullableInteger,
  readNullableString,
  readNullableUtcDateTime,
  readRecord,
  readString,
  readUtcDateTime,
} from './responseParsers';

export const parseVisitSummary = (value: unknown): VisitSummary => {
  const record = readRecord(value);
  return {
    totalCount: readInteger(record, 'totalCount'),
    checkedCount: readInteger(record, 'checkedCount'),
    goodCount: readInteger(record, 'goodCount'),
    cautionCount: readInteger(record, 'cautionCount'),
    unconfirmedCount: readInteger(record, 'unconfirmedCount'),
  };
};

const parseVisitStatus = (record: Record<string, unknown>): VisitListItem['status'] => {
  const status = readString(record, 'status');
  if (!isVisitStatus(status)) throw new Error('방문 상태 응답이 올바르지 않습니다.');
  return status;
};

const parseVisitItemStatus = (record: Record<string, unknown>) => {
  const status = readString(record, 'status');
  if (!isVisitItemStatus(status)) throw new Error('방문 항목 상태 응답이 올바르지 않습니다.');
  return status;
};

const parseVisitListItem = (value: unknown): VisitListItem => {
  const record = readRecord(value);
  return {
    visitId: readInteger(record, 'visitId', 1),
    status: parseVisitStatus(record),
    startedAt: readUtcDateTime(record, 'startedAt'),
    completedAt: readNullableUtcDateTime(record, 'completedAt'),
    summary: parseVisitSummary(record.summary),
  };
};

export const parseVisitPage = (value: unknown): VisitPage => {
  const record = readRecord(value);
  return {
    content: readArray(record, 'content').map(parseVisitListItem),
    page: readInteger(record, 'page'),
    size: readInteger(record, 'size', 1),
    totalElements: readInteger(record, 'totalElements'),
    totalPages: readInteger(record, 'totalPages'),
    hasNext: readBoolean(record, 'hasNext'),
  };
};

const parseVisitItemOrigin = (record: Record<string, unknown>): VisitItemOrigin => {
  const origin = readString(record, 'origin');
  if (origin !== 'PROVIDED' && origin !== 'CUSTOM') throw new Error('방문 항목 origin 응답이 올바르지 않습니다.');
  return origin;
};

const parseSnapshotItem = (value: unknown): VisitSnapshotItem => {
  const record = readRecord(value);
  const origin = parseVisitItemOrigin(record);
  const sourceCheckItemId = readNullableInteger(record, 'sourceCheckItemId', 1);
  const guide = readNullableString(record, 'guide');
  if (origin === 'PROVIDED' && sourceCheckItemId === null)
    throw new Error('PROVIDED 방문 항목의 제공 출처가 올바르지 않습니다.');
  if (origin === 'CUSTOM' && (sourceCheckItemId !== null || guide !== null))
    throw new Error('CUSTOM 방문 항목의 제공 출처나 안내가 올바르지 않습니다.');

  const statusVersion = readInteger(record, 'statusVersion');
  const statusSavedAt = readUtcDateTime(record, 'statusSavedAt');
  const version = readInteger(record, 'version');
  const savedAt = readUtcDateTime(record, 'savedAt');
  if (version !== statusVersion || savedAt !== statusSavedAt) {
    throw new Error('상태의 v1.1 필드와 deprecated 별칭이 일치하지 않습니다.');
  }

  const inlineMemo = readString(record, 'inlineMemo', { allowEmpty: true, maximumCodePoints: 200 });
  if (inlineMemo.includes('\r') || inlineMemo.includes('\n')) throw new Error('인라인 메모 응답은 한 줄이어야 합니다.');

  return {
    visitItemId: readInteger(record, 'visitItemId', 1),
    origin,
    sourceChecklistItemId: readNullableInteger(record, 'sourceChecklistItemId', 1),
    sourceCheckItemId,
    question: readString(record, 'question', { maximumCodePoints: 200 }),
    guide,
    order: readInteger(record, 'order', 1),
    status: parseVisitItemStatus(record),
    statusVersion,
    statusSavedAt,
    inlineMemo,
    memoVersion: readInteger(record, 'memoVersion'),
    memoSavedAt: readNullableUtcDateTime(record, 'memoSavedAt'),
    version,
    savedAt,
  };
};

const parseStage = (value: unknown): VisitStageSnapshot => {
  const record = readRecord(value);
  const stage = readString(record, 'stage');
  if (!isChecklistStage(stage)) throw new Error('방문 단계 응답이 올바르지 않습니다.');
  return {
    stage,
    sourceChecklistId: readNullableInteger(record, 'sourceChecklistId', 1),
    checklistName: readString(record, 'checklistName'),
    items: readArray(record, 'items').map(parseSnapshotItem),
    summary: parseVisitSummary(record.summary),
  };
};

export const parseVisitDetail = (value: unknown): VisitDetail => {
  const record = readRecord(value);
  return {
    visitId: readInteger(record, 'visitId', 1),
    propertyId: readInteger(record, 'propertyId', 1),
    status: parseVisitStatus(record),
    startedAt: readUtcDateTime(record, 'startedAt'),
    completedAt: readNullableUtcDateTime(record, 'completedAt'),
    updatedAt: readUtcDateTime(record, 'updatedAt'),
    stages: readArray(record, 'stages').map(parseStage),
    summary: parseVisitSummary(record.summary),
  };
};

export const parseVisitItemStatusUpdate = (value: unknown): VisitItemStatusUpdateResult => {
  const record = readRecord(value);
  const item = readRecord(record.item);
  const statusVersion = readInteger(item, 'statusVersion');
  const statusSavedAt = readUtcDateTime(item, 'statusSavedAt');
  if (readInteger(item, 'version') !== statusVersion || readUtcDateTime(item, 'savedAt') !== statusSavedAt) {
    throw new Error('상태 저장 응답의 deprecated 별칭이 일치하지 않습니다.');
  }
  return {
    item: {
      visitItemId: readInteger(item, 'visitItemId', 1),
      status: parseVisitItemStatus(item),
      statusVersion,
      statusSavedAt,
    },
    stageSummary: parseVisitSummary(record.stageSummary),
    visitSummary: parseVisitSummary(record.visitSummary),
  };
};

export const parseVisitItemMemoUpdate = (value: unknown): VisitItemMemoUpdateResult => {
  const record = readRecord(value);
  const memo = readString(record, 'memo', { allowEmpty: true, maximumCodePoints: 200 });
  if (memo.includes('\r') || memo.includes('\n')) throw new Error('인라인 메모 응답은 한 줄이어야 합니다.');
  return {
    visitItemId: readInteger(record, 'visitItemId', 1),
    memo,
    memoVersion: readInteger(record, 'memoVersion'),
    memoSavedAt: readUtcDateTime(record, 'memoSavedAt'),
  };
};

export const parseVisitCompletion = (value: unknown): VisitCompletionResult => {
  const record = readRecord(value);
  const status = parseVisitStatus(record);
  if (status !== 'COMPLETED') throw new Error('완료 방문 상태 응답이 올바르지 않습니다.');
  return {
    visitId: readInteger(record, 'visitId', 1),
    status,
    startedAt: readUtcDateTime(record, 'startedAt'),
    completedAt: readUtcDateTime(record, 'completedAt'),
    summary: parseVisitSummary(record.summary),
  };
};
