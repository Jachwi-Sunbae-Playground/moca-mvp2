import type { ReactNode } from 'react';

type StatusPanelProps = {
  title: string;
  description: string;
  tone?: 'neutral' | 'error' | 'success';
  isBusy?: boolean;
  action?: ReactNode;
};

const StatusPanel = ({ title, description, tone = 'neutral', isBusy = false, action }: StatusPanelProps) => (
  <main className="status-page">
    <section
      className={`status-panel status-panel--${tone}`}
      aria-live={isBusy ? 'polite' : undefined}
      aria-busy={isBusy || undefined}
    >
      <div className="status-panel__mark" aria-hidden="true">
        {isBusy ? <span className="spinner" /> : tone === 'success' ? '✓' : tone === 'error' ? '!' : '·'}
      </div>
      <h1>{title}</h1>
      <p>{description}</p>
      {action === undefined ? null : <div className="status-panel__action">{action}</div>}
    </section>
  </main>
);

export default StatusPanel;
