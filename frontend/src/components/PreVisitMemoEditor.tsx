import { useEffect, useId, useRef, useState } from 'react';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import type { SavePropertyPreVisitMemoRequestDto } from '../apis/dtos/PropertyDto';
import { useSavePropertyPreVisitMemo } from '../hooks/query/usePropertyMutations';
import type { PropertyMemo, PropertyPreVisitMemo } from '../types/Property';
import type { PublicConfig } from '../types/PublicConfig';
import { formatDateTime } from '../utils/propertyFormat';
import { getUnicodeCodePointLength } from '../utils/unicode';
import Icon from './ui/Icon';
import { Button } from './ui/Button';
import TextAreaField from './ui/TextAreaField';

type PreVisitMemoEditorProps = {
  config: PublicConfig;
  propertyId: number;
  initialMemo: PropertyMemo;
};

const legacyMemoFields: ReadonlyArray<{
  key: Exclude<keyof PropertyPreVisitMemo, 'additionalMemo' | 'savedAt'>;
  label: string;
}> = [
  { key: 'viewingSchedule', label: '방 보러 가는 일정' },
  { key: 'moveInAvailability', label: '입주 가능일' },
  { key: 'provisionalDeposit', label: '가계약금' },
  { key: 'roomOptions', label: '방 옵션' },
  { key: 'maintenanceAndUtilities', label: '관리비 및 공과금' },
  { key: 'commuteTime', label: '통학·통근 시간' },
  { key: 'governmentSupport', label: '정부 지원금 가능 종류' },
];

const toFreeformMemo = (memo: PropertyPreVisitMemo): string => {
  const legacyMemo = legacyMemoFields
    .filter(({ key }) => memo[key].trim() !== '')
    .map(({ key, label }) => `${label}: ${memo[key].trim()}`);

  return [memo.additionalMemo.trim(), ...legacyMemo].filter((value) => value !== '').join('\n');
};

const toSaveRequest = (memo: string): SavePropertyPreVisitMemoRequestDto => ({
  viewingSchedule: '',
  moveInAvailability: '',
  provisionalDeposit: '',
  roomOptions: '',
  maintenanceAndUtilities: '',
  commuteTime: '',
  governmentSupport: '',
  additionalMemo: memo,
});

