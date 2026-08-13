import { useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import AuthenticatedPhoto from '../components/AuthenticatedPhoto';
import ConfirmDialog from '../components/ConfirmDialog';
import PageHeading from '../components/PageHeading';
import PreVisitMemoEditor from '../components/PreVisitMemoEditor';
import VisitSummaryPanel from '../components/VisitSummaryPanel';
import StartVisitPanel from '../components/StartVisitPanel';
import { usePropertyDetail } from '../hooks/query/useProperties';
import { useRemoveProperty } from '../hooks/query/usePropertyMutations';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import {
  formatDateTime,
  formatWon,
  getChecklistStageLabel,
  getSafeHttpUrl,
  parsePositiveId,
} from '../utils/propertyFormat';

type PropertyDetailPageProps = { config: PublicConfig };

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

const ResolvedPropertyDetailPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const navigate = useNavigate();
  const property = usePropertyDetail(config, propertyId);
  const removeMutation = useRemoveProperty(config, propertyId);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const deleteButtonRef = useRef<HTMLButtonElement>(null);

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
  const preview = detail.photoPreview.photos[0];

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
    <main className="property-page property-detail-page">
      <div className="page-container">
        <PageHeading
          title={detail.name}
          description={`최근 활동 ${formatDateTime(detail.lastActivityAt)}`}
          backTo="/properties"
          backLabel="매물 목록"
        />

        <div className="detail-actions" aria-label="매물 관리">
          <Link className="secondary-link" to={`/properties/${propertyId}/edit`}>
            기본 정보 수정
          </Link>
          <Link className="secondary-link" to={`/properties/${propertyId}/photos`}>
            사진 전체보기
          </Link>
        </div>

        <section className="detail-section" aria-labelledby="basic-info-heading">
          <p className="section-eyebrow">기본 정보</p>
          <h2 id="basic-info-heading">계약 조건과 발견 경로</h2>
          <dl className="detail-definition-list">
            <div>
              <dt>보증금</dt>
              <dd>{formatWon(detail.depositAmount)}</dd>
            </div>
            <div>
              <dt>월세</dt>
              <dd>{formatWon(detail.monthlyRentAmount)}</dd>
            </div>
            <div className="detail-definition-list__wide">
              <dt>발견 경로</dt>
              <dd>
                {safeSourceUrl === null ? (
                  detail.discoverySource.value
                ) : (
                  <a href={safeSourceUrl} target="_blank" rel="noopener noreferrer">
                    매물 원문 새 창에서 보기
                  </a>
                )}
              </dd>
            </div>
          </dl>
        </section>

        <section className="detail-section" aria-labelledby="photos-preview-heading">
          <div className="section-heading-row">
            <div>
              <p className="section-eyebrow">사진</p>
              <h2 id="photos-preview-heading">등록한 사진 {detail.photoPreview.totalCount}장</h2>
            </div>
            <Link className="inline-link" to={`/properties/${propertyId}/photos`}>
              {detail.photoPreview.totalCount === 0 ? '사진 추가' : '전체보기'}
            </Link>
          </div>
          {preview === undefined ? (
            <div className="photo-empty">
              <strong>등록한 사진이 없어요.</strong>
              <span>직접 확인한 사진을 안전하게 보관해 보세요.</span>
            </div>
          ) : (
            <AuthenticatedPhoto
              config={config}
              propertyId={propertyId}
              photoId={preview.photoId}
              contentUrl={preview.contentUrl}
              alt="등록한 매물 사진 미리보기"
              className="detail-photo-preview"
            />
          )}
        </section>

        <PreVisitMemoEditor key={propertyId} config={config} propertyId={propertyId} initialMemo={detail.memo} />

        <section className="detail-section" aria-labelledby="visit-heading">
          <p className="section-eyebrow">최근 방문</p>
          <h2 id="visit-heading">가장 최근 확인 결과</h2>
          <VisitSummaryPanel recentVisit={detail.recentVisit} />
          <div className="visit-detail-actions">
            {detail.recentVisit !== null && (
              <Link className="secondary-link" to={`/visits/${detail.recentVisit.visitId}`}>
                최근 방문 이어보기
              </Link>
            )}
            <Link className="secondary-link" to={`/properties/${propertyId}/visits`}>
              전체 방문 기록
            </Link>
          </div>
          <StartVisitPanel config={config} property={detail} />
        </section>

        <section className="detail-section" aria-labelledby="checklist-heading">
          <p className="section-eyebrow">활성 체크리스트</p>
          <h2 id="checklist-heading">현재 연결된 확인 단계</h2>
          <ul className="checklist-summary-list">
            {CHECKLIST_STAGES.map((stage) => {
              const checklist = detail.activeChecklists.find((item) => item.stage === stage);
              return (
                <li key={stage}>
                  <span>{getChecklistStageLabel(stage)}</span>
                  <strong>{checklist?.name ?? '연결된 체크리스트 없음'}</strong>
                  <small>{checklist === undefined ? '선택하기' : `${checklist.itemCount}개 항목`}</small>
                  <Link className="inline-link" to={`/properties/${propertyId}/active-checklists/${stage}`}>
                    {checklist === undefined ? '연결' : '변경'}
                  </Link>
                </li>
              );
            })}
          </ul>
          <p className="section-note">원본 체크리스트를 수정하면 연결한 모든 매물에 바로 반영됩니다.</p>
        </section>

        <section className="detail-section danger-section" aria-labelledby="delete-heading">
          <p className="section-eyebrow">삭제 영향</p>
          <h2 id="delete-heading">이 매물을 삭제하면</h2>
          <ul>
            <li>사진 {detail.deletionImpact.photoCount}장</li>
            <li>방문 기록 {detail.deletionImpact.visitCount}개</li>
            <li>활성 체크리스트 연결 {detail.deletionImpact.activeChecklistCount}개</li>
          </ul>
          <p>체크리스트 원본과 다른 매물의 연결은 유지됩니다.</p>
          <button
            ref={deleteButtonRef}
            className="danger-outline-button"
            type="button"
            onClick={() => setIsDeleteDialogOpen(true)}
          >
            매물 삭제
          </button>
        </section>

        <ConfirmDialog
          isOpen={isDeleteDialogOpen}
          title={`${detail.name} 매물을 삭제할까요?`}
          description={
            <>
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
