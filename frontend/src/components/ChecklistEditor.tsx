import { useEffect, useMemo, useRef, useState } from 'react';
import { checklistStageMeta } from '../constants/checklist';
import { useActiveCheckItems } from '../hooks/query/useChecklists';
import { useUnsavedChangesGuard } from '../hooks/useUnsavedChangesGuard';
import type { ChecklistDetail, ChecklistStage } from '../types/Checklist';
import { checkItemToEditorItem, checklistItemToEditorItem, type ChecklistEditorItem } from '../types/ChecklistEditor';
import type { PublicConfig } from '../types/PublicConfig';
import {
  editorItemsFingerprint,
  moveEditorItem,
  unicodeCodePointLength,
  validateCustomQuestion,
} from '../utils/checklistEditor';
import { validateChecklistName } from '../utils/checklist';
import CheckItemPicker from './CheckItemPicker';

type ChecklistEditorProps = {
  config: PublicConfig;
  stage: ChecklistStage;
  initialName: string;
  initialItems: ChecklistEditorItem[];
  submitLabel: string;
  isSubmitting: boolean;
  serverError?: string;
  onNameChange?: (name: string) => void;
  onDirtyChange?: (isDirty: boolean) => void;
  onSubmit: (input: { name: string; items: ChecklistEditorItem[] }) => Promise<ChecklistDetail>;
};

