import { useMutation } from '@tanstack/react-query';
import { useCallback, useEffect, useReducer, useRef } from 'react';
import { isVisitMemoVersionConflict, isVisitStatusVersionConflict } from '../../apis/visitErrorMessages';
import { fetchVisitDetail, updateVisitItemMemo, updateVisitItemStatus } from '../../apis/visitApi';
import { queryClient } from '../../app/queryClient';
import { applyVisitItemMemoUpdate, applyVisitItemStatusUpdate, invalidateVisitAggregates } from '../../app/visitCache';
import { visitMutationKeys, visitQueryKeys } from '../../app/visitQueryKeys';
import type { PublicConfig } from '../../types/PublicConfig';
import type { VisitDetail, VisitItemStatus, VisitSnapshotItem } from '../../types/Visit';
import { inlineMemoCodePointLength, isInlineMemoWithinLimit, removeInlineMemoLineBreaks } from '../../utils/inlineMemo';
import type { VisitItemAutosaveHandle } from './useVisitAutosaveRegistry';

export type VisitSavePhase = 'idle' | 'saving' | 'refreshing' | 'saved' | 'error';

type StatusRuntime = {
  confirmed: VisitItemStatus;
  version: number;
  savedAt: string;
  desired: VisitItemStatus | null;
  intentId: number;
  phase: VisitSavePhase;
  error: unknown;
  promise: Promise<boolean> | null;
};

type MemoRuntime = {
  confirmed: string;
  draft: string;
  version: number;
  savedAt: string | null;
  dirty: boolean;
  intentId: number;
  phase: VisitSavePhase;
  error: unknown;
  limitReached: boolean;
  debounceReady: boolean;
  forceFlush: boolean;
  timer: ReturnType<typeof setTimeout> | null;
  promise: Promise<boolean> | null;
};

type Runtime = { status: StatusRuntime; memo: MemoRuntime };

const createRuntime = (item: VisitSnapshotItem): Runtime => ({
  status: {
    confirmed: item.status,
    version: item.statusVersion,
    savedAt: item.statusSavedAt,
    desired: null,
    intentId: 0,
    phase: 'idle',
    error: null,
    promise: null,
  },
  memo: {
    confirmed: item.inlineMemo,
    draft: item.inlineMemo,
    version: item.memoVersion,
    savedAt: item.memoSavedAt,
    dirty: false,
    intentId: 0,
    phase: 'idle',
    error: null,
    limitReached: false,
    debounceReady: false,
    forceFlush: false,
    timer: null,
    promise: null,
  },
});

const findVisitItem = (detail: VisitDetail, visitItemId: number) => {
  for (const stage of detail.stages) {
    const item = stage.items.find((candidate) => candidate.visitItemId === visitItemId);
    if (item !== undefined) return { item, stageSummary: stage.summary };
  }
  return null;
};