const PreVisitMemoEditor = ({ config, propertyId, initialMemo }: PreVisitMemoEditorProps) => {
  const initialValue = toFreeformMemo(initialMemo);
  const [draft, setDraft] = useState(initialValue);
  const [savedMemo, setSavedMemo] = useState(initialValue);
  const [savedAt, setSavedAt] = useState(initialMemo.savedAt);
  const draftRef = useRef(draft);
  const savedMemoRef = useRef(savedMemo);
  const disclosureRef = useRef<HTMLDetailsElement>(null);
  const summaryRef = useRef<HTMLElement>(null);
  const fieldIdPrefix = useId();
  const saveMutation = useSavePropertyPreVisitMemo(config, propertyId);

  draftRef.current = draft;
  savedMemoRef.current = savedMemo;

  useEffect(() => {
    const nextSavedMemo = toFreeformMemo(initialMemo);
    const hadUnsavedChanges = draftRef.current !== savedMemoRef.current;

    savedMemoRef.current = nextSavedMemo;
    setSavedMemo(nextSavedMemo);
    setSavedAt(initialMemo.savedAt);

    if (!hadUnsavedChanges) {
      draftRef.current = nextSavedMemo;
      setDraft(nextSavedMemo);
    }
  }, [initialMemo]);

  const isDirty = draft !== savedMemo;
  const memoLength = getUnicodeCodePointLength(draft);
  const hasError = memoLength > 5_000;

  const save = async () => {
    if (!isDirty || hasError || saveMutation.isPending) return;

    try {
      const saved = await saveMutation.mutateAsync(toSaveRequest(draft));
      const nextSavedMemo = toFreeformMemo(saved);
      draftRef.current = nextSavedMemo;
      savedMemoRef.current = nextSavedMemo;
      setDraft(nextSavedMemo);
      setSavedMemo(nextSavedMemo);
      setSavedAt(saved.savedAt);
      disclosureRef.current?.removeAttribute('open');
      summaryRef.current?.focus();
    } catch {
      // Mutation state renders a retryable error while preserving the draft.
    }
  };

  return (
    <section className="pre-visit-memo-editor" aria-labelledby={`${fieldIdPrefix}-summary-heading`}>
      <details ref={disclosureRef} className="pre-visit-memo-editor__disclosure">
        <summary ref={summaryRef} className="pre-visit-memo-editor__summary" aria-label="퀵 메모 입력 열기">
          <strong id={`${fieldIdPrefix}-summary-heading`}>퀵 메모</strong>
          <span className="pre-visit-memo-editor__summary-action">
            <span className="sr-only">{savedMemo.trim() === '' ? '메모 작성' : '메모 수정'}</span>
            <Icon name="edit" size={18} />
          </span>
          <small>{savedMemo.trim() === '' ? '아직 작성한 메모가 없어요.' : savedMemo}</small>
        </summary>

        <div className="pre-visit-memo-editor__body">
          <form
            className="pre-visit-memo-editor__form"
            aria-busy={saveMutation.isPending}
            onSubmit={(event) => {
              event.preventDefault();
              void save();
            }}
          >
            <TextAreaField
              id={`${fieldIdPrefix}-memo`}
              fieldClassName="pre-visit-memo-editor__freeform"
              className="pre-visit-memo-editor__textarea"
              label="메모"
              name="additionalMemo"
              rows={7}
              value={draft}
              placeholder="매물에 대해 기억할 내용을 자유롭게 적어 주세요."
              readOnly={saveMutation.isPending}
              helpText={`${memoLength.toLocaleString('ko-KR')} / 5,000`}
              error={hasError ? '5,000자 이하로 입력해 주세요.' : undefined}
              onChange={(event) => {
                saveMutation.reset();
                setDraft(event.target.value);
              }}
            />

            <div className="pre-visit-memo-editor__status" aria-live="polite">
              {hasError && <span className="field-error">글자 수를 확인한 뒤 저장해 주세요.</span>}
              {!hasError && saveMutation.isPending && <span>퀵 메모를 저장하고 있어요…</span>}
              {!hasError && !saveMutation.isPending && isDirty && <span>저장되지 않은 변경사항이 있어요.</span>}
              {!hasError && !saveMutation.isPending && !isDirty && saveMutation.isSuccess && savedAt !== null && (
                <span>저장했어요. 마지막 저장 {formatDateTime(savedAt)}</span>
              )}
              {!hasError && !saveMutation.isPending && !isDirty && !saveMutation.isSuccess && savedAt !== null && (
                <span>마지막 저장 {formatDateTime(savedAt)}</span>
              )}
              {!hasError && !saveMutation.isPending && !isDirty && savedAt === null && (
                <span>{saveMutation.isSuccess ? '빈 메모로 저장했어요.' : '아직 저장한 메모가 없어요.'}</span>
              )}
            </div>

            {saveMutation.isError && (
              <p className="form-error" role="alert">
                {getPropertyErrorMessage(saveMutation.error)} 작성 중인 내용은 유지됐어요.
              </p>
            )}

            <div className="pre-visit-memo-editor__actions">
              <Button
                variant="neutral"
                fullWidth
                type="submit"
                disabled={!isDirty || hasError || saveMutation.isPending}
                isLoading={saveMutation.isPending}
                loadingLabel="메모 저장 중…"
              >
                {saveMutation.isError ? '다시 저장' : '메모 저장'}
              </Button>
            </div>
          </form>
        </div>
      </details>
    </section>
  );
};

export default PreVisitMemoEditor;
