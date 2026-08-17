import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import ChecklistEditor from '../components/ChecklistEditor';
import ChecklistStartOptions from '../components/ChecklistStartOptions';
import type { ChecklistStartMode } from '../components/ChecklistStartOptions';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import { Button } from '../components/ui/Button';
import TopNavigation from '../components/ui/TopNavigation';
import { checklistStageMeta, isChecklistStage } from '../constants/checklist';
import { useCreateChecklist } from '../hooks/query/useChecklistMutations';
import { useChecklistPreset } from '../hooks/query/useChecklists';
import type { ChecklistStage } from '../types/Checklist';
import { checkItemToEditorItem } from '../types/ChecklistEditor';
import type { PublicConfig } from '../types/PublicConfig';
import { parseChecklistReturnTo } from '../utils/checklist';
import { toCreateChecklistItems } from '../utils/checklistEditor';
import styles from './CreateChecklistPage.module.css';

const CreateChecklistPage = ({ config }: { config: PublicConfig }) => {
  const [searchParams] = useSearchParams();
  const stageParam = searchParams.get('stage');
  if (!isChecklistStage(stageParam)) return <InvalidCreateStage />;
  return (
    <ResolvedCreateChecklistPage
      key={stageParam}
      config={config}
      stage={stageParam}
      returnTo={searchParams.get('returnTo')}
      requestedStartMode={parseStartMode(searchParams.get('start'))}
    />
  );
};

const parseStartMode = (value: string | null): ChecklistStartMode | null =>
  value === 'EMPTY' || value === 'ONE_ROOM' ? value : null;

const InvalidCreateStage = () => (
  <main className="property-page">
    <div className="page-container">
      <div className="content-state">
        <strong>체크리스트를 만들 단계를 선택해 주세요.</strong>
        <Link to="/checklists">단계 선택으로 돌아가기</Link>
      </div>
    </div>
  </main>
);

const ResolvedCreateChecklistPage = ({
  config,
  stage,
  returnTo,
  requestedStartMode,
}: {
  config: PublicConfig;
  stage: ChecklistStage;
  returnTo: string | null;
  requestedStartMode: ChecklistStartMode | null;
}) => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const safeReturn = parseChecklistReturnTo(returnTo);
  const [startMode, setStartMode] = useState<ChecklistStartMode | null>(requestedStartMode);
  const [editorInitialName, setEditorInitialName] = useState(() =>
    requestedStartMode === null
      ? ''
      : requestedStartMode === 'ONE_ROOM'
        ? `원룸 ${checklistStageMeta[stage].label} 체크리스트`
        : `${checklistStageMeta[stage].label} 체크리스트`,
  );
  const preset = useChecklistPreset(config, stage, 'ONE_ROOM', startMode === 'ONE_ROOM');
  const create = useCreateChecklist(config);
  const isAddingItems = searchParams.get('mode') === 'add-items';

  const tabTarget = (nextStage: ChecklistStage) => `/checklists/new?stage=${nextStage}`;

  return (
    <main className={`${styles.page} property-page checklist-page checklist-editor-page`}>
      <div className={`${styles.container} page-container page-container--form`}>
        <TopNavigation
          className={styles.topNavigation}
          title={isAddingItems ? '체크 항목 편집' : '새 체크리스트'}
          backLabel={isAddingItems ? '새 체크리스트로 돌아가기' : '새 체크리스트 닫기'}
          navigationIcon="close"
          {...(isAddingItems
            ? {
                onBack: () => {
                  const next = new URLSearchParams(searchParams);
                  next.delete('mode');
                  setSearchParams(next, { replace: true });
                },
              }
            : { backTo: safeReturn?.path ?? `/checklists/${stage}` })}
        />
        <h1 className="sr-only">새 체크리스트</h1>
        <ChecklistStageTabs stage={stage} getTo={tabTarget} fullBleed />

        {startMode === null ? (
          <section className={styles.presetSection} aria-labelledby="preset-heading">
            <h2 id="preset-heading">항목 구성 선택</h2>
            <p>빈 목록이나 원룸 제공 항목으로 시작한 뒤 직접 질문을 섞고 순서를 바꿀 수 있어요.</p>
            <ChecklistStartOptions
              onSelect={(mode) => {
                if (mode === 'EMPTY') {
                  setEditorInitialName(`${checklistStageMeta[stage].label} 체크리스트`);
                } else {
                  setEditorInitialName(`원룸 ${checklistStageMeta[stage].label} 체크리스트`);
                }
                setStartMode(mode);
              }}
            />
          </section>
        ) : startMode === 'ONE_ROOM' && preset.isPending ? (
          <div className={styles.presetStatus} role="status">
            <span className="spinner" />
            프리셋을 불러오는 중이에요.
          </div>
        ) : startMode === 'ONE_ROOM' && preset.isError ? (
          <div className={styles.presetError} role="alert">
            <div>
              <strong>프리셋을 불러오지 못했어요.</strong>
              <span>{getChecklistErrorMessage(preset.error)}</span>
            </div>
            <Button variant="text" type="button" onClick={() => void preset.refetch()}>
              다시 시도
            </Button>
            <Button
              variant="text"
              type="button"
              onClick={() => {
                setEditorInitialName(`${checklistStageMeta[stage].label} 체크리스트`);
                setStartMode('EMPTY');
              }}
            >
              빈 목록으로 시작
            </Button>
          </div>
        ) : (
          <ChecklistEditor
            key={`${stage}-${startMode}`}
            config={config}
            stage={stage}
            initialName={editorInitialName}
            initialItems={startMode === 'ONE_ROOM' ? (preset.data?.items ?? []).map(checkItemToEditorItem) : []}
            submitLabel="체크리스트 만들기"
            isSubmitting={create.isPending}
            actionDivider={false}
            serverError={create.isError ? getChecklistErrorMessage(create.error) : undefined}
            viewMode={isAddingItems ? 'ADD_ITEMS' : 'EDIT'}
            onViewModeChange={(mode) => {
              const next = new URLSearchParams(searchParams);
              if (mode === 'ADD_ITEMS') next.set('mode', 'add-items');
              else next.delete('mode');
              setSearchParams(next, { replace: mode === 'EDIT' });
            }}
            onSubmit={async ({ name, items }) => {
              const created = await create.mutateAsync({
                name,
                stage,
                items: toCreateChecklistItems(items),
              });
              if (safeReturn !== null && safeReturn.stage === stage) {
                navigate(safeReturn.path, { replace: true, state: { newChecklistId: created.checklistId } });
              } else {
                navigate(`/checklists/${created.checklistId}`, { replace: true, state: { focusHeading: true } });
              }
              return created;
            }}
          />
        )}
      </div>
    </main>
  );
};

export default CreateChecklistPage;
