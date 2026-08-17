import { useRef } from 'react';
import { acceptedPhotoTypes, MAX_PROPERTY_PHOTOS, usePhotoUploadQueue } from '../hooks/usePhotoUploadQueue';
import type { PublicConfig } from '../types/PublicConfig';
import Icon from './ui/Icon';
import styles from './PhotoUploadPanel.module.css';

type PhotoUploadPanelProps = {
  config: PublicConfig;
  propertyId: number;
  currentPhotoCount: number;
};

const PhotoUploadPanel = ({ config, propertyId, currentPhotoCount }: PhotoUploadPanelProps) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const uploadQueue = usePhotoUploadQueue(config, propertyId, currentPhotoCount);
  const isLimitReached = currentPhotoCount >= MAX_PROPERTY_PHOTOS;
  const uploadErrors = uploadQueue.items.filter((item) => item.status === 'error');

  return (
    <section className={styles.root} aria-labelledby="photo-upload-heading">
      <h2 className="sr-only" id="photo-upload-heading">
        사진 추가
      </h2>
      <label
        className={styles.tile}
        data-disabled={uploadQueue.isUploading || isLimitReached}
        htmlFor="property-photo-files"
      >
        <Icon name="image" size={25} />
        <strong aria-hidden="true">{uploadQueue.isUploading ? '추가 중' : '추가'}</strong>
        <small aria-hidden="true">
          {currentPhotoCount}/{MAX_PROPERTY_PHOTOS}
        </small>
        <span className="sr-only">사진 파일 선택</span>
      </label>
      <input
        id="property-photo-files"
        ref={inputRef}
        className="sr-only"
        type="file"
        accept={acceptedPhotoTypes.join(',')}
        multiple
        disabled={uploadQueue.isUploading || isLimitReached}
        aria-label="사진 파일 선택"
        aria-describedby="photo-upload-help"
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          void uploadQueue.uploadFiles(files);
          event.target.value = '';
        }}
      />
      <p id="photo-upload-help" className="sr-only">
        JPEG·PNG·WebP 형식을 사진당 10MiB 이하로 선택할 수 있어요. 원본 파일명은 저장하지 않아요.
      </p>
      {isLimitReached && (
        <p className={`${styles.feedback} form-notice`} role="status">
          사진 30장이 모두 등록되어 추가할 수 없어요.
        </p>
      )}
      {uploadErrors.length > 0 && (
        <div className={`${styles.feedback} ${styles.errorList}`} role="alert">
          {uploadErrors.map((item) => (
            <p key={item.id}>{item.message}</p>
          ))}
        </div>
      )}
    </section>
  );
};

export default PhotoUploadPanel;
