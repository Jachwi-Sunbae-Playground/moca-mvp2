import { useEffect, useRef, useState } from 'react';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { usePropertyPhotos } from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import AuthenticatedPhoto from './AuthenticatedPhoto';
import Icon from './ui/Icon';
import styles from './PropertyPhotoViewer.module.css';

type PropertyPhotoViewerProps = {
  config: PublicConfig;
  propertyId: number;
  propertyName: string;
  initialIndex: number;
  onClose: () => void;
};

const PropertyPhotoViewer = ({ config, propertyId, propertyName, initialIndex, onClose }: PropertyPhotoViewerProps) => {
  const photos = usePropertyPhotos(config, propertyId);
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    closeButtonRef.current?.focus();

    const closeWithEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', closeWithEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', closeWithEscape);
    };
  }, [onClose]);

  const photoList = photos.data?.photos ?? [];
  const safeIndex = Math.min(currentIndex, Math.max(photoList.length - 1, 0));
  const currentPhoto = photoList[safeIndex];

  return (
    <div
      className={styles.backdrop}
      role="dialog"
      aria-modal="true"
      aria-label={`${propertyName} 사진 크게 보기`}
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) onClose();
      }}
    >
      <div className={styles.viewer}>
        <header className={styles.header}>
          <strong>{propertyName}</strong>
          {photoList.length > 0 && (
            <span aria-live="polite">
              {safeIndex + 1} / {photoList.length}
            </span>
          )}
          <button ref={closeButtonRef} type="button" aria-label="사진 크게 보기 닫기" onClick={onClose}>
            <Icon name="close" size={24} />
          </button>
        </header>

        <div className={styles.photoArea}>
          {photos.isPending ? (
            <div className={styles.state} role="status">
              사진을 불러오는 중이에요.
            </div>
          ) : photos.isError ? (
            <div className={styles.state} role="alert">
              <span>{getPropertyErrorMessage(photos.error)}</span>
              <button type="button" onClick={() => void photos.refetch()}>
                다시 시도
              </button>
            </div>
          ) : currentPhoto === undefined ? (
            <div className={styles.state}>표시할 사진이 없어요.</div>
          ) : (
            <AuthenticatedPhoto
              config={config}
              propertyId={propertyId}
              photoId={currentPhoto.photoId}
              contentUrl={currentPhoto.contentUrl}
              alt={`${propertyName} 사진 ${safeIndex + 1}`}
              className={styles.photo}
            />
          )}
        </div>

        {photoList.length > 1 && (
          <div className={styles.controls}>
            <button
              type="button"
              aria-label="이전 사진"
              disabled={safeIndex === 0}
              onClick={() => setCurrentIndex((index) => Math.max(index - 1, 0))}
            >
              <Icon name="arrow-left" size={24} />
            </button>
            <button
              type="button"
              aria-label="다음 사진"
              disabled={safeIndex === photoList.length - 1}
              onClick={() => setCurrentIndex((index) => Math.min(index + 1, photoList.length - 1))}
            >
              <Icon name="arrow-right" size={24} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default PropertyPhotoViewer;
