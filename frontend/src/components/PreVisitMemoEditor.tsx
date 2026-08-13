import { useEffect, useId, useRef, useState } from 'react';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import type { SavePropertyPreVisitMemoRequestDto } from '../apis/dtos/PropertyDto';
import { useSavePropertyPreVisitMemo } from '../hooks/query/usePropertyMutations';
import type { PropertyMemo, PropertyPreVisitMemo } from '../types/Property';
import type { PublicConfig } from '../types/PublicConfig';
import { formatDateTime } from '../utils/propertyFormat';
import { getUnicodeCodePointLength } from '../utils/unicode';

type PreVisitMemoEditorProps = {
  config: PublicConfig;
  propertyId: number;
  initialMemo: PropertyMemo;
};

type MemoDraft = SavePropertyPreVisitMemoRequestDto;
type StructuredMemoField = Exclude<keyof MemoDraft, 'additionalMemo' | 'content'>;

const STRUCTURED_MEMO_FIELDS: ReadonlyArray<{
  key: StructuredMemoField;
  label: string;
  help: string;
}> = [
  { key: 'viewingSchedule', label: '방 보러 가는 일정', help: '방문 날짜와 시간을 자유롭게 적어 주세요.' },
  { key: 'moveInAvailability', label: '입주 가능일', help: '입주할 수 있는 시기나 확인할 내용을 적어 주세요.' },
  { key: 'provisionalDeposit', label: '가계약금', help: '금액과 반환 조건 등 확인할 내용을 적어 주세요.' },
  { key: 'roomOptions', label: '방 옵션', help: '가전·가구 포함 여부와 상태를 적어 주세요.' },
  {
    key: 'maintenanceAndUtilities',
    label: '관리비 및 공과금',
    help: '관리비 항목과 별도 공과금 등 확인할 내용을 적어 주세요.',
  },
  { key: 'commuteTime', label: '통학·통근 시간', help: '이동 수단과 예상 소요 시간을 적어 주세요.' },
  {
    key: 'governmentSupport',
    label: '정부 지원금 가능 종류',
    help: '가능 여부를 확인할 대출·보증·지원 제도를 적어 주세요.',
  },
];

const toMemoDraft = (memo: PropertyPreVisitMemo): MemoDraft => ({
  viewingSchedule: memo.viewingSchedule,
  moveInAvailability: memo.moveInAvailability,
  provisionalDeposit: memo.provisionalDeposit,
  roomOptions: memo.roomOptions,
  maintenanceAndUtilities: memo.maintenanceAndUtilities,
  commuteTime: memo.commuteTime,
  governmentSupport: memo.governmentSupport,
  additionalMemo: memo.additionalMemo,
});

const areMemoDraftsEqual = (left: MemoDraft, right: MemoDraft): boolean =>
  STRUCTURED_MEMO_FIELDS.every(({ key }) => left[key] === right[key]) && left.additionalMemo === right.additionalMemo;

