import { useState } from 'react';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { usePropertyPhotos } from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import PhotoUploadPanel from './PhotoUploadPanel';
import PropertyPhotoCard from './PropertyPhotoCard';
import styles from './PropertyPhotoManager.module.css';

type PropertyPhotoManagerProps = {
  config: PublicConfig;
  propertyId: number;
  showHeading?: boolean;
};

const PropertyPhotoManager = ({ config, propertyId, showHeading = false }: PropertyPhotoManagerProps) => {
  const [visibleCount, setVisibleCount] = useState(6);
  const photos = usePropertyPhotos(config, propertyId);

  if (photos.isPending) {
    return (
      <div className={styles.state} role="status">
        사진 정보를 불러오는 중이에요.
      </div>
    );
  }

  if (photos.isError) {
    return (
      <div className={styles.state} role="alert">
        <span>{getPropertyErrorMessage(photos.error)}</span>
        <button type="button" onClick={() => void photos.refetch()}>
          다시 시도
        </button>
      </div>
    );
  }

  const visiblePhotos = photos.data.photos.slice(0, visibleCount);

  return (
    <section id="property-photo-management" className={styles.root} aria-labelledby="photo-gallery-heading">
      <div className={`${styles.heading} ${showHeading ? '' : 'sr-only'}`}>
        <h2 id="photo-gallery-heading" tabIndex={-1}>
          사진 관리
        </h2>
      </div>
      <div className={styles.grid}>
        <PhotoUploadPanel config={config} propertyId={propertyId} currentPhotoCount={photos.data.totalCount} />
        {photos.data.photos.length === 0 ? (
          <div className={styles.empty}>
            <strong>등록한 사진이 없어요.</strong>
            <span>왼쪽 위 추가 버튼으로 사진을 등록해 보세요.</span>
          </div>
        ) : (
          <ul className={styles.photoList}>
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
      </div>
      {visibleCount < photos.data.photos.length && (
        <button
          className={styles.loadMore}
          type="button"
          onClick={() => setVisibleCount((current) => Math.min(current + 6, photos.data.photos.length))}
        >
          다음 사진 보기
        </button>
      )}
    </section>
  );
};

export default PropertyPhotoManager;
