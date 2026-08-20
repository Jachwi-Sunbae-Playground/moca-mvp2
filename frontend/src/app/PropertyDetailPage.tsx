import { useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import AuthenticatedPhoto from '../components/AuthenticatedPhoto';
import ConfirmDialog from '../components/ConfirmDialog';
import PropertyPhotoViewer from '../components/PropertyPhotoViewer';
import Icon from '../components/ui/Icon';
import TopNavigation from '../components/ui/TopNavigation';
import TopNavigationMenu from '../components/ui/TopNavigationMenu';
import { usePropertyChecklistOverview, usePropertyDetail, usePropertyMemo } from '../hooks/query/useProperties';
import { useRemoveProperty } from '../hooks/query/usePropertyMutations';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import { formatWon, getChecklistStageLabel, getSafeHttpUrl, parsePositiveId } from '../utils/propertyFormat';
import styles from './PropertyDetailPage.module.css';

const PropertyDetailPage = ({ config }: { config: PublicConfig }) => {
  const propertyId = parsePositiveId(useParams().propertyId);
  if (propertyId === null) {
    return (
      <main className="property-page">
        <div className="content-state">
          <strong>올바른 매물 주소가 아니에요.</strong>
          <Link to="/properties">매물 목록으로 돌아가기</Link>
        </div>
      </main>
    );
  }
  return <ResolvedPropertyDetailPage config={config} propertyId={propertyId} />;
};

const ResolvedPropertyDetailPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const navigate = useNavigate();
  const property = usePropertyDetail(config, propertyId);
  const memo = usePropertyMemo(config, propertyId);
  const checklists = usePropertyChecklistOverview(config, propertyId);
  const removeMutation = useRemoveProperty(config, propertyId);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedPhotoIndex, setSelectedPhotoIndex] = useState<number | null>(null);
  const deleteButtonRef = useRef<HTMLButtonElement>(null);
  const photoTriggerRef = useRef<HTMLButtonElement | null>(null);

  if (property.isPending) return <div className="content-state">매물 상세를 불러오는 중이에요.</div>;
  if (property.isError) {
    const isNotFound = property.error instanceof ApiError && property.error.code === 'PROPERTY_NOT_FOUND';
    return (
      <main className="property-page">
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
      </main>
    );
  }

  const detail = property.data;
  const safeSourceUrl = detail.discoverySource.type === 'URL' ? getSafeHttpUrl(detail.discoverySource.value) : null;
  const highlightedMemoItems = memo.data?.items.filter((item) => item.content.trim() !== '') ?? [];
  const progress = checklists.data?.overallProgress;

  const deleteProperty = async () => {
    try {
      await removeMutation.mutateAsync();
      setIsDeleteDialogOpen(false);
      navigate('/properties', { replace: true, state: { focusHeading: true } });
    } catch {
      // 삭제 확인 창에서 재시도할 수 있도록 유지한다.
    }
  };

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <TopNavigation
          title="매물 정보"
          backTo="/properties"
          backLabel="매물 목록으로 돌아가기"
          endSlot={
            <TopNavigationMenu label="매물 메뉴 열기">
              <Link to={`/properties/${propertyId}/edit`}>수정</Link>
              <button
                ref={deleteButtonRef}
                type="button"
                data-tone="danger"
                onClick={() => {
                  removeMutation.reset();
                  setIsDeleteDialogOpen(true);
                }}
              >
                삭제
              </button>
            </TopNavigationMenu>
          }
        />

        <section className={styles.basicSection}>
          <h1>{detail.name}</h1>
          <p className={styles.priceSummary}>
            보증금 {formatWon(detail.depositAmount)} <span aria-hidden="true">/</span> 월세{' '}
            {formatWon(detail.monthlyRentAmount)}
          </p>
          {detail.discoverySource.value !== '' && (
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
          )}
        </section>

        <section className={styles.photoSection} aria-label="매물 사진">
          <div className={styles.sectionHeading}>
            <div>
              <strong>사진</strong>
              <span>{detail.photoPreview.totalCount}장</span>
            </div>
            <Link to={`/properties/${propertyId}/photos`}>
              <Icon name="plus" size={16} /> 사진 관리
            </Link>
          </div>
          {detail.photoPreview.photos.length > 0 ? (
            <div className={styles.photoGrid} data-photo-count={Math.min(detail.photoPreview.photos.length, 3)}>
              {detail.photoPreview.photos.slice(0, 3).map((photo, index) => {
                const isMorePreview = index === 2 && detail.photoPreview.totalCount > 3;
                return (
                  <button
                    type="button"
                    className={styles.photoThumbnailLink}
                    key={photo.photoId}
                    aria-label={`${detail.name} 사진 ${index + 1} 크게 보기`}
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
                      <span className={styles.photoMoreOverlay}>+{detail.photoPreview.totalCount - 2}</span>
                    )}
                  </button>
                );
              })}
            </div>
          ) : (
            <Link className={styles.emptyPhotoLink} to={`/properties/${propertyId}/photos`}>
              사진을 추가해 주세요.
            </Link>
          )}
        </section>

        <section className={styles.memoSection} aria-labelledby="memo-heading">
          <div className={styles.sectionHeading}>
            <strong id="memo-heading">메모</strong>
            <Link to={`/properties/${propertyId}/memo`}>
              메모 작성 <Icon name="arrow-right" size={16} />
            </Link>
          </div>
          {memo.isPending ? (
            <p className={styles.sectionState}>메모를 불러오는 중이에요.</p>
          ) : memo.isError ? (
            <button className={styles.sectionRetry} type="button" onClick={() => void memo.refetch()}>
              메모를 불러오지 못했어요. 다시 시도
            </button>
          ) : highlightedMemoItems.length === 0 && memo.data.freeMemo.trim() === '' ? (
            <p className={styles.sectionState}>아직 작성한 메모가 없어요.</p>
          ) : (
            <ul className={styles.memoSummary}>
              {memo.data.items.map((item) => (
                <li className={item.content.trim() === '' ? undefined : styles.hasMemo} key={item.systemMemoItemId}>
                  {item.label}
                </li>
              ))}
              {memo.data.freeMemo.trim() !== '' && <li className={styles.hasMemo}>추가 메모</li>}
            </ul>
          )}
        </section>

        <section className={styles.checklistSection} aria-labelledby="checklist-heading">
          <div className={styles.checklistHeading}>
            <div>
              <h2 id="checklist-heading">3단계 체크리스트</h2>
              <p>단계를 누르면 체크리스트를 확인하거나 연결할 수 있어요.</p>
            </div>
            {progress !== undefined && (
              <strong>
                {progress.completedCount}/{progress.totalCount}
              </strong>
            )}
          </div>
          {progress !== undefined && (
            <div className={styles.progressTrack} aria-label={`체크 진행 ${progress.progressRate}%`}>
              {progress.goodCount > 0 && (
                <span className={styles.progressGood} style={{ flexGrow: progress.goodCount }} />
              )}
              {progress.cautionCount > 0 && (
                <span className={styles.progressCaution} style={{ flexGrow: progress.cautionCount }} />
              )}
              {progress.unconfirmedCount > 0 && (
                <span className={styles.progressUnconfirmed} style={{ flexGrow: progress.unconfirmedCount }} />
              )}
            </div>
          )}
          {checklists.isError ? (
            <button className={styles.sectionRetry} type="button" onClick={() => void checklists.refetch()}>
              체크리스트 연결을 불러오지 못했어요. 다시 시도
            </button>
          ) : (
            <ol className={styles.checklistList}>
              {CHECKLIST_STAGES.map((stage, index) => {
                const item = checklists.data?.stages.find((candidate) => candidate.stage === stage);
                const checklistPath =
                  item?.applied === true && item.propertyChecklistId !== null
                    ? `/properties/${propertyId}/checklists/${item.propertyChecklistId}`
                    : `/properties/${propertyId}/active-checklists/${stage}`;
                return (
                  <li key={stage}>
                    <Link to={checklistPath}>
                      <span className={styles.stageNumber}>{index + 1}</span>
                      <span className={styles.stageCopy}>
                        <strong>{getChecklistStageLabel(stage)}</strong>
                        <small>{item?.applied === true ? item.checklistName : '연결된 체크리스트 없음'}</small>
                      </span>
                      <Icon name="arrow-right" size={18} />
                    </Link>
                  </li>
                );
              })}
            </ol>
          )}
        </section>

        {selectedPhotoIndex !== null && (
          <PropertyPhotoViewer
            config={config}
            propertyId={propertyId}
            propertyName={detail.name}
            initialIndex={selectedPhotoIndex}
            onClose={() => {
              setSelectedPhotoIndex(null);
              window.requestAnimationFrame(() => photoTriggerRef.current?.focus());
            }}
          />
        )}

        <ConfirmDialog
          isOpen={isDeleteDialogOpen}
          title={`${detail.name} 매물을 삭제할까요?`}
          description="사진·메모·연결된 체크리스트도 함께 삭제되며 되돌릴 수 없습니다."
          confirmLabel="매물 삭제"
          isConfirming={removeMutation.isPending}
          returnFocusRef={deleteButtonRef}
          onCancel={() => setIsDeleteDialogOpen(false)}
          onConfirm={() => void deleteProperty()}
        >
          {removeMutation.isError && (
            <p role="alert" className={styles.deleteError}>
              매물을 삭제하지 못했습니다. 매물은 그대로 유지됩니다.
            </p>
          )}
        </ConfirmDialog>
      </div>
    </main>
  );
};

export default PropertyDetailPage;
