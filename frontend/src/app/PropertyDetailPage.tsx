import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { visitItemStatusMeta } from '../constants/visit';
import AuthenticatedPhoto from '../components/AuthenticatedPhoto';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import ConfirmDialog from '../components/ConfirmDialog';
import PreVisitMemoEditor from '../components/PreVisitMemoEditor';
import PropertyPhotoViewer from '../components/PropertyPhotoViewer';
import { ButtonLink } from '../components/ui/Button';
import Icon from '../components/ui/Icon';
import TopNavigation from '../components/ui/TopNavigation';
import { useChecklistDetail } from '../hooks/query/useChecklists';
import { usePropertyDetail } from '../hooks/query/useProperties';
import { useRemoveProperty } from '../hooks/query/usePropertyMutations';
import { useStartPropertyVisit } from '../hooks/query/useVisitMutations';
import { useVisitDetail } from '../hooks/query/useVisits';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import type { PropertyActiveChecklist, PropertyDetail } from '../types/Property';
import type { VisitStageSnapshot } from '../types/Visit';
import { formatWon, getChecklistStageLabel, getSafeHttpUrl, parsePositiveId } from '../utils/propertyFormat';
import styles from './PropertyDetailPage.module.css';

type PropertyDetailPageProps = { config: PublicConfig };

const getStageMarkerY = (tabs: HTMLElement | null) =>
  Math.max((tabs?.getBoundingClientRect().bottom ?? 48) + 1, window.innerHeight * 0.65);

const PropertyDetailPage = ({ config }: PropertyDetailPageProps) => {
  const { propertyId: propertyIdParam } = useParams();
  const propertyId = parsePositiveId(propertyIdParam);

  if (propertyId === null) {
    return <InvalidPropertyState />;
  }

  return <ResolvedPropertyDetailPage config={config} propertyId={propertyId} />;
};

const InvalidPropertyState = () => (
  <main className="property-page">
    <div className="page-container">
      <div className="content-state">
        <strong>올바른 매물 주소가 아니에요.</strong>
        <Link to="/properties">매물 목록으로 돌아가기</Link>
      </div>
    </div>
  </main>
);

const ChecklistStageEntry = ({
  config,
  property,
  stage,
  recordedStage,
}: {
  config: PublicConfig;
  property: PropertyDetail;
  stage: (typeof CHECKLIST_STAGES)[number];
  recordedStage?: VisitStageSnapshot;
}) => {
  const checklist = property.activeChecklists.find((item) => item.stage === stage);
  const stageLabel = getChecklistStageLabel(stage);

  if (recordedStage !== undefined && property.recentVisit !== null) {
    return (
      <RecordedChecklistPreview
        visitId={property.recentVisit.visitId}
        stage={stage}
        stageLabel={stageLabel}
        recordedStage={recordedStage}
      />
    );
  }

  if (checklist === undefined) {
    return (
      <Link
        to={`/properties/${property.propertyId}/active-checklists/${stage}`}
        aria-label={`${stageLabel} 체크리스트 연결`}
      >
        <strong>연결된 체크리스트 없음</strong>
        <Icon name="arrow-right" size={18} />
      </Link>
    );
  }

  return (
    <ConnectedChecklistPreview
      config={config}
      property={property}
      checklist={checklist}
      stage={stage}
      stageLabel={stageLabel}
    />
  );
};

const RecordedChecklistPreview = ({
  visitId,
  stage,
  stageLabel,
  recordedStage,
}: {
  visitId: number;
  stage: ChecklistStage;
  stageLabel: string;
  recordedStage: VisitStageSnapshot;
}) => (
  <>
    <div className={styles.checklistPreviewHeading}>
      <strong>{recordedStage.checklistName}</strong>
      <Link to={`/visits/${visitId}?stage=${encodeURIComponent(stage)}`} aria-label={`${stageLabel} 체크하기`}>
        {recordedStage.summary.checkedCount}/{recordedStage.summary.totalCount}
        <Icon name="arrow-right" size={16} />
      </Link>
    </div>
    <ol className={styles.recordedChecklistItems} aria-label={`${stageLabel} 기록`}>
      {recordedStage.items.map((item) => {
        const status = visitItemStatusMeta[item.status];

        return (
          <li key={item.visitItemId}>
            <div className={styles.recordedQuestion}>
              <span className={styles.itemOrder}>{String(item.order).padStart(2, '0')}</span>
              <p>{item.question}</p>
              <span className={styles.recordedStatus} data-status={item.status}>
                {status.label}
              </span>
            </div>
            {item.inlineMemo.trim() !== '' && <p className={styles.recordedMemo}>{item.inlineMemo}</p>}
          </li>
        );
      })}
    </ol>
  </>
);