const PreVisitMemoEditor = ({ config, propertyId, initialMemo }: PreVisitMemoEditorProps) => {
  const initialDraft = toMemoDraft(initialMemo);
  const [draft, setDraft] = useState<MemoDraft>(initialDraft);
  const [savedDraft, setSavedDraft] = useState<MemoDraft>(initialDraft);
  const [savedAt, setSavedAt] = useState(initialMemo.savedAt);
  const draftRef = useRef(draft);
  const savedDraftRef = useRef(savedDraft);
  const fieldIdPrefix = useId();
  const saveMutation = useSavePropertyPreVisitMemo(config, propertyId);

  draftRef.current = draft;
  savedDraftRef.current = savedDraft;

  useEffect(() => {
    const nextSavedDraft = toMemoDraft(initialMemo);
    const hadUnsavedChanges = !areMemoDraftsEqual(draftRef.current, savedDraftRef.current);

    savedDraftRef.current = nextSavedDraft;
    setSavedDraft(nextSavedDraft);
    setSavedAt(initialMemo.savedAt);

    if (!hadUnsavedChanges) {
      draftRef.current = nextSavedDraft;
      setDraft(nextSavedDraft);
    }
  }, [initialMemo]);

  const isDirty = !areMemoDraftsEqual(draft, savedDraft);
  const memoFieldKeys: ReadonlyArray<Exclude<keyof MemoDraft, 'content'>> = [
    ...STRUCTURED_MEMO_FIELDS.map(({ key }) => key),
    'additionalMemo',
  ];
  const lengths = Object.fromEntries(
    memoFieldKeys.map((key) => [key, getUnicodeCodePointLength(draft[key])]),
  ) as Record<Exclude<keyof MemoDraft, 'content'>, number>;
  const hasInvalidField =
    STRUCTURED_MEMO_FIELDS.some(({ key }) => lengths[key] > 200) || lengths.additionalMemo > 5_000;

  const updateDraft = (key: Exclude<keyof MemoDraft, 'content'>, value: string) => {
    saveMutation.reset();
    setDraft((current) => ({ ...current, [key]: value }));
  };

  const save = async () => {
    if (!isDirty || hasInvalidField || saveMutation.isPending) return;

    try {
      const savedMemo = await saveMutation.mutateAsync(draft);
      const nextSavedDraft = toMemoDraft(savedMemo);
      draftRef.current = nextSavedDraft;
      savedDraftRef.current = nextSavedDraft;
      setDraft(nextSavedDraft);
      setSavedDraft(nextSavedDraft);
      setSavedAt(savedMemo.savedAt);
    } catch {
      // Mutation state renders a safe retryable error while preserving the complete draft.
    }
  };

  return (
    <section className="detail-section pre-visit-memo-editor" aria-labelledby={`${fieldIdPrefix}-heading`}>
      <p className="section-eyebrow">방문 전 사전 메모</p>
      <h2 id={`${fieldIdPrefix}-heading`}>방을 보러 가기 전에 확인할 내용을 정리해요</h2>
      <p className="pre-visit-memo-editor__intro" id={`${fieldIdPrefix}-intro`}>
        모든 항목은 선택 사항이에요. 아는 내용만 적고 아래 버튼으로 한 번에 저장할 수 있어요.
      </p>

      <form
        className="pre-visit-memo-editor__form"
        aria-describedby={`${fieldIdPrefix}-intro`}
        aria-busy={saveMutation.isPending}
        onSubmit={(event) => {
          event.preventDefault();
          void save();
        }}
      >
        <div className="pre-visit-memo-editor__fields">
          {STRUCTURED_MEMO_FIELDS.map(({ key, label, help }) => {
            const inputId = `${fieldIdPrefix}-${key}`;
            const helpId = `${inputId}-help`;
            const countId = `${inputId}-count`;
            const errorId = `${inputId}-error`;
            const hasError = lengths[key] > 200;

            return (
              <div className="pre-visit-memo-field" key={key}>
                <label htmlFor={inputId}>{label}</label>
                <textarea
                  id={inputId}
                  name={key}
                  rows={3}
                  value={draft[key]}
                  readOnly={saveMutation.isPending}
                  aria-invalid={hasError || undefined}
                  aria-describedby={`${helpId} ${countId}${hasError ? ` ${errorId}` : ''}`}
                  onChange={(event) => updateDraft(key, event.target.value)}
                />
                <div className="pre-visit-memo-field__meta">
                  <p className="field-help" id={helpId}>
                    {help}
                  </p>
                  <span
                    className={hasError ? 'character-count character-count--error' : 'character-count'}
                    id={countId}
                  >
                    {lengths[key].toLocaleString('ko-KR')} / 200
                  </span>
                </div>
                {hasError && (
                  <p className="field-error" id={errorId}>
                    200자 이하로 입력해 주세요.
                  </p>
                )}
              </div>
            );
          })}

          <div className="pre-visit-memo-field pre-visit-memo-field--additional">
            <label htmlFor={`${fieldIdPrefix}-additionalMemo`}>추가 메모</label>
            <textarea
              id={`${fieldIdPrefix}-additionalMemo`}
              name="additionalMemo"
              rows={6}
              value={draft.additionalMemo}
              readOnly={saveMutation.isPending}
              aria-invalid={lengths.additionalMemo > 5_000 || undefined}
              aria-describedby={`${fieldIdPrefix}-additionalMemo-help ${fieldIdPrefix}-additionalMemo-count${
                lengths.additionalMemo > 5_000 ? ` ${fieldIdPrefix}-additionalMemo-error` : ''
              }`}
              onChange={(event) => updateDraft('additionalMemo', event.target.value)}
            />
            <div className="pre-visit-memo-field__meta">
              <p className="field-help" id={`${fieldIdPrefix}-additionalMemo-help`}>
                위 항목에 담기 어려운 질문이나 직접 확인할 내용을 자유롭게 적어 주세요.
              </p>
              <span
                className={
                  lengths.additionalMemo > 5_000 ? 'character-count character-count--error' : 'character-count'
                }
                id={`${fieldIdPrefix}-additionalMemo-count`}
              >
                {lengths.additionalMemo.toLocaleString('ko-KR')} / 5,000
              </span>
            </div>
            {lengths.additionalMemo > 5_000 && (
              <p className="field-error" id={`${fieldIdPrefix}-additionalMemo-error`}>
                5,000자 이하로 입력해 주세요.
              </p>
            )}
          </div>
        </div>

        <div className="pre-visit-memo-editor__status" aria-live="polite">
          {hasInvalidField && <span className="field-error">글자 수를 확인한 뒤 저장해 주세요.</span>}
          {!hasInvalidField && saveMutation.isPending && <span>방문 전 사전 메모를 저장하고 있어요…</span>}
          {!hasInvalidField && !saveMutation.isPending && isDirty && <span>저장되지 않은 변경사항이 있어요.</span>}
          {!hasInvalidField && !saveMutation.isPending && !isDirty && saveMutation.isSuccess && savedAt !== null && (
            <span>저장했어요. 마지막 저장 {formatDateTime(savedAt)}</span>
          )}
          {!hasInvalidField && !saveMutation.isPending && !isDirty && !saveMutation.isSuccess && savedAt !== null && (
            <span>마지막 저장 {formatDateTime(savedAt)}</span>
          )}
          {!hasInvalidField && !saveMutation.isPending && !isDirty && savedAt === null && (
            <span>{saveMutation.isSuccess ? '빈 메모로 저장했어요.' : '아직 저장한 방문 전 사전 메모가 없어요.'}</span>
          )}
        </div>

        {saveMutation.isError && (
          <p className="form-error" role="alert">
            {getPropertyErrorMessage(saveMutation.error)} 작성 중인 내용은 유지됐어요.
          </p>
        )}

        <div className="pre-visit-memo-editor__actions">
          <button
            className="primary-button"
            type="submit"
            disabled={!isDirty || hasInvalidField || saveMutation.isPending}
          >
            {saveMutation.isPending ? '메모 저장 중…' : saveMutation.isError ? '다시 저장' : '메모 저장'}
          </button>
        </div>
      </form>
    </section>
  );
};

export default PreVisitMemoEditor;
