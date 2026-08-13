import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const warning = '저장하지 않은 변경이 있어요. 이 페이지를 나갈까요?';

export const useUnsavedChangesGuard = (isDirty: boolean) => {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isDirty) return;

    const beforeUnload = (event: BeforeUnloadEvent) => event.preventDefault();
    const captureInternalLink = (event: MouseEvent) => {
      if (
        event.defaultPrevented ||
        event.button !== 0 ||
        event.metaKey ||
        event.ctrlKey ||
        event.shiftKey ||
        event.altKey
      ) {
        return;
      }
      const target = event.target;
      const anchor = target instanceof Element ? target.closest('a[href]') : null;
      if (!(anchor instanceof HTMLAnchorElement)) return;
      const url = new URL(anchor.href, window.location.href);
      if (url.origin !== window.location.origin || url.href === window.location.href) return;
      event.preventDefault();
      if (window.confirm(warning)) navigate(`${url.pathname}${url.search}${url.hash}`);
    };

    window.addEventListener('beforeunload', beforeUnload);
    document.addEventListener('click', captureInternalLink, true);
    return () => {
      window.removeEventListener('beforeunload', beforeUnload);
      document.removeEventListener('click', captureInternalLink, true);
    };
  }, [isDirty, navigate]);
};
