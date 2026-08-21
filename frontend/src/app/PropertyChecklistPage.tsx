import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import ChecklistPageLayout from '../components/ChecklistPageLayout';
import PropertyChecklistItemControl from '../components/PropertyChecklistItemControl';
import {
  usePropertyChecklistDetail,
  usePropertyChecklistOverview,
  usePropertyDetail,
} from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import { getChecklistStageLabel, parsePositiveId } from '../utils/propertyFormat';
import styles from './PropertyChecklistPage.module.css';

const PropertyChecklistPage = ({ config }: { config: PublicConfig }) => {
  const params = useParams();
  const propertyId = parsePositiveId(params.propertyId);
  const propertyChecklistId = parsePositiveId(params.propertyChecklistId);

  if (propertyId === null || propertyChecklistId === null) {
    return (
      <main className="property-page">
        <div className="content-state">
          <strong>올바른 매물 체크리스트 주소가 아니에요.</strong>
          <Link to="/properties">매물 목록으로 돌아가기</Link>
        </div>
      </main>
    );
  }

  return (
    <ResolvedPropertyChecklistPage
      key={propertyChecklistId}
      config={config}
      propertyId={propertyId}
      propertyChecklistId={propertyChecklistId}
    />
  );
};

const ResolvedPropertyChecklistPage = ({
  config,
  propertyId,
  propertyChecklistId,
}: {
  config: PublicConfig;
  propertyId: number;
  propertyChecklistId: number;
}) => {
  const checklist = usePropertyChecklistDetail(config, propertyId, propertyChecklistId);
  const overview = usePropertyChecklistOverview(config, propertyId);
  const property = usePropertyDetail(config, propertyId);

  if (checklist.isPending) {
    return <div className="content-state">체크리스트를 불러오는 중이에요.</div>;
  }

  if (checklist.isError) {
    const isNotFound = checklist.error instanceof ApiError && checklist.error.code === 'PROPERTY_CHECKLIST_NOT_FOUND';
    return (
      <main className="property-page">
        <div className="content-state content-state--error" role="alert">
          <strong>{isNotFound ? '연결된 체크리스트를 찾을 수 없어요.' : '체크리스트를 불러오지 못했어요.'}</strong>
          {!isNotFound && (
            <button className="inline-button" type="button" onClick={() => void checklist.refetch()}>
              다시 시도
            </button>
          )}
          <Link to={`/properties/${propertyId}`}>매물 상세로 돌아가기</Link>
        </div>
      </main>
    );
  }

  const detail = checklist.data;
  const completedCount = detail.items.filter((item) => item.status !== 'UNCONFIRMED').length;
  const getStagePath = (stage: typeof detail.stage) => {
    const stageSummary = overview.data?.stages.find((item) => item.stage === stage);
    if (stageSummary?.applied === true && stageSummary.propertyChecklistId !== null) {
      return `/properties/${propertyId}/checklists/${stageSummary.propertyChecklistId}`;
    }
    return `/properties/${propertyId}/active-checklists/${stage}?from=property-detail`;
  };

  return (
    <ChecklistPageLayout
      title={property.data === undefined ? '매물 체크리스트' : `${property.data.name} 체크리스트`}
      backTo={`/properties/${propertyId}`}
      backLabel="매물 상세로 돌아가기"
      stage={detail.stage}
      getStageTo={getStagePath}
      endSlot={
        <Link
          className={styles.replaceLink}
          to={`/properties/${propertyId}/active-checklists/${detail.stage}?from=property-detail&mode=replace`}
          aria-label="체크리스트 변경"
        >
          변경
        </Link>
      }
    >
      <header className={styles.heading}>
        <div>
          <span>{getChecklistStageLabel(detail.stage)}</span>
          <h1>{detail.checklistName}</h1>
        </div>
        <strong>
          {completedCount}/{detail.items.length}
        </strong>
      </header>
      <ol className={styles.items}>
        {detail.items.map((item) => (
          <PropertyChecklistItemControl
            key={item.itemId}
            config={config}
            propertyId={propertyId}
            propertyChecklistId={propertyChecklistId}
            item={item}
          />
        ))}
      </ol>
    </ChecklistPageLayout>
  );
};

export default PropertyChecklistPage;
