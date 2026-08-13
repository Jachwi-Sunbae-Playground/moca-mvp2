import { Component, Suspense } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

class RouteErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(_error: Error, _info: ErrorInfo) {
    // The retry is deliberately user-triggered so an unavailable chunk does not create a reload loop.
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="property-page">
          <div className="page-container">
            <div className="content-state content-state--error" role="alert">
              <strong>화면을 불러오지 못했어요.</strong>
              <span>새 배포로 화면 파일이 바뀌었을 수 있어요.</span>
              <button className="inline-button" type="button" onClick={() => window.location.reload()}>
                새로고침해 다시 시도
              </button>
            </div>
          </div>
        </main>
      );
    }

    return this.props.children;
  }
}

const LazyRouteBoundary = ({ children }: { children: ReactNode }) => (
  <RouteErrorBoundary>
    <Suspense
      fallback={
        <main className="property-page">
          <div className="page-container">
            <div className="content-state" role="status">
              <span className="spinner" />
              화면을 불러오는 중이에요.
            </div>
          </div>
        </main>
      }
    >
      {children}
    </Suspense>
  </RouteErrorBoundary>
);

export default LazyRouteBoundary;
