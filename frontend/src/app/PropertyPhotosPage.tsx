import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import PropertyPhotoManager from '../components/PropertyPhotoManager';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyDetail } from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './PropertyPhotosPage.module.css';

type PropertyPhotosPageProps = { config: PublicConfig };

const PropertyPhotosPage = ({ config }: PropertyPhotosPageProps) => {
  const { propertyId: propertyIdParam } = useParams();
  const propertyId = parsePositiveId(propertyIdParam);

  if (propertyId === null)
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state">
            <strong>올바른 매물 주소가 아니에요.</strong>
            <Link to="/properties">매물 목록으로 돌아가기</Link>
          </div>
        </div>
      </main>
    );
  return <ResolvedPropertyPhotosPage config={config} propertyId={propertyId} />;
};

const ResolvedPropertyPhotosPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const property = usePropertyDetail(config, propertyId);

  if (property.isPending)
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state" role="status">
            <span className="spinner" />
            사진 정보를 불러오는 중이에요.
          </div>
        </div>
      </main>
    );

  if (property.isError) {
    const error = property.error;
    const isNotFound = error instanceof ApiError && error.code === 'PROPERTY_NOT_FOUND';
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state content-state--error" role="alert">
            <strong>{isNotFound ? '매물을 찾을 수 없어요.' : '사진 목록을 불러오지 못했어요.'}</strong>
            <span>{getPropertyErrorMessage(error)}</span>
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

  return (
    <main className={styles.page}>
      <TopNavigation
        title={`${property.data.name} · 사진`}
        backTo={`/properties/${propertyId}`}
        backLabel="매물 상세로 돌아가기"
      />
      <div className={styles.container}>
        <h1 className="sr-only">{property.data.name} 사진 관리</h1>
        <PropertyPhotoManager config={config} propertyId={propertyId} />
      </div>
    </main>
  );
};

export default PropertyPhotosPage;
