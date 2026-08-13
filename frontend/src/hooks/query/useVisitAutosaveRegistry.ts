import { useCallback, useMemo, useReducer, useRef } from 'react';

export type VisitItemAutosaveHandle = {
  hasPending: () => boolean;
  hasError: () => boolean;
  flush: () => Promise<boolean>;
};

export const useVisitAutosaveRegistry = () => {
  const handlesRef = useRef(new Map<number, VisitItemAutosaveHandle>());
  const [revision, notify] = useReducer((value: number) => value + 1, 0);

  const register = useCallback((visitItemId: number, handle: VisitItemAutosaveHandle) => {
    handlesRef.current.set(visitItemId, handle);
    notify();
    return () => {
      if (handlesRef.current.get(visitItemId) === handle) handlesRef.current.delete(visitItemId);
      notify();
    };
  }, []);

  const flushAll = useCallback(async (): Promise<boolean> => {
    const results = await Promise.all([...handlesRef.current.values()].map((handle) => handle.flush()));
    notify();
    return results.every(Boolean) && [...handlesRef.current.values()].every((handle) => !handle.hasPending());
  }, []);

  const focusFirstError = useCallback(() => {
    const target = document.querySelector<HTMLElement>(
      '[data-autosave-error="true"] button, [data-autosave-error="true"] input',
    );
    target?.focus();
  }, []);

  return useMemo(() => {
    const handles = [...handlesRef.current.values()];
    return {
      register,
      notify,
      flushAll,
      focusFirstError,
      hasPending: handles.some((handle) => handle.hasPending()),
      hasError: handles.some((handle) => handle.hasError()),
    };
  }, [flushAll, focusFirstError, register, revision]);
};
