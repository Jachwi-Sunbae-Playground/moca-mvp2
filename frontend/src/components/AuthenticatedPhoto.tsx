import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { useAuthenticatedPhoto } from '../hooks/query/useAuthenticatedPhoto';
import type { PublicConfig } from '../types/PublicConfig';
import './AuthenticatedPhoto.css';

type AuthenticatedPhotoProps = {
  config: PublicConfig;
  propertyId: number;
  photoId: number;
  contentUrl: string;
  alt: string;
  className?: string;
};

const AuthenticatedPhoto = ({
  config,
  propertyId,
  photoId,
  contentUrl,
  alt,
  className = '',
}: AuthenticatedPhotoProps) => {
  const photo = useAuthenticatedPhoto(config, propertyId, photoId, contentUrl);

  if (photo.isError) {
    return (
      <div
        className={`authenticated-photo authenticated-photo--error ${className}`}
        role="group"
        aria-label={`${alt} 불러오기 실패`}
      >
        <span>{getPropertyErrorMessage(photo.error)}</span>
        <button type="button" className="inline-button" onClick={() => void photo.refetch()}>
          다시 시도
        </button>
      </div>
    );
  }

  if (photo.isPending || photo.objectUrl === null) {
    return (
      <div className={`authenticated-photo authenticated-photo--loading ${className}`} role="status">
        <span className="spinner" aria-hidden="true" />
        <span>사진을 불러오는 중</span>
      </div>
    );
  }

  return <img className={`authenticated-photo__image ${className}`} src={photo.objectUrl} alt={alt} />;
};

export default AuthenticatedPhoto;
