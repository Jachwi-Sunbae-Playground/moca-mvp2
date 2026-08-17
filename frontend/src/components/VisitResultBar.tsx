import type { VisitSummary } from '../types/Visit';
import styles from './VisitResultBar.module.css';

type VisitResultBarProps = {
  summary: VisitSummary;
};

const VisitResultBar = ({ summary }: VisitResultBarProps) => {
  const results = [
    {
      label: '괜찮음',
      count: summary.goodCount,
      segmentClassName: styles.goodSegment,
      itemClassName: styles.goodResult,
    },
    {
      label: '주의',
      count: summary.cautionCount,
      segmentClassName: styles.cautionSegment,
      itemClassName: styles.cautionResult,
    },
    {
      label: '미확인',
      count: summary.unconfirmedCount,
      segmentClassName: styles.unconfirmedSegment,
      itemClassName: styles.unconfirmedResult,
    },
  ];

  return (
    <div className={styles.results}>
      <div className={styles.bar} aria-hidden="true">
        {results
          .filter((result) => result.count > 0)
          .map((result) => (
            <span key={result.label} className={result.segmentClassName} style={{ flexGrow: result.count }} />
          ))}
      </div>
      <ul aria-label="최근 방문 결과 집계">
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

export default VisitResultBar;
