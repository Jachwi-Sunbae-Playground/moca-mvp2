import { useMemo, useState } from 'react';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import { useCheckItemSearch } from '../hooks/query/useChecklists';
import type { CheckItem, ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import BottomActionArea from './ui/BottomActionArea';
import { Button } from './ui/Button';
import SearchField from './ui/SearchField';
import styles from './ChecklistEditor.module.css';

type CheckItemPickerProps = {
  config: PublicConfig;
  stage: ChecklistStage;
  existingSourceIds: number[];
  disabled: boolean;
  onCancel: () => void;
  onAdd: (items: CheckItem[]) => void;
};

const CheckItemPicker = ({ config, stage, existingSourceIds, disabled, onCancel, onAdd }: CheckItemPickerProps) => {
  const [input, setInput] = useState('');
  const [query, setQuery] = useState('');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(() => new Set());
  const result = useCheckItemSearch(config, stage, query);
  const items = useMemo(() => result.data?.pages.flatMap((page) => page.content) ?? [], [result.data]);
  const existingIds = useMemo(() => new Set(existingSourceIds), [existingSourceIds]);

  const search = (nextQuery: string) => {
    setQuery(nextQuery.trim());
    setSelectedIds(new Set());
  };

  const addSelected = () => {
    const selected = items.filter((item) => selectedIds.has(item.checkItemId) && !existingIds.has(item.checkItemId));
    if (selected.length === 0) return;
    onAdd(selected);
    setSelectedIds(new Set());
  };

  return (
    <section className={styles['item-picker']} aria-labelledby="item-picker-heading">
      <div className={styles['section-heading-row']}>
        <div>
          <h2 id="item-picker-heading">체크 항목 검색</h2>
        </div>
        <span className={styles['selection-count']} aria-live="polite">
          {selectedIds.size}개 선택
        </span>
      </div>
      <div className={styles['check-item-search']}>
        <SearchField
          label="제공 항목 검색"
          value={input}
          placeholder="예: 채광, 관리비, 소음"
          disabled={disabled}
          showSubmitButton={false}
          onValueChange={setInput}
          onSubmit={() => search(input)}
          onClear={() => {
            setInput('');
            setQuery('');
            setSelectedIds(new Set());
          }}
        />
      </div>

      {result.isPending ? (
        <div className={styles['compact-state']} role="status">
          <span className="spinner" /> 항목을 불러오는 중이에요.
        </div>
      ) : result.isError ? (
        <div className={styles['item-picker__error']} role="alert">
          <span>{getChecklistErrorMessage(result.error)}</span>
          <Button
            className={styles['item-picker__retry']}
            type="button"
            variant="text"
            disabled={disabled}
            onClick={() => void result.refetch()}
          >
            다시 시도
          </Button>
        </div>
      ) : items.length === 0 ? (
        <p className={styles['compact-state']}>검색 결과가 없어요.</p>
      ) : (
        <div className={styles['check-item-results']}>
          <h3>검색 결과</h3>
          <ul className={styles['check-item-search-results']}>
            {items.map((item) => {
              const exists = existingIds.has(item.checkItemId);
              const checked = exists || selectedIds.has(item.checkItemId);
              return (
                <li key={item.checkItemId}>
                  <label>
                    <input
                      className="sr-only"
                      type="checkbox"
                      checked={checked}
                      disabled={disabled || exists}
                      onChange={(event) => {
                        const next = new Set(selectedIds);
                        if (event.target.checked) next.add(item.checkItemId);
                        else next.delete(item.checkItemId);
                        setSelectedIds(next);
                      }}
                    />
                    <span className={styles['check-item-search-results__control']} data-selected={checked || undefined}>
                      {checked && <span aria-hidden="true">✓</span>}
                    </span>
                    <span className={styles['check-item-search-results__copy']}>
                      <strong>{item.question}</strong>
                      {item.guide !== null && <small>{item.guide}</small>}
                    </span>
                  </label>
                  {exists && <small className={styles['already-added']}>이미 추가됨</small>}
                </li>
              );
            })}
          </ul>
        </div>
      )}

      {result.hasNextPage && (
        <Button
          variant="secondary"
          fullWidth
          className={styles['compact-button']}
          type="button"
          disabled={disabled || result.isFetchingNextPage}
          onClick={() => void result.fetchNextPage()}
        >
          {result.isFetchingNextPage ? '불러오는 중…' : '항목 더 보기'}
        </Button>
      )}
      <div className={styles['item-picker__bottom-actions']}>
        <BottomActionArea sticky={false} divider={false}>
          <Button variant="secondary" type="button" disabled={disabled} onClick={onCancel}>
            취소
          </Button>
          <Button
            fullWidth
            className={styles['item-picker__add']}
            type="button"
            disabled={disabled || selectedIds.size === 0}
            onClick={addSelected}
          >
            선택한 {selectedIds.size}개 항목 추가
          </Button>
        </BottomActionArea>
      </div>
    </section>
  );
};

export default CheckItemPicker;
