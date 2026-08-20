import { useEffect, useState } from 'react';
import {
  usePropertyChecklistItemMemoMutation,
  usePropertyChecklistItemStatusMutation,
} from '../hooks/query/usePropertyChecklistItemMutations';
import type { PropertyChecklistItem, PropertyChecklistItemStatus } from '../types/Property';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './PropertyChecklistItemControl.module.css';

const STATUS_OPTIONS: Array<{ value: PropertyChecklistItemStatus; label: string }> = [
  { value: 'GOOD', label: '괜찮음' },
  { value: 'CAUTION', label: '주의' },
  { value: 'UNCONFIRMED', label: '미확인' },
];

const PropertyChecklistItemControl = ({
  config,
  propertyId,
  propertyChecklistId,
  item,
}: {
  config: PublicConfig;
  propertyId: number;
  propertyChecklistId: number;
  item: PropertyChecklistItem;
}) => {
  const [isMemoOpen, setIsMemoOpen] = useState(item.memo.trim() !== '');
  const [memo, setMemo] = useState(item.memo);
  const statusMutation = usePropertyChecklistItemStatusMutation(config, propertyId, propertyChecklistId);
  const memoMutation = usePropertyChecklistItemMemoMutation(config, propertyId, propertyChecklistId);

  useEffect(() => setMemo(item.memo), [item.memo]);

  const saveMemo = () => {
    if (memo === item.memo || memoMutation.isPending) return;
    memoMutation.mutate({ itemId: item.itemId, memo });
  };

  return (
    <li className={styles.item}>
      <fieldset>
        <legend className="sr-only">{item.question}</legend>
        <div className={styles.content}>
          <strong aria-hidden="true">{item.question}</strong>
          <div className={styles.statuses}>
            {STATUS_OPTIONS.map((option) => (
              <label key={option.value} data-status={option.value}>
                <input
                  type="radio"
                  name={`property-checklist-item-${item.itemId}`}
                  value={option.value}
                  checked={item.status === option.value}
                  disabled={statusMutation.isPending}
                  onChange={() => statusMutation.mutate({ itemId: item.itemId, status: option.value })}
                />
                <span aria-hidden="true" />
                <small>{option.label}</small>
              </label>
            ))}
          </div>
        </div>
      </fieldset>
      <button
        type="button"
        className={styles.memoToggle}
        aria-expanded={isMemoOpen}
        aria-controls={`property-checklist-memo-${item.itemId}`}
        aria-label={`${item.question} 메모 ${isMemoOpen ? '닫기' : '열기'}`}
        onClick={() => setIsMemoOpen((current) => !current)}
      >
        <span aria-hidden="true" />
      </button>
      {isMemoOpen && (
        <div className={styles.memoPanel} id={`property-checklist-memo-${item.itemId}`}>
          <label className="sr-only" htmlFor={`property-checklist-memo-input-${item.itemId}`}>
            {item.question} 메모
          </label>
          <input
            id={`property-checklist-memo-input-${item.itemId}`}
            value={memo}
            maxLength={500}
            placeholder="메모 입력…"
            onChange={(event) => setMemo(event.target.value)}
            onBlur={saveMemo}
          />
        </div>
      )}
      {(statusMutation.isError || memoMutation.isError) && (
        <p className={styles.error} role="alert">
          변경 내용을 저장하지 못했어요. 다시 시도해 주세요.
        </p>
      )}
    </li>
  );
};

export default PropertyChecklistItemControl;
