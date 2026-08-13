import { useEffect, useRef } from 'react';
import { registerNavigationGuard } from '../app/guardedHistory';

export const useAutosaveNavigationGuard = ({
  shouldFlush,
  flush,
  onFlushFailure,
}: {
  shouldFlush: boolean;
  flush: () => Promise<boolean>;
  onFlushFailure: () => void;
}) => {
  const shouldFlushRef = useRef(shouldFlush);
  const flushRef = useRef(flush);
  const onFlushFailureRef = useRef(onFlushFailure);
  shouldFlushRef.current = shouldFlush;
  flushRef.current = flush;
  onFlushFailureRef.current = onFlushFailure;

  useEffect(
    () =>
      registerNavigationGuard({
        shouldFlush: () => shouldFlushRef.current,
        flush: () => flushRef.current(),
        onFlushFailure: () => onFlushFailureRef.current(),
      }),
    [],
  );

  useEffect(() => {
    if (!shouldFlush) return;

    const beforeUnload = (event: BeforeUnloadEvent) => event.preventDefault();

    window.addEventListener('beforeunload', beforeUnload);
    return () => window.removeEventListener('beforeunload', beforeUnload);
  }, [shouldFlush]);
};
