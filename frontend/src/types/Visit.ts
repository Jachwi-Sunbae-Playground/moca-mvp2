import type { ChecklistStage } from './Checklist';

export type VisitStatus = 'IN_PROGRESS' | 'COMPLETED';
export type VisitItemStatus = 'GOOD' | 'CAUTION' | 'UNCONFIRMED';
export type VisitItemOrigin = 'PROVIDED' | 'CUSTOM';

export type VisitSummary = {
  totalCount: number;
  checkedCount: number;
  goodCount: number;
  cautionCount: number;
  unconfirmedCount: number;
};

export type VisitListItem = {
  visitId: number;
  status: VisitStatus;
  startedAt: string;
  completedAt: string | null;
  summary: VisitSummary;
};

export type VisitPage = {
  content: VisitListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type VisitSnapshotItem = {
  visitItemId: number;
  origin: VisitItemOrigin;
  sourceChecklistItemId: number | null;
  sourceCheckItemId: number | null;
  question: string;
  guide: string | null;
  order: number;
  status: VisitItemStatus;
  statusVersion: number;
  statusSavedAt: string;
  inlineMemo: string;
  memoVersion: number;
  memoSavedAt: string | null;
  /** @deprecated statusVersion의 v1.0 별칭이다. */
  version: number;
  /** @deprecated statusSavedAt의 v1.0 별칭이다. */
  savedAt: string;
};

export type VisitStageSnapshot = {
  stage: ChecklistStage;
  sourceChecklistId: number | null;
  checklistName: string;
  items: VisitSnapshotItem[];
  summary: VisitSummary;
};

export type VisitDetail = {
  visitId: number;
  propertyId: number;
  status: VisitStatus;
  startedAt: string;
  completedAt: string | null;
  updatedAt: string;
  stages: VisitStageSnapshot[];
  summary: VisitSummary;
};

export type VisitItemStatusUpdateResult = {
  item: Pick<VisitSnapshotItem, 'visitItemId' | 'status' | 'statusVersion' | 'statusSavedAt'>;
  stageSummary: VisitSummary;
  visitSummary: VisitSummary;
};

export type VisitItemMemoUpdateResult = {
  visitItemId: number;
  memo: string;
  memoVersion: number;
  memoSavedAt: string;
};

/** @deprecated v1.0 상태 응답 호환 전용이다. */
export type VisitItemUpdateResult = {
  item: Pick<VisitSnapshotItem, 'visitItemId' | 'status' | 'version' | 'savedAt'>;
  stageSummary: VisitSummary;
  visitSummary: VisitSummary;
};

export type VisitCompletionResult = Pick<VisitDetail, 'visitId' | 'status' | 'startedAt' | 'completedAt' | 'summary'>;