export const useVisitItemAutosave = ({
  config,
  visitId,
  propertyId,
  item,
  announce,
  register,
  onActivityChange,
}: {
  config: PublicConfig;
  visitId: number;
  propertyId: number;
  item: VisitSnapshotItem;
  announce: (message: string) => void;
  register: (visitItemId: number, handle: VisitItemAutosaveHandle) => () => void;
  onActivityChange: () => void;
}) => {
  const runtimeRef = useRef<Runtime | null>(null);
  if (runtimeRef.current === null) runtimeRef.current = createRuntime(item);
  const runtime = runtimeRef.current;
  const mountedRef = useRef(true);
  const [, forceRender] = useReducer((value: number) => value + 1, 0);

  const statusMutation = useMutation({
    mutationKey: visitMutationKeys.statusUpdate(visitId, item.visitItemId),
    retry: false,
    mutationFn: ({ status, expectedStatusVersion }: { status: VisitItemStatus; expectedStatusVersion: number }) =>
      updateVisitItemStatus(config, visitId, item.visitItemId, { status, expectedStatusVersion }),
  });
  const memoMutation = useMutation({
    mutationKey: visitMutationKeys.memoUpdate(visitId, item.visitItemId),
    retry: false,
    mutationFn: ({ memo, expectedMemoVersion }: { memo: string; expectedMemoVersion: number }) =>
      updateVisitItemMemo(config, visitId, item.visitItemId, { memo, expectedMemoVersion }),
  });

  const notify = useCallback(() => {
    if (!mountedRef.current) return;
    forceRender();
    onActivityChange();
  }, [onActivityChange]);

  const announceIfMounted = (message: string) => {
    if (mountedRef.current) announce(message);
  };

  const clearMemoTimer = useCallback(() => {
    if (runtime.memo.timer === null) return;
    clearTimeout(runtime.memo.timer);
    runtime.memo.timer = null;
  }, [runtime]);

  const synchronizeStatusFromCache = () => {
    const cached = queryClient.getQueryData<VisitDetail>(visitQueryKeys.detail(visitId));
    if (cached === undefined) return;
    const found = findVisitItem(cached, item.visitItemId);
    if (found !== null && found.item.statusVersion > runtime.status.version) {
      runtime.status.confirmed = found.item.status;
      runtime.status.version = found.item.statusVersion;
      runtime.status.savedAt = found.item.statusSavedAt;
    }
  };

  const synchronizeMemoFromCache = () => {
    const cached = queryClient.getQueryData<VisitDetail>(visitQueryKeys.detail(visitId));
    if (cached === undefined) return;
    const found = findVisitItem(cached, item.visitItemId);
    if (found !== null && found.item.memoVersion > runtime.memo.version) {
      runtime.memo.confirmed = found.item.inlineMemo;
      runtime.memo.version = found.item.memoVersion;
      runtime.memo.savedAt = found.item.memoSavedAt;
    }
  };

  const refreshStatusChannel = async (): Promise<boolean> => {
    const latest = await fetchVisitDetail(config, visitId);
    const found = findVisitItem(latest, item.visitItemId);
    if (found === null) return false;
    runtime.status.confirmed = found.item.status;
    runtime.status.version = found.item.statusVersion;
    runtime.status.savedAt = found.item.statusSavedAt;
    queryClient.setQueryData<VisitDetail>(visitQueryKeys.detail(visitId), (current) =>
      applyVisitItemStatusUpdate(current, {
        item: {
          visitItemId: found.item.visitItemId,
          status: found.item.status,
          statusVersion: found.item.statusVersion,
          statusSavedAt: found.item.statusSavedAt,
        },
        stageSummary: found.stageSummary,
        visitSummary: latest.summary,
      }),
    );
    return true;
  };

  const refreshMemoChannel = async (): Promise<boolean> => {
    const latest = await fetchVisitDetail(config, visitId);
    const found = findVisitItem(latest, item.visitItemId);
    if (found === null) return false;
    runtime.memo.confirmed = found.item.inlineMemo;
    runtime.memo.version = found.item.memoVersion;
    runtime.memo.savedAt = found.item.memoSavedAt;
    queryClient.setQueryData<VisitDetail>(visitQueryKeys.detail(visitId), (current) =>
      found.item.memoSavedAt === null
        ? current
        : applyVisitItemMemoUpdate(current, {
            visitItemId: found.item.visitItemId,
            memo: found.item.inlineMemo,
            memoVersion: found.item.memoVersion,
            memoSavedAt: found.item.memoSavedAt,
          }),
    );
    return true;
  };

  const executeStatusQueue = async (): Promise<boolean> => {
    while (runtime.status.desired !== null) {
      const target = runtime.status.desired;
      const targetIntentId = runtime.status.intentId;
      let conflictRetries = 0;
      runtime.status.phase = 'saving';
      runtime.status.error = null;
      notify();

      while (true) {
        try {
          synchronizeStatusFromCache();
          const result = await statusMutation.mutateAsync({
            status: target,
            expectedStatusVersion: runtime.status.version,
          });
          if (result.item.statusVersion >= runtime.status.version) {
            runtime.status.confirmed = result.item.status;
            runtime.status.version = result.item.statusVersion;
            runtime.status.savedAt = result.item.statusSavedAt;
          }
          queryClient.setQueryData<VisitDetail>(visitQueryKeys.detail(visitId), (current) =>
            applyVisitItemStatusUpdate(current, result),
          );
          if (runtime.status.intentId === targetIntentId) runtime.status.desired = null;
          runtime.status.phase = runtime.status.desired === null ? 'saved' : 'saving';
          runtime.status.error = null;
          announceIfMounted(`${item.question} 상태를 저장했어요.`);
          void invalidateVisitAggregates(queryClient, propertyId);
          notify();
          break;
        } catch (error) {
          if (isVisitStatusVersionConflict(error) && conflictRetries < 1) {
            conflictRetries += 1;
            runtime.status.phase = 'refreshing';
            announceIfMounted(`${item.question} 상태의 최신 버전을 확인하고 있어요.`);
            notify();
            try {
              if (await refreshStatusChannel()) continue;
            } catch {
              // The original conflict remains retryable below.
            }
          }
          runtime.status.phase = 'error';
          runtime.status.error = error;
          announceIfMounted(`${item.question} 상태를 저장하지 못했어요.`);
          notify();
          return false;
        }
      }
    }
    return true;
  };

  const runStatusQueue = (): Promise<boolean> => {
    if (runtime.status.promise !== null) return runtime.status.promise;
    const promise = executeStatusQueue().finally(() => {
      if (runtime.status.promise === promise) runtime.status.promise = null;
      notify();
    });
    runtime.status.promise = promise;
    notify();
    return promise;
  };

  const executeMemoQueue = async (): Promise<boolean> => {
    while (runtime.memo.dirty) {
      if (!runtime.memo.debounceReady && !runtime.memo.forceFlush) return true;
      const target = runtime.memo.draft;
      const targetIntentId = runtime.memo.intentId;
      let conflictRetries = 0;
      runtime.memo.debounceReady = false;
      runtime.memo.phase = 'saving';
      runtime.memo.error = null;
      notify();

      while (true) {
        try {
          synchronizeMemoFromCache();
          const result = await memoMutation.mutateAsync({ memo: target, expectedMemoVersion: runtime.memo.version });
          if (result.memoVersion >= runtime.memo.version) {
            runtime.memo.confirmed = result.memo;
            runtime.memo.version = result.memoVersion;
            runtime.memo.savedAt = result.memoSavedAt;
          }
          queryClient.setQueryData<VisitDetail>(visitQueryKeys.detail(visitId), (current) =>
            applyVisitItemMemoUpdate(current, result),
          );
          runtime.memo.dirty = runtime.memo.draft !== runtime.memo.confirmed;
          if (runtime.memo.intentId === targetIntentId) runtime.memo.dirty = false;
          runtime.memo.phase = runtime.memo.dirty ? 'idle' : 'saved';
          runtime.memo.error = null;
          announceIfMounted(`${item.question} 메모를 저장했어요.`);
          void invalidateVisitAggregates(queryClient, propertyId);
          notify();
          break;
        } catch (error) {
          if (isVisitMemoVersionConflict(error) && conflictRetries < 1) {
            conflictRetries += 1;
            runtime.memo.phase = 'refreshing';
            announceIfMounted(`${item.question} 메모의 최신 버전을 확인하고 있어요.`);
            notify();
            try {
              if (await refreshMemoChannel()) continue;
            } catch {
              // The local draft remains retryable below.
            }
          }
          runtime.memo.phase = 'error';
          runtime.memo.error = error;
          runtime.memo.dirty = true;
          announceIfMounted(`${item.question} 메모를 저장하지 못했어요. 작성한 내용은 유지됩니다.`);
          notify();
          return false;
        }
      }
    }
    return true;
  };

  const runMemoQueue = (): Promise<boolean> => {
    if (runtime.memo.promise !== null) return runtime.memo.promise;
    const promise = executeMemoQueue().finally(() => {
      if (runtime.memo.promise === promise) runtime.memo.promise = null;
      notify();
    });
    runtime.memo.promise = promise;
    notify();
    return promise;
  };

  const scheduleMemo = () => {
    clearMemoTimer();
    if (!runtime.memo.dirty) return;
    if (runtime.memo.forceFlush) {
      runtime.memo.debounceReady = true;
      void runMemoQueue();
      return;
    }
    runtime.memo.timer = setTimeout(() => {
      runtime.memo.timer = null;
      runtime.memo.debounceReady = true;
      notify();
      void runMemoQueue();
    }, 1000);
  };

  const actionsRef = useRef({
    selectStatus: (_status: VisitItemStatus) => undefined,
    changeMemo: (_value: string) => undefined,
    flushMemo: async () => true,
    retryStatus: () => undefined,
    retryMemo: () => undefined,
    flush: async () => true,
  });

  actionsRef.current = {
    selectStatus: (status) => {
      runtime.status.desired = status;
      runtime.status.intentId += 1;
      runtime.status.phase = 'saving';
      runtime.status.error = null;
      notify();
      void runStatusQueue();
    },
    changeMemo: (value) => {
      const sanitized = removeInlineMemoLineBreaks(value);
      if (!isInlineMemoWithinLimit(sanitized)) {
        runtime.memo.limitReached = true;
        notify();
        return;
      }
      runtime.memo.limitReached = inlineMemoCodePointLength(sanitized) === 200;
      runtime.memo.draft = sanitized;
      runtime.memo.intentId += 1;
      runtime.memo.dirty = sanitized !== runtime.memo.confirmed;
      runtime.memo.error = null;
      runtime.memo.phase = runtime.memo.dirty ? 'idle' : 'saved';
      runtime.memo.debounceReady = runtime.memo.forceFlush;
      if (!runtime.memo.dirty) clearMemoTimer();
      else scheduleMemo();
      notify();
    },
    flushMemo: async () => {
      clearMemoTimer();
      if (runtime.memo.phase === 'error') return false;
      runtime.memo.debounceReady = true;
      notify();
      return runMemoQueue();
    },
    retryStatus: () => {
      if (runtime.status.desired === null) runtime.status.desired = runtime.status.confirmed;
      runtime.status.phase = 'saving';
      runtime.status.error = null;
      notify();
      void runStatusQueue();
    },
    retryMemo: () => {
      runtime.memo.dirty = true;
      runtime.memo.debounceReady = true;
      runtime.memo.phase = 'saving';
      runtime.memo.error = null;
      notify();
      void runMemoQueue();
    },
    flush: async () => {
      clearMemoTimer();
      runtime.memo.forceFlush = true;
      runtime.memo.debounceReady = true;
      notify();
      try {
        const saveStatus = runtime.status.phase === 'error' ? Promise.resolve(false) : runStatusQueue();
        const saveMemo = runtime.memo.phase === 'error' ? Promise.resolve(false) : runMemoQueue();
        const [statusSaved, memoSaved] = await Promise.all([saveStatus, saveMemo]);
        if (!statusSaved || !memoSaved) return false;
        if (runtime.status.desired !== null || runtime.memo.dirty) {
          const [latestStatusSaved, latestMemoSaved] = await Promise.all([runStatusQueue(), runMemoQueue()]);
          return latestStatusSaved && latestMemoSaved;
        }
        return true;
      } finally {
        runtime.memo.forceFlush = false;
        notify();
      }
    },
  };

  const handleRef = useRef<VisitItemAutosaveHandle | null>(null);
  if (handleRef.current === null) {
    handleRef.current = {
      hasPending: () =>
        runtime.status.desired !== null ||
        runtime.status.promise !== null ||
        runtime.memo.dirty ||
        runtime.memo.promise !== null ||
        runtime.memo.timer !== null,
      hasError: () => runtime.status.phase === 'error' || runtime.memo.phase === 'error',
      flush: () => actionsRef.current.flush(),
    };
  }

  useEffect(
    () => register(item.visitItemId, handleRef.current as VisitItemAutosaveHandle),
    [item.visitItemId, register],
  );

  useEffect(() => {
    if (item.statusVersion >= runtime.status.version) {
      runtime.status.confirmed = item.status;
      runtime.status.version = item.statusVersion;
      runtime.status.savedAt = item.statusSavedAt;
    }
    if (item.memoVersion >= runtime.memo.version) {
      runtime.memo.confirmed = item.inlineMemo;
      runtime.memo.version = item.memoVersion;
      runtime.memo.savedAt = item.memoSavedAt;
      if (!runtime.memo.dirty && runtime.memo.promise === null) runtime.memo.draft = item.inlineMemo;
    }
    notify();
  }, [
    item.inlineMemo,
    item.memoSavedAt,
    item.memoVersion,
    item.status,
    item.statusSavedAt,
    item.statusVersion,
    notify,
    runtime,
  ]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      clearMemoTimer();
    };
  }, [clearMemoTimer]);

  return {
    displayedStatus: runtime.status.desired ?? runtime.status.confirmed,
    statusVersion: runtime.status.version,
    statusSavedAt: runtime.status.savedAt,
    statusPhase: runtime.status.phase,
    statusError: runtime.status.error,
    memoDraft: runtime.memo.draft,
    memoVersion: runtime.memo.version,
    memoSavedAt: runtime.memo.savedAt,
    memoPhase: runtime.memo.phase,
    memoError: runtime.memo.error,
    memoCount: inlineMemoCodePointLength(runtime.memo.draft),
    memoLimitReached: runtime.memo.limitReached,
    isPending: handleRef.current.hasPending(),
    selectStatus: (status: VisitItemStatus) => actionsRef.current.selectStatus(status),
    changeMemo: (value: string) => actionsRef.current.changeMemo(value),
    flushMemo: () => actionsRef.current.flushMemo(),
    retryStatus: () => actionsRef.current.retryStatus(),
    retryMemo: () => actionsRef.current.retryMemo(),
  };
};
