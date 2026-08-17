import { useMemo, useState } from 'react';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import { useCheckItemSearch } from '../hooks/query/useChecklists';
import type { CheckItem, ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import BottomActionArea from './ui/BottomActionArea';
import { Button } from './ui/Button';
import SearchField from './ui/SearchField';

type CheckItemPickerProps = {
  config: PublicConfig;
  stage: ChecklistStage;
  existingSourceIds: number[];
  disabled: boolean;
  onAdd: (items: CheckItem[]) => void;
};

const CheckItemPicker = ({ config, stage, existingSourceIds, disabled, onAdd }: CheckItemPickerProps) => {
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
    <section className="item-picker item-picker--standalone" aria-labelledby="item-picker-heading">
      <div className="section-heading-row">
        <div>
          <h2 id="item-picker-heading">체크 항목 검색</h2>
        </div>
        <span className="selection-count" aria-live="polite">
          {selectedIds.size}개 선택
        </span>
      </div>
      <p className="field-help">현재 단계에서 새로 사용할 수 있는 항목만 검색됩니다.</p>
      <div className="check-item-search">
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
        <div className="compact-state" role="status">
          <span className="spinner" /> 항목을 불러오는 중이에요.
        </div>
      ) : result.isError ? (
        <div className="item-picker__error" role="alert">
          <span>{getChecklistErrorMessage(result.error)}</span>
          <Button
            className="item-picker__retry"
            type="button"
            variant="text"
            disabled={disabled}
            onClick={() => void result.refetch()}
          >
            다시 시도
          </Button>
        </div>
      ) : items.length === 0 ? (
        <p className="compact-state">검색 결과가 없어요. 직접 질문을 추가할 수도 있어요.</p>
      ) : (
        <ul className="check-item-search-results">
          {items.map((item) => {
            const exists = existingIds.has(item.checkItemId);
            const checked = exists || selectedIds.has(item.checkItemId);
            return (
              <li key={item.checkItemId}>
                <label>
                  <input
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
                  <span>
                    <strong>{item.question}</strong>
                    <small>{item.guide}</small>
                  </span>
                </label>
                {exists && <small className="already-added">이미 추가됨</small>}
              </li>
            );
          })}
        </ul>
      )}

      {result.hasNextPage && (
        <Button
          variant="secondary"
          fullWidth
          className="compact-button"
          type="button"
          disabled={disabled || result.isFetchingNextPage}
          onClick={() => void result.fetchNextPage()}
        >
          {result.isFetchingNextPage ? '불러오는 중…' : '항목 더 보기'}
        </Button>
      )}
      <div className="item-picker__bottom-actions">
        <BottomActionArea sticky={false} divider={false}>
          <Button
            fullWidth
            className="item-picker__add"
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
