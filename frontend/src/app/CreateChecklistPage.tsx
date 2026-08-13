import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import ChecklistEditor from '../components/ChecklistEditor';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import PageHeading from '../components/PageHeading';
import { checklistStageMeta, isChecklistStage } from '../constants/checklist';
import { useCreateChecklist } from '../hooks/query/useChecklistMutations';
import { useChecklistPreset } from '../hooks/query/useChecklists';
import type { ChecklistStage } from '../types/Checklist';
import { checkItemToEditorItem } from '../types/ChecklistEditor';
import type { PublicConfig } from '../types/PublicConfig';
import { parseChecklistReturnTo } from '../utils/checklist';
import { toCreateChecklistItems } from '../utils/checklistEditor';

const CreateChecklistPage = ({ config }: { config: PublicConfig }) => {
  const [searchParams] = useSearchParams();
  const stageParam = searchParams.get('stage');
  if (!isChecklistStage(stageParam)) return <InvalidCreateStage />;
  return <ResolvedCreateChecklistPage config={config} stage={stageParam} returnTo={searchParams.get('returnTo')} />;
};

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
}: {
  config: PublicConfig;
  stage: ChecklistStage;
  returnTo: string | null;
}) => {
  const navigate = useNavigate();
  const safeReturn = parseChecklistReturnTo(returnTo);
  const [startMode, setStartMode] = useState<'EMPTY' | 'ONE_ROOM' | null>(null);
  const [draftName, setDraftName] = useState('');
  const [editorInitialName, setEditorInitialName] = useState('');
  const [isEditorDirty, setIsEditorDirty] = useState(false);
  const preset = useChecklistPreset(config, stage, 'ONE_ROOM', startMode === 'ONE_ROOM');
  const create = useCreateChecklist(config);

  const tabTarget = (nextStage: ChecklistStage) => `/checklists/new?stage=${nextStage}`;

  return (
    <main className="property-page checklist-page">
      <div className="page-container page-container--form">
        <PageHeading
          title="새 체크리스트"
          description={`${checklistStageMeta[stage].label} 단계의 확인 목록을 만들어요.`}
          backTo={safeReturn?.path ?? `/checklists/${stage}`}
          backLabel="취소"
        />
        <ChecklistStageTabs stage={stage} getTo={tabTarget} />

        {startMode === null ? (
          <section className="preset-section" aria-labelledby="preset-heading">
            <p className="section-eyebrow">시작 방식</p>
            <h2 id="preset-heading">항목 구성 선택</h2>
            <p>빈 목록이나 원룸 제공 항목으로 시작한 뒤 직접 질문을 섞고 순서를 바꿀 수 있어요.</p>
            <div className="preset-options">
              <button
                type="button"
                onClick={() => {
                  setEditorInitialName(draftName || `${checklistStageMeta[stage].label} 체크리스트`);
                  setStartMode('EMPTY');
                }}
              >
                <strong>빈 목록</strong>
                <span>제공 항목을 검색하거나 나만의 질문만으로 구성해요.</span>
                <small>빈 목록으로 시작 →</small>
              </button>
              <button
                type="button"
                onClick={() => {
                  setEditorInitialName(draftName || `원룸 ${checklistStageMeta[stage].label} 체크리스트`);
                  setStartMode('ONE_ROOM');
                }}
              >
                <strong>원룸 제공 항목</strong>
                <span>원룸을 확인할 때 자주 쓰는 제공 항목을 먼저 불러와요.</span>
                <small>원룸 항목으로 시작 →</small>
              </button>
            </div>
          </section>
        ) : startMode === 'ONE_ROOM' && preset.isPending ? (
          <div className="content-state" role="status">
            <span className="spinner" />
            프리셋을 불러오는 중이에요.
          </div>
        ) : preset.isError ? (
          <div className="content-state content-state--error" role="alert">
            <strong>프리셋을 불러오지 못했어요.</strong>
            <span>{getChecklistErrorMessage(preset.error)}</span>
            <button type="button" className="inline-button" onClick={() => void preset.refetch()}>
              다시 시도
            </button>
            <button type="button" className="inline-button" onClick={() => setStartMode(null)}>
              다른 시작 방식 선택
            </button>
          </div>
        ) : (
          <>
            <button
              type="button"
              className="inline-button preset-change-button"
              onClick={() => {
                if (!isEditorDirty || window.confirm('현재 항목 구성을 초기화하고 다른 프리셋을 선택할까요?')) {
                  setStartMode(null);
                }
              }}
            >
              시작 방식 변경 (항목 초기화)
            </button>
            <ChecklistEditor
              key={`${stage}-${startMode}`}
              config={config}
              stage={stage}
              initialName={editorInitialName}
              initialItems={startMode === 'ONE_ROOM' ? (preset.data?.items ?? []).map(checkItemToEditorItem) : []}
              submitLabel="체크리스트 만들기"
              isSubmitting={create.isPending}
              serverError={create.isError ? getChecklistErrorMessage(create.error) : undefined}
              onNameChange={setDraftName}
              onDirtyChange={setIsEditorDirty}
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
          </>
        )}
      </div>
    </main>
  );
};

export default CreateChecklistPage;
