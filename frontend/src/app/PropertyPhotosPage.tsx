import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import PageHeading from '../components/PageHeading';
import PhotoUploadPanel from '../components/PhotoUploadPanel';
import PropertyPhotoCard from '../components/PropertyPhotoCard';
import { usePropertyDetail, usePropertyPhotos } from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import { parsePositiveId } from '../utils/propertyFormat';

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
  const [visibleCount, setVisibleCount] = useState(6);
  const property = usePropertyDetail(config, propertyId);
  const photos = usePropertyPhotos(config, propertyId);

  if (property.isPending || photos.isPending)
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

  if (property.isError || photos.isError) {
    const error = property.error ?? photos.error;
    const isNotFound = error instanceof ApiError && error.code === 'PROPERTY_NOT_FOUND';
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state content-state--error" role="alert">
            <strong>{isNotFound ? '매물을 찾을 수 없어요.' : '사진 목록을 불러오지 못했어요.'}</strong>
            <span>{getPropertyErrorMessage(error)}</span>
            {!isNotFound && (
              <button
                className="inline-button"
                type="button"
                onClick={() => void Promise.all([property.refetch(), photos.refetch()])}
              >
                다시 시도
              </button>
            )}
            <Link to="/properties">매물 목록으로 돌아가기</Link>
          </div>
        </div>
      </main>
    );
  }

  const visiblePhotos = photos.data.photos.slice(0, visibleCount);

  return (
    <main className="property-page property-photos-page">
      <div className="page-container">
        <PageHeading
          title={`${property.data.name} 사진`}
          description={`업로드 순으로 ${photos.data.totalCount}장을 보관하고 있어요.`}
          backTo={`/properties/${propertyId}`}
          backLabel="매물 상세"
        />
        <PhotoUploadPanel config={config} propertyId={propertyId} currentPhotoCount={photos.data.totalCount} />

        <section className="detail-section" aria-labelledby="photo-gallery-heading">
          <div className="section-heading-row">
            <div>
              <p className="section-eyebrow">사진 전체보기</p>
              <h2 id="photo-gallery-heading" tabIndex={-1}>
                등록한 사진 {photos.data.totalCount}장
              </h2>
            </div>
          </div>
          {photos.data.photos.length === 0 ? (
            <div className="photo-empty">
              <strong>등록한 사진이 없어요.</strong>
              <span>위에서 확인한 사진을 추가해 보세요.</span>
            </div>
          ) : (
            <ul className="photo-grid">
              {visiblePhotos.map((photo, index) => (
                <PropertyPhotoCard
                  key={photo.photoId}
                  config={config}
                  propertyId={propertyId}
                  photo={photo}
                  position={index + 1}
                />
              ))}
            </ul>
          )}
          {visibleCount < photos.data.photos.length && (
            <button
              className="secondary-button"
              type="button"
              onClick={() => setVisibleCount((current) => Math.min(current + 6, photos.data.photos.length))}
            >
              다음 사진 보기
            </button>
          )}
        </section>
      </div>
    </main>
  );
};

export default PropertyPhotosPage;
