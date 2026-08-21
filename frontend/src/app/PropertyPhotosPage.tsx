import { Link, useParams } from 'react-router-dom';
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

  return (
    <main className={styles.page}>
      <TopNavigation
        title={property.data === undefined ? '사진 관리' : `${property.data.name} · 사진`}
        backTo={`/properties/${propertyId}`}
        backLabel="매물 상세로 돌아가기"
      />
      <div className={styles.container}>
        <h1 className="sr-only">{property.data === undefined ? '매물' : property.data.name} 사진 관리</h1>
        <PropertyPhotoManager config={config} propertyId={propertyId} />
      </div>
    </main>
  );
};

export default PropertyPhotosPage;
