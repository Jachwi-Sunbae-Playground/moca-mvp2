import type { PropertyChecklistProgress } from '../types/Property';
import styles from './ChecklistProgressBar.module.css';

type ChecklistProgressBarProps = {
  progress: Pick<PropertyChecklistProgress, 'goodCount' | 'cautionCount' | 'unconfirmedCount'>;
  compact?: boolean;
};

const ChecklistProgressBar = ({ progress, compact = false }: ChecklistProgressBarProps) => {
  const results = [
    {
      label: '괜찮음',
      count: progress.goodCount,
      segmentClassName: styles.goodSegment,
      itemClassName: styles.goodResult,
    },
    {
      label: '주의',
      count: progress.cautionCount,
      segmentClassName: styles.cautionSegment,
      itemClassName: styles.cautionResult,
    },
    {
      label: '미확인',
      count: progress.unconfirmedCount,
      segmentClassName: styles.unconfirmedSegment,
      itemClassName: styles.unconfirmedResult,
    },
  ];

  return (
    <div className={styles.results} data-compact={compact || undefined}>
      <div className={styles.bar} aria-hidden="true">
        {results
          .filter((result) => result.count > 0)
          .map((result) => (
            <span key={result.label} className={result.segmentClassName} style={{ flexGrow: result.count }} />
          ))}
      </div>
      <ul aria-label="체크리스트 진행 결과 집계">
        {results.map((result) => (
          <li key={result.label} className={result.itemClassName} data-empty={result.count === 0 || undefined}>
            <span className={styles.resultText}>
              {result.label}{' '}
              <strong className={result.count > 0 ? styles.activeCount : undefined}>{result.count}</strong>
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default ChecklistProgressBar;
