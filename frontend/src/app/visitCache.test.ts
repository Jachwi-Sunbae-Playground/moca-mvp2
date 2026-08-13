import { describe, expect, it } from 'vitest';
import { visitDetailFixture } from '../test/visitFixtures';
import type { VisitDetail, VisitItemStatusUpdateResult } from '../types/Visit';
import { applyVisitCompletion, applyVisitItemMemoUpdate, applyVisitItemStatusUpdate } from './visitCache';

const detail = visitDetailFixture as VisitDetail;

describe('FE-4 방문 캐시 병합', () => {
  it('서버가 반환한 상태·version·savedAt과 집계를 반영한다', () => {
    const result: VisitItemStatusUpdateResult = {
      item: { visitItemId: 501, status: 'GOOD', statusVersion: 1, statusSavedAt: '2026-08-11T04:02:00Z' },
      stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 1, cautionCount: 0, unconfirmedCount: 0 },
      visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
    };

    const updated = applyVisitItemStatusUpdate(detail, result);
    expect(updated?.stages[0]?.items[0]).toMatchObject({
      ...result.item,
      version: 1,
      inlineMemo: '',
      memoVersion: 0,
      memoSavedAt: null,
    });
    expect(updated?.summary).toEqual(result.visitSummary);
  });

  it('서로 다른 항목의 늦은 과거 응답이 더 최신 서버 집계를 되돌리지 않는다', () => {
    const later: VisitItemStatusUpdateResult = {
      item: { visitItemId: 503, status: 'CAUTION', statusVersion: 1, statusSavedAt: '2026-08-11T04:03:00Z' },
      stageSummary: { totalCount: 2, checkedCount: 2, goodCount: 1, cautionCount: 1, unconfirmedCount: 0 },
      visitSummary: { totalCount: 3, checkedCount: 3, goodCount: 2, cautionCount: 1, unconfirmedCount: 0 },
    };
    const earlier: VisitItemStatusUpdateResult = {
      item: { visitItemId: 501, status: 'GOOD', statusVersion: 1, statusSavedAt: '2026-08-11T04:02:00Z' },
      stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 1, cautionCount: 0, unconfirmedCount: 0 },
      visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
    };

    const withLater = applyVisitItemStatusUpdate(detail, later);
    const withReorderedResponses = applyVisitItemStatusUpdate(withLater, earlier);
    expect(withReorderedResponses?.stages[0]?.items[0]).toMatchObject(earlier.item);
    expect(withReorderedResponses?.summary).toEqual(later.visitSummary);
  });

  it('저장 시각이 같아도 현재 서버 확정 항목과 맞지 않는 과거 집계는 적용하지 않는다', () => {
    const later: VisitItemStatusUpdateResult = {
      item: { visitItemId: 503, status: 'CAUTION', statusVersion: 1, statusSavedAt: '2026-08-11T04:03:00Z' },
      stageSummary: { totalCount: 2, checkedCount: 2, goodCount: 1, cautionCount: 1, unconfirmedCount: 0 },
      visitSummary: { totalCount: 3, checkedCount: 3, goodCount: 2, cautionCount: 1, unconfirmedCount: 0 },
    };
    const earlierWithSameTimestamp: VisitItemStatusUpdateResult = {
      item: { visitItemId: 501, status: 'GOOD', statusVersion: 1, statusSavedAt: '2026-08-11T04:03:00Z' },
      stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 1, cautionCount: 0, unconfirmedCount: 0 },
      visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
    };

    const withLater = applyVisitItemStatusUpdate(detail, later);
    const withTiedTimestamp = applyVisitItemStatusUpdate(withLater, earlierWithSameTimestamp);
    expect(withTiedTimestamp?.summary).toEqual(later.visitSummary);
  });

  it('메모 결과는 상태·상태 버전·집계를 변경하지 않고 메모 채널만 병합한다', () => {
    const updated = applyVisitItemMemoUpdate(detail, {
      visitItemId: 501,
      memo: '창틀 습기 확인',
      memoVersion: 1,
      memoSavedAt: '2026-08-11T04:04:00Z',
    });

    expect(updated?.stages[0]?.items[0]).toMatchObject({
      status: 'UNCONFIRMED',
      statusVersion: 0,
      inlineMemo: '창틀 습기 확인',
      memoVersion: 1,
      memoSavedAt: '2026-08-11T04:04:00Z',
    });
    expect(updated?.stages[0]?.summary).toEqual(detail.stages[0]?.summary);
    expect(updated?.summary).toEqual(detail.summary);
  });

  it('이미 완료된 방문에 늦은 완료 응답이 와도 최초 completedAt과 최신 updatedAt을 보존한다', () => {
    const completed = {
      ...detail,
      status: 'COMPLETED' as const,
      completedAt: '2026-08-11T04:05:00Z',
      updatedAt: '2026-08-11T04:07:00Z',
    };

    const updated = applyVisitCompletion(completed, {
      visitId: 31,
      status: 'COMPLETED',
      startedAt: detail.startedAt,
      completedAt: '2026-08-11T04:06:00Z',
      summary: detail.summary,
    });

    expect(updated?.completedAt).toBe('2026-08-11T04:05:00Z');
    expect(updated?.updatedAt).toBe('2026-08-11T04:07:00Z');
  });
});