const ConnectedChecklistPreview = ({
  config,
  property,
  checklist,
  stage,
  stageLabel,
}: {
  config: PublicConfig;
  property: PropertyDetail;
  checklist: PropertyActiveChecklist;
  stage: ChecklistStage;
  stageLabel: string;
}) => {
  const navigate = useNavigate();
  const startVisit = useStartPropertyVisit(config, property.propertyId);
  const detail = useChecklistDetail(config, checklist.checklistId);

  const recentVisit = property.recentVisit;
  const visitPath = recentVisit === null ? null : `/visits/${recentVisit.visitId}?stage=${encodeURIComponent(stage)}`;

  const start = async () => {
    try {
      const visit = await startVisit.mutateAsync();
      navigate(`/visits/${visit.visitId}?stage=${encodeURIComponent(stage)}`);
    } catch {
      // The row stays available for a safe retry.
    }
  };

  return (
    <>
      <div className={styles.checklistPreviewHeading}>
        <strong>{checklist.name}</strong>
        {visitPath === null ? (
          <button
            type="button"
            aria-label={`${stageLabel} 체크 시작`}
            disabled={startVisit.isPending}
            onClick={() => void start()}
          >
            {startVisit.isPending ? '여는 중…' : '체크 시작'}
          </button>
        ) : (
          <Link to={visitPath} aria-label={`${stageLabel} 체크하기`}>
            체크 보기 <Icon name="arrow-right" size={16} />
          </Link>
        )}
      </div>

      {detail.isPending ? (
        <div className={styles.checklistPreviewState} role="status">
          체크리스트를 불러오는 중이에요.
        </div>
      ) : detail.isError ? (
        <div className={styles.checklistPreviewState} role="alert">
          <span>체크리스트를 불러오지 못했어요.</span>
          <button type="button" onClick={() => void detail.refetch()}>
            다시 시도
          </button>
        </div>
      ) : (
        <ol className={styles.checklistPreviewItems}>
          {detail.data.items.map((item) => (
            <li key={item.checklistItemId}>
              <span>{String(item.order).padStart(2, '0')}</span>
              <p>{item.question}</p>
            </li>
          ))}
        </ol>
      )}
      {startVisit.isError && (
        <span className={styles.checklistError} role="alert">
          체크 화면을 열지 못했어요. 다시 눌러 주세요.
        </span>
      )}
    </>
  );
};

const PropertyChecklistList = ({
  config,
  property,
  recordedStages = [],
  isRecordedVisitPending = false,
}: {
  config: PublicConfig;
  property: PropertyDetail;
  recordedStages?: VisitStageSnapshot[];
  isRecordedVisitPending?: boolean;
}) => (
  <ul className={styles.checklistList}>
    {CHECKLIST_STAGES.map((stage) => {
      const recordedStage = recordedStages.find((item) => item.stage === stage);
      const isLinked =
        isRecordedVisitPending ||
        recordedStage !== undefined ||
        property.activeChecklists.some((item) => item.stage === stage);

      return (
        <li
          id={`property-checklist-section-${stage}`}
          className={styles.checklistStageSection}
          data-checklist-stage={stage}
          key={stage}
        >
          <h3 className={styles.checklistStageHeading}>{getChecklistStageLabel(stage)}</h3>
          <div
            className={`${styles.checklistStageCard} ${isLinked ? styles.linkedChecklist : styles.unlinkedChecklist}`}
          >
            {isRecordedVisitPending ? (
              <div className={styles.checklistPreviewHeading}>
                <strong>최근 체크 기록을 불러오는 중이에요.</strong>
              </div>
            ) : (
              <ChecklistStageEntry config={config} property={property} stage={stage} recordedStage={recordedStage} />
            )}
          </div>
        </li>
      );
    })}
  </ul>
);

const PropertyChecklistListWithVisit = ({ config, property }: { config: PublicConfig; property: PropertyDetail }) => {
  const visitId = property.recentVisit?.visitId;
  if (visitId === undefined) return <PropertyChecklistList config={config} property={property} />;
  return <RecordedVisitChecklistList config={config} property={property} visitId={visitId} />;
};

const RecordedVisitChecklistList = ({
  config,
  property,
  visitId,
}: {
  config: PublicConfig;
  property: PropertyDetail;
  visitId: number;
}) => {
  const visit = useVisitDetail(config, visitId);

  return (
    <>
      {visit.isError && (
        <p className={styles.visitLoadNotice} role="status">
          최근 체크 기록을 불러오지 못해 현재 체크리스트를 표시해요.
        </p>
      )}
      <PropertyChecklistList
        config={config}
        property={property}
        recordedStages={visit.data?.stages}
        isRecordedVisitPending={visit.isPending}
      />
    </>
  );
};

const ResolvedPropertyDetailPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const navigate = useNavigate();
  const property = usePropertyDetail(config, propertyId);
  const removeMutation = useRemoveProperty(config, propertyId);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedPhotoIndex, setSelectedPhotoIndex] = useState<number | null>(null);
  const [selectedStage, setSelectedStage] = useState<ChecklistStage>('ONLINE_PHONE');
  const deleteButtonRef = useRef<HTMLButtonElement>(null);
  const photoTriggerRef = useRef<HTMLButtonElement | null>(null);
  const stickyStageTabsRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (property.data === undefined) return;

    const updateSelectedStage = () => {
      const isAtPageBottom =
        window.scrollY > 0 && window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 4;
      if (isAtPageBottom) {
        setSelectedStage('PRE_CONTRACT');
        return;
      }

      const markerY = getStageMarkerY(stickyStageTabsRef.current);
      const sections = CHECKLIST_STAGES.map((stage) => ({
        stage,
        rect: document.getElementById(`property-checklist-section-${stage}`)?.getBoundingClientRect(),
      })).filter((section) => section.rect !== undefined);
      const sectionAtMarker = sections.find(
        ({ rect }) => rect !== undefined && rect.top <= markerY + 1 && rect.bottom > markerY,
      );
      const lastPassedSection = sections.reduce<(typeof sections)[number] | undefined>(
        (current, section) => (section.rect !== undefined && section.rect.top <= markerY + 1 ? section : current),
        undefined,
      );

      setSelectedStage(sectionAtMarker?.stage ?? lastPassedSection?.stage ?? 'ONLINE_PHONE');
    };

    window.addEventListener('scroll', updateSelectedStage, { passive: true });
    window.addEventListener('resize', updateSelectedStage);
    updateSelectedStage();

    return () => {
      window.removeEventListener('scroll', updateSelectedStage);
      window.removeEventListener('resize', updateSelectedStage);
    };
  }, [property.data]);

  const selectChecklistStage = (stage: ChecklistStage) => {
    setSelectedStage(stage);
    const section = document.getElementById(`property-checklist-section-${stage}`);
    if (section === null) return;

    window.scrollTo({
      top: window.scrollY + section.getBoundingClientRect().top - getStageMarkerY(stickyStageTabsRef.current),
      behavior: 'smooth',
    });
  };

  const closePhotoViewer = () => {
    setSelectedPhotoIndex(null);
    window.requestAnimationFrame(() => photoTriggerRef.current?.focus());
  };

  if (property.isPending) {
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state" role="status">
            <span className="spinner" />
            매물 상세를 불러오는 중이에요.
          </div>
        </div>
      </main>
    );
  }

  if (property.isError) {
    const isNotFound = property.error instanceof ApiError && property.error.code === 'PROPERTY_NOT_FOUND';
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state content-state--error" role="alert">
            <strong>{isNotFound ? '매물을 찾을 수 없어요.' : '매물 상세를 불러오지 못했어요.'}</strong>
            <span>{getPropertyErrorMessage(property.error)}</span>
            {!isNotFound && (
              <button className="inline-button" type="button" onClick={() => void property.refetch()}>
                다시 시도
              </button>
            )}
            <Link to="/properties">매물 목록으로 돌아가기</Link>
          </div>
        </div>
      </main>
    );
  }

  const detail = property.data;
  const safeSourceUrl = detail.discoverySource.type === 'URL' ? getSafeHttpUrl(detail.discoverySource.value) : null;

  const deleteProperty = async () => {
    try {
      await removeMutation.mutateAsync();
      setIsDeleteDialogOpen(false);
      navigate('/properties', { replace: true, state: { focusHeading: true } });
    } catch {
      // Dialog remains open with a safe retryable error.
    }
  };

  return (
    <main className={`${styles.page} property-detail-page`}>
      <div className={styles.container}>
        <TopNavigation
          title={detail.name}
          backTo="/properties"
          backLabel="매물 목록으로 돌아가기"
          endSlot={
            <details className={styles.pageMenu}>
              <summary aria-label="매물 메뉴 열기">
                <Icon name="more-vertical" size={22} />
              </summary>
              <div className={styles.pageMenuItems}>
                <Link to={`/properties/${propertyId}/edit`}>수정</Link>
                <button
                  ref={deleteButtonRef}
                  type="button"
                  className={styles.deleteMenuItem}
                  aria-label="매물 삭제"
                  onClick={() => setIsDeleteDialogOpen(true)}
                >
                  삭제
                </button>
              </div>
            </details>
          }
        />
        {detail.photoPreview.photos.length > 0 && (
          <section className={styles.photoSection} aria-label="매물 사진">
            <div className={styles.photoGrid} data-photo-count={Math.min(detail.photoPreview.photos.length, 3)}>
              {detail.photoPreview.photos.slice(0, 3).map((photo, index) => {
                const isMorePreview = index === 2 && detail.photoPreview.totalCount > 2;
                const remainingPhotoCount = detail.photoPreview.totalCount - 2;

                return (
                  <button
                    type="button"
                    className={styles.photoThumbnailLink}
                    key={photo.photoId}
                    aria-label={
                      isMorePreview
                        ? `나머지 사진 ${remainingPhotoCount}장 크게 보기`
                        : `${detail.name} 사진 ${index + 1} 크게 보기`
                    }
                    onClick={(event) => {
                      photoTriggerRef.current = event.currentTarget;
                      setSelectedPhotoIndex(index);
                    }}
                  >
                    <AuthenticatedPhoto
                      config={config}
                      propertyId={propertyId}
                      photoId={photo.photoId}
                      contentUrl={photo.contentUrl}
                      alt=""
                      className={styles.photoThumbnail}
                    />
                    {isMorePreview && (
                      <span className={styles.photoMoreOverlay}>
                        보기 <Icon name="arrow-right" size={18} />
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </section>
        )}

        <h1 className={styles.propertyTitle}>{detail.name}</h1>

        <section className={styles.basicSection} aria-labelledby="basic-info-heading">
          <h2 id="basic-info-heading" className="sr-only">
            매물 기본 정보
          </h2>
          <p className={styles.priceSummary}>
            보증금 {formatWon(detail.depositAmount)} <span aria-hidden="true">·</span> 월세{' '}
            {formatWon(detail.monthlyRentAmount)}
          </p>
          <div className={styles.discoverySource}>
            <span className={styles.discoveryLabel}>
              <Icon name="link" size={17} /> 확인한 곳
            </span>
            {safeSourceUrl === null ? (
              <span className={styles.discoveryValue}>{detail.discoverySource.value}</span>
            ) : (
              <a href={safeSourceUrl} target="_blank" rel="noopener noreferrer">
                매물 원문 보기 <Icon name="external-link" size={17} />
              </a>
            )}
          </div>
        </section>

        <PreVisitMemoEditor key={propertyId} config={config} propertyId={propertyId} initialMemo={detail.memo} />

        <section className={styles.checklistSection} aria-labelledby="checklist-heading">
          <div className={styles.checklistSectionHeader}>
            <h2 id="checklist-heading">체크리스트</h2>
            <ButtonLink
              className={styles.manageChecklistLink}
              variant="text"
              to={`/properties/${propertyId}/active-checklists/ONLINE_PHONE`}
            >
              연결 관리
            </ButtonLink>
          </div>
          <div ref={stickyStageTabsRef} className={styles.stickyStageTabs}>
            <ChecklistStageTabs
              stage={selectedStage}
              idPrefix="property-checklist-stage"
              onSelect={selectChecklistStage}
            />
          </div>
          <PropertyChecklistListWithVisit config={config} property={detail} />
        </section>

        {selectedPhotoIndex !== null && (
          <PropertyPhotoViewer
            config={config}
            propertyId={propertyId}
            propertyName={detail.name}
            initialIndex={selectedPhotoIndex}
            onClose={closePhotoViewer}
          />
        )}

        <ConfirmDialog
          isOpen={isDeleteDialogOpen}
          title={`${detail.name} 매물을 삭제할까요?`}
          description={
            <>
              <p>
                <strong>삭제 전 아래 내용을 확인해 주세요.</strong>
              </p>
              <p>매물과 연결된 사진·방문 기록·활성 연결이 삭제되며 되돌릴 수 없습니다.</p>
              <ul>
                <li>사진 {detail.deletionImpact.photoCount}장</li>
                <li>방문 {detail.deletionImpact.visitCount}개</li>
                <li>활성 연결 {detail.deletionImpact.activeChecklistCount}개</li>
              </ul>
              <p>사용자가 만든 체크리스트 원본과 다른 매물 연결은 유지됩니다.</p>
            </>
          }
          confirmLabel="매물 삭제"
          isConfirming={removeMutation.isPending}
          returnFocusRef={deleteButtonRef}
          onCancel={() => setIsDeleteDialogOpen(false)}
          onConfirm={() => void deleteProperty()}
        >
          {removeMutation.isError && (
            <p className="form-error" role="alert">
              {getPropertyErrorMessage(removeMutation.error)} 매물은 그대로 유지됩니다.
            </p>
          )}
        </ConfirmDialog>
      </div>
    </main>
  );
};

export default PropertyDetailPage;
