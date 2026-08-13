import type { QueryClient } from '@tanstack/react-query';
import type {
  VisitCompletionResult,
  VisitDetail,
  VisitItemMemoUpdateResult,
  VisitItemStatusUpdateResult,
  VisitItemUpdateResult,
  VisitSummary,
} from '../types/Visit';
import { propertyQueryKeys } from './propertyQueryKeys';
import { visitQueryKeys } from './visitQueryKeys';

export const invalidateVisitAggregates = (
  queryClient: QueryClient,
  propertyId: number,
  { includeVisitLists = true }: { includeVisitLists?: boolean } = {},
) => {
  const requests: Promise<unknown>[] = [
    queryClient.invalidateQueries({ queryKey: propertyQueryKeys.detail(propertyId), exact: true }),
    queryClient.invalidateQueries({ queryKey: propertyQueryKeys.lists() }),
  ];

  if (includeVisitLists) {
    requests.push(queryClient.invalidateQueries({ queryKey: visitQueryKeys.propertyLists(propertyId) }));
  }

  return Promise.all(requests);
};

const latestStatusSavedAt = (detail: VisitDetail): number =>
  Math.max(...detail.stages.flatMap((stage) => stage.items.map((item) => Date.parse(item.statusSavedAt))), 0);

const summarizeStatuses = (statuses: VisitDetail['stages'][number]['items'][number]['status'][]): VisitSummary => ({
  totalCount: statuses.length,
  checkedCount: statuses.filter((status) => status !== 'UNCONFIRMED').length,
  goodCount: statuses.filter((status) => status === 'GOOD').length,
  cautionCount: statuses.filter((status) => status === 'CAUTION').length,
  unconfirmedCount: statuses.filter((status) => status === 'UNCONFIRMED').length,
});

const summariesEqual = (left: VisitSummary, right: VisitSummary): boolean =>
  left.totalCount === right.totalCount &&
  left.checkedCount === right.checkedCount &&
  left.goodCount === right.goodCount &&
  left.cautionCount === right.cautionCount &&
  left.unconfirmedCount === right.unconfirmedCount;

export const applyVisitItemStatusUpdate = (
  detail: VisitDetail | undefined,
  result: VisitItemStatusUpdateResult,
): VisitDetail | undefined => {
  if (detail === undefined) return detail;

  const responseSavedAt = Date.parse(result.item.statusSavedAt);
  const previousLatestSavedAt = latestStatusSavedAt(detail);
  let matchedStage = false;

  const updatedStages = detail.stages.map((stage) => {
    const hasItem = stage.items.some((item) => item.visitItemId === result.item.visitItemId);
    if (!hasItem) return stage;
    matchedStage = true;
    return {
      ...stage,
      items: stage.items.map((item) =>
        item.visitItemId === result.item.visitItemId && result.item.statusVersion >= item.statusVersion
          ? {
              ...item,
              ...result.item,
              version: result.item.statusVersion,
              savedAt: result.item.statusSavedAt,
            }
          : item,
      ),
    };
  });

  if (!matchedStage) return detail;

  const updatedStage = updatedStages.find((stage) =>
    stage.items.some((item) => item.visitItemId === result.item.visitItemId),
  );
  const serverAggregatesMatchConfirmedItems =
    updatedStage !== undefined &&
    summariesEqual(result.stageSummary, summarizeStatuses(updatedStage.items.map((item) => item.status))) &&
    summariesEqual(
      result.visitSummary,
      summarizeStatuses(updatedStages.flatMap((stage) => stage.items.map((item) => item.status))),
    );
  const mayApplyAggregate =
    responseSavedAt > previousLatestSavedAt ||
    (responseSavedAt === previousLatestSavedAt && serverAggregatesMatchConfirmedItems);
  const stages = updatedStages.map((stage) =>
    stage === updatedStage && mayApplyAggregate ? { ...stage, summary: result.stageSummary } : stage,
  );

  return {
    ...detail,
    stages,
    summary: mayApplyAggregate ? result.visitSummary : detail.summary,
    updatedAt: responseSavedAt >= Date.parse(detail.updatedAt) ? result.item.statusSavedAt : detail.updatedAt,
  };
};

export const applyVisitItemMemoUpdate = (
  detail: VisitDetail | undefined,
  result: VisitItemMemoUpdateResult,
): VisitDetail | undefined => {
  if (detail === undefined) return detail;

  let matchedItem = false;
  const stages = detail.stages.map((stage) => ({
    ...stage,
    items: stage.items.map((item) => {
      if (item.visitItemId !== result.visitItemId) return item;
      matchedItem = true;
      return result.memoVersion >= item.memoVersion
        ? {
            ...item,
            inlineMemo: result.memo,
            memoVersion: result.memoVersion,
            memoSavedAt: result.memoSavedAt,
          }
        : item;
    }),
  }));

  if (!matchedItem) return detail;
  return {
    ...detail,
    stages,
    updatedAt: Date.parse(result.memoSavedAt) >= Date.parse(detail.updatedAt) ? result.memoSavedAt : detail.updatedAt,
  };
};

/** @deprecated v1.0 상태 응답 호환 전용이다. */
export const applyVisitItemUpdate = (
  detail: VisitDetail | undefined,
  result: VisitItemUpdateResult,
): VisitDetail | undefined =>
  applyVisitItemStatusUpdate(detail, {
    ...result,
    item: {
      visitItemId: result.item.visitItemId,
      status: result.item.status,
      statusVersion: result.item.version,
      statusSavedAt: result.item.savedAt,
    },
  });

export const applyVisitCompletion = (
  detail: VisitDetail | undefined,
  result: VisitCompletionResult,
): VisitDetail | undefined =>
  detail === undefined || detail.visitId !== result.visitId
    ? detail
    : {
        ...detail,
        status: result.status,
        startedAt: result.startedAt,
        completedAt: detail.completedAt ?? result.completedAt,
        summary: result.summary,
        updatedAt: detail.completedAt === null ? (result.completedAt ?? detail.updatedAt) : detail.updatedAt,
      };
