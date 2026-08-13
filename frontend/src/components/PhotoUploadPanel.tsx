import { useRef } from 'react';
import { acceptedPhotoTypes, MAX_PROPERTY_PHOTOS, usePhotoUploadQueue } from '../hooks/usePhotoUploadQueue';
import type { PublicConfig } from '../types/PublicConfig';

type PhotoUploadPanelProps = {
  config: PublicConfig;
  propertyId: number;
  currentPhotoCount: number;
};

const PhotoUploadPanel = ({ config, propertyId, currentPhotoCount }: PhotoUploadPanelProps) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const uploadQueue = usePhotoUploadQueue(config, propertyId, currentPhotoCount);
  const isLimitReached = currentPhotoCount >= MAX_PROPERTY_PHOTOS;

  return (
    <section className="detail-section photo-upload-panel" aria-labelledby="photo-upload-heading">
      <div className="section-heading-row">
        <div>
          <p className="section-eyebrow">사진 추가</p>
          <h2 id="photo-upload-heading">한 장씩 안전하게 올려요</h2>
        </div>
        <span>
          {currentPhotoCount} / {MAX_PROPERTY_PHOTOS}
        </span>
      </div>
      <p className="section-note">
        JPEG·PNG·WebP, 사진당 10MiB 이하. 여러 장을 선택하면 순서대로 업로드하며 실패해도 나머지는 계속 처리합니다.
      </p>
      <label className="file-input-label" htmlFor="property-photo-files">
        사진 파일 선택
      </label>
      <input
        id="property-photo-files"
        ref={inputRef}
        className="file-input"
        type="file"
        accept={acceptedPhotoTypes.join(',')}
        multiple
        disabled={uploadQueue.isUploading || isLimitReached}
        aria-describedby="photo-upload-help"
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          void uploadQueue.uploadFiles(files);
          event.target.value = '';
        }}
      />
      <p id="photo-upload-help" className="field-help">
        원본 파일명은 서버 식별자로 저장하거나 사진 설명으로 사용하지 않아요.
      </p>
      {isLimitReached && (
        <p className="form-notice" role="status">
          사진 30장이 모두 등록되어 추가할 수 없어요.
        </p>
      )}
      {uploadQueue.items.length > 0 && (
        <div className="upload-queue" aria-live="polite">
          <div className="upload-queue__heading">
            <strong>업로드 결과</strong>
            <button
              type="button"
              className="text-button"
              disabled={uploadQueue.isUploading}
              onClick={uploadQueue.clearItems}
            >
              결과 지우기
            </button>
          </div>
          <ul>
            {uploadQueue.items.map((item) => (
              <li key={item.id} data-status={item.status}>
                <span>{item.label}</span>
                <strong>{item.message}</strong>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
};

export default PhotoUploadPanel;