const ChecklistEditor = ({
  config,
  stage,
  initialName,
  initialItems,
  submitLabel,
  isSubmitting,
  serverError,
  onNameChange,
  onDirtyChange,
  onSubmit,
}: ChecklistEditorProps) => {
  const incomingItemsFingerprint = useMemo(() => editorItemsFingerprint(initialItems), [initialItems]);
  const [baselineName, setBaselineName] = useState(initialName);
  const [baselineItemsFingerprint, setBaselineItemsFingerprint] = useState(incomingItemsFingerprint);
  const [name, setName] = useState(initialName);
  const [items, setItems] = useState(initialItems);
  const [customInput, setCustomInput] = useState('');
  const [hasAttemptedCustomAdd, setHasAttemptedCustomAdd] = useState(false);
  const [hasSubmitted, setHasSubmitted] = useState(false);
  const [announcement, setAnnouncement] = useState('');
  const [pendingFocusKey, setPendingFocusKey] = useState<string | null>(null);
  const nextCustomKey = useRef(0);
  const customInputRef = useRef<HTMLTextAreaElement>(null);
  const itemFocusTargets = useRef(new Map<string, HTMLElement>());
  const submissionInFlight = useRef(false);
  const activeCatalog = useActiveCheckItems(config, stage);
  const activeSourceIds = useMemo(
    () => new Set(activeCatalog.data?.content.map((item) => item.checkItemId) ?? []),
    [activeCatalog.data],
  );
  const currentItemsFingerprint = editorItemsFingerprint(items);
  const isDirty =
    name !== baselineName || currentItemsFingerprint !== baselineItemsFingerprint || customInput.length > 0;
  useUnsavedChangesGuard(isDirty && !isSubmitting);

  useEffect(() => {
    if (isDirty || (initialName === baselineName && incomingItemsFingerprint === baselineItemsFingerprint)) {
      return;
    }
    setName(initialName);
    setItems(initialItems);
    setBaselineName(initialName);
    setBaselineItemsFingerprint(incomingItemsFingerprint);
  }, [baselineItemsFingerprint, baselineName, incomingItemsFingerprint, initialItems, initialName, isDirty]);

  useEffect(() => {
    onDirtyChange?.(isDirty);
    return () => onDirtyChange?.(false);
  }, [isDirty, onDirtyChange]);

  useEffect(() => {
    if (pendingFocusKey === null) return;
    if (pendingFocusKey === 'custom-input') customInputRef.current?.focus();
    else itemFocusTargets.current.get(pendingFocusKey)?.focus();
    setPendingFocusKey(null);
  }, [items, pendingFocusKey]);

  const nameError = validateChecklistName(name);
  const hasInvalidCustom = items.some(
    (item) => item.origin === 'CUSTOM' && validateCustomQuestion(item.question) !== null,
  );
  const itemError =
    items.length === 0
      ? '체크 항목을 한 개 이상 추가해 주세요.'
      : hasInvalidCustom
        ? '직접 추가한 질문의 입력 오류를 확인해 주세요.'
        : null;
  const customInputError = validateCustomQuestion(customInput);
  const customInputValidationMessage =
    customInputError ?? '입력 중인 직접 질문을 목록에 추가하거나 입력란을 비워 주세요.';

  const move = (index: number, direction: -1 | 1) => {
    const item = items[index];
    setItems(moveEditorItem(items, index, direction));
    setPendingFocusKey(item.clientKey);
    setAnnouncement(`${item.question} 항목을 ${direction === -1 ? '위로' : '아래로'} 이동했어요.`);
  };

  const remove = (index: number) => {
    const removed = items[index];
    const nextItems = items.filter((_, candidateIndex) => candidateIndex !== index);
    const nextFocusItem = nextItems[Math.min(index, nextItems.length - 1)];
    setItems(nextItems);
    setPendingFocusKey(nextFocusItem?.clientKey ?? 'custom-input');
    setAnnouncement(`${removed.question} 항목을 제거했어요. 저장하기 전까지 서버에는 반영되지 않습니다.`);
  };

  const addCustomItem = () => {
    setHasAttemptedCustomAdd(true);
    if (customInputError !== null) return;
    const clientKey = `custom:${nextCustomKey.current}`;
    nextCustomKey.current += 1;
    const question = customInput.trim();
    setItems((current) => [
      ...current,
      {
        clientKey,
        origin: 'CUSTOM',
        checklistItemId: null,
        sourceCheckItemId: null,
        question,
        guide: null,
      },
    ]);
    setCustomInput('');
    setHasAttemptedCustomAdd(false);
    setPendingFocusKey(clientKey);
    setAnnouncement(`${question} 직접 추가 항목을 목록 끝에 추가했어요.`);
  };

  return (
    <form
      className="checklist-editor"
      onSubmit={async (event) => {
        event.preventDefault();
        setHasSubmitted(true);
        if (customInput.length > 0) {
          setHasAttemptedCustomAdd(true);
          customInputRef.current?.focus();
        }
        if (nameError !== null || itemError !== null || customInput.length > 0 || submissionInFlight.current) {
          return;
        }
        submissionInFlight.current = true;
        setAnnouncement('체크리스트를 저장하고 있어요.');
        try {
          const saved = await onSubmit({ name: name.trim(), items });
          const savedItems = saved.items.map(checklistItemToEditorItem);
          const savedFingerprint = editorItemsFingerprint(savedItems);
          setName(saved.name);
          setItems(savedItems);
          setBaselineName(saved.name);
          setBaselineItemsFingerprint(savedFingerprint);
          setHasSubmitted(false);
          setAnnouncement('체크리스트를 저장했어요. 서버에서 확인한 최신 내용입니다.');
        } catch {
          setAnnouncement('저장하지 못했어요. 작성한 내용은 그대로 유지됩니다.');
        } finally {
          submissionInFlight.current = false;
        }
      }}
    >
      <section className="editor-section">
        <div className="form-field">
          <label htmlFor="checklist-name">체크리스트 이름</label>
          <input
            id="checklist-name"
            value={name}
            maxLength={50}
            disabled={isSubmitting}
            aria-invalid={hasSubmitted && nameError !== null}
            aria-describedby={hasSubmitted && nameError !== null ? 'checklist-name-error' : 'checklist-name-help'}
            onChange={(event) => {
              setName(event.target.value);
              onNameChange?.(event.target.value);
            }}
          />
          <p id="checklist-name-help" className="field-help">
            같은 단계에서 같은 이름을 여러 번 사용할 수 있어요. {name.length}/50
          </p>
          {hasSubmitted && nameError !== null && (
            <p id="checklist-name-error" className="field-error">
              {nameError}
            </p>
          )}
        </div>
        <dl className="fixed-stage">
          <dt>확인 단계</dt>
          <dd>{checklistStageMeta[stage].label}</dd>
        </dl>
      </section>

      <section className="editor-section" aria-labelledby="selected-items-heading">
        <div className="section-heading-row">
          <div>
            <p className="section-eyebrow">선택한 항목</p>
            <h2 id="selected-items-heading">확인 순서</h2>
          </div>
          <span className="selection-count">{items.length}개</span>
        </div>
        <p className="field-help">제공 항목과 직접 추가한 질문을 섞어 원하는 확인 순서로 저장할 수 있어요.</p>
        {items.length === 0 ? (
          <p className="compact-state">아래에서 제공 항목이나 직접 만든 질문을 한 개 이상 추가해 주세요.</p>
        ) : (
          <ol className="selected-check-items">
            {items.map((item, index) => {
              const customError = item.origin === 'CUSTOM' ? validateCustomQuestion(item.question) : null;
              const isInactiveProvided =
                item.origin === 'PROVIDED' && activeCatalog.isSuccess && !activeSourceIds.has(item.sourceCheckItemId);
              return (
                <li key={item.clientKey}>
                  <span className="selected-check-items__number" aria-hidden="true">
                    {index + 1}
                  </span>
                  <div className="selected-check-items__copy">
                    <span className={`item-origin item-origin--${item.origin.toLowerCase()}`}>
                      {item.origin === 'PROVIDED' ? '제공 항목' : '직접 추가'}
                    </span>
                    {item.origin === 'PROVIDED' ? (
                      <>
                        <strong
                          ref={(element) => {
                            if (element === null) itemFocusTargets.current.delete(item.clientKey);
                            else itemFocusTargets.current.set(item.clientKey, element);
                          }}
                          tabIndex={-1}
                        >
                          {item.question}
                        </strong>
                        <small>{item.guide}</small>
                        {isInactiveProvided && (
                          <small className="inactive-item-note">
                            더 이상 제공되지 않음 · 유지, 이동 또는 제거 가능
                          </small>
                        )}
                      </>
                    ) : (
                      <div className="custom-item-field">
                        <label htmlFor={`custom-item-${item.clientKey}`}>직접 추가 질문 {index + 1}</label>
                        <textarea
                          id={`custom-item-${item.clientKey}`}
                          ref={(element) => {
                            if (element === null) itemFocusTargets.current.delete(item.clientKey);
                            else itemFocusTargets.current.set(item.clientKey, element);
                          }}
                          value={item.question}
                          rows={3}
                          disabled={isSubmitting}
                          aria-invalid={customError !== null}
                          aria-describedby={`custom-item-${item.clientKey}-help${customError === null ? '' : ` custom-item-${item.clientKey}-error`}`}
                          onChange={(event) => {
                            const question = event.target.value;
                            setItems((current) =>
                              current.map((candidate) =>
                                candidate.clientKey === item.clientKey && candidate.origin === 'CUSTOM'
                                  ? { ...candidate, question }
                                  : candidate,
                              ),
                            );
                          }}
                        />
                        <small id={`custom-item-${item.clientKey}-help`}>
                          이 체크리스트에만 저장됩니다. {unicodeCodePointLength(item.question.trim())}/200
                        </small>
                        {customError !== null && (
                          <small id={`custom-item-${item.clientKey}-error`} className="field-error">
                            {customError}
                          </small>
                        )}
                      </div>
                    )}
                  </div>
                  <span className="selected-check-items__actions">
                    <button
                      type="button"
                      disabled={isSubmitting || index === 0}
                      aria-label={`${item.question || '빈 직접 추가 질문'} 위로 이동`}
                      onClick={() => move(index, -1)}
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      disabled={isSubmitting || index === items.length - 1}
                      aria-label={`${item.question || '빈 직접 추가 질문'} 아래로 이동`}
                      onClick={() => move(index, 1)}
                    >
                      ↓
                    </button>
                    <button
                      type="button"
                      className="remove-item-button"
                      disabled={isSubmitting}
                      aria-label={`${item.question || '빈 직접 추가 질문'} 제거`}
                      onClick={() => remove(index)}
                    >
                      제거
                    </button>
                  </span>
                </li>
              );
            })}
          </ol>
        )}
        {hasSubmitted && itemError !== null && (
          <p className="form-error" role="alert">
            {itemError}
          </p>
        )}
      </section>

      <section className="editor-section" aria-labelledby="custom-item-heading">
        <p className="section-eyebrow">직접 추가</p>
        <h2 id="custom-item-heading">나만의 질문 추가</h2>
        <div className="form-field custom-question-adder">
          <label htmlFor="new-custom-question">질문</label>
          <textarea
            id="new-custom-question"
            ref={customInputRef}
            value={customInput}
            rows={3}
            disabled={isSubmitting}
            aria-invalid={hasAttemptedCustomAdd}
            aria-describedby={
              hasAttemptedCustomAdd ? 'new-custom-question-help new-custom-question-error' : 'new-custom-question-help'
            }
            onChange={(event) => setCustomInput(event.target.value)}
          />
          <p id="new-custom-question-help" className="field-help">
            이 체크리스트에만 저장되며 같은 질문도 여러 번 추가할 수 있어요.{' '}
            {unicodeCodePointLength(customInput.trim())}/200
          </p>
          {hasAttemptedCustomAdd && (
            <p id="new-custom-question-error" className="field-error">
              {customInputValidationMessage}
            </p>
          )}
          <button type="button" className="secondary-button" disabled={isSubmitting} onClick={addCustomItem}>
            직접 질문 추가
          </button>
        </div>
      </section>

      <CheckItemPicker
        config={config}
        stage={stage}
        existingSourceIds={items.flatMap((item) => (item.origin === 'PROVIDED' ? [item.sourceCheckItemId] : []))}
        disabled={isSubmitting}
        onAdd={(newItems) => {
          const existingIds = new Set(
            items.flatMap((item) => (item.origin === 'PROVIDED' ? [item.sourceCheckItemId] : [])),
          );
          const additions = newItems.filter((item) => !existingIds.has(item.checkItemId)).map(checkItemToEditorItem);
          if (additions.length === 0) return;
          setItems((current) => [...current, ...additions]);
          setPendingFocusKey(additions[0].clientKey);
          setAnnouncement(`${additions.length}개 제공 항목을 목록 끝에 추가했어요.`);
        }}
      />

      <p className="editor-save-status" role="status" aria-live="polite">
        {announcement}
      </p>
      {serverError !== undefined && (
        <p className="form-error" role="alert">
          {serverError} 작성한 내용은 그대로 유지됩니다. 같은 버튼으로 다시 시도할 수 있어요.
        </p>
      )}
      <div className="sticky-form-action">
        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? '저장 중…' : submitLabel}
        </button>
      </div>
    </form>
  );
};

export default ChecklistEditor;
