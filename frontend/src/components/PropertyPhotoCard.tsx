import { useRef, useState } from 'react';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { useRemovePropertyPhoto, useSetRepresentativePropertyPhoto } from '../hooks/query/usePropertyMutations';
import type { PropertyPhoto } from '../types/Property';
import type { PublicConfig } from '../types/PublicConfig';
import AuthenticatedPhoto from './AuthenticatedPhoto';
import ConfirmDialog from './ConfirmDialog';
import dialogStyles from './ConfirmDialog.module.css';
import styles from './PropertyPhotoCard.module.css';

type PropertyPhotoCardProps = {
  config: PublicConfig;
  propertyId: number;
  photo: PropertyPhoto;
  position: number;
};

const PropertyPhotoCard = ({ config, propertyId, photo, position }: PropertyPhotoCardProps) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const deleteButtonRef = useRef<HTMLButtonElement>(null);
  const removeMutation = useRemovePropertyPhoto(config, propertyId);
  const representativeMutation = useSetRepresentativePropertyPhoto(config, propertyId);
  const accessibleName = `업로드 순 ${position}번째 사진`;

  const remove = async () => {
    try {
      await removeMutation.mutateAsync(photo.photoId);
      setIsDialogOpen(false);
      window.setTimeout(() => document.getElementById('photo-gallery-heading')?.focus(), 0);
    } catch {
      // Dialog remains open so the user can retry without optimistic removal.
    }
  };

  return (
    <li className={styles.item}>
      <AuthenticatedPhoto
        config={config}
        propertyId={propertyId}
        photoId={photo.photoId}
        contentUrl={photo.contentUrl}
        alt={accessibleName}
      />
      <button
        className={`${styles.representativeButton} ${photo.representative === true ? styles.isRepresentative : ''}`}
        type="button"
        aria-label={
          photo.representative === true ? `${accessibleName} 대표 사진` : `${accessibleName}을 대표 사진으로 지정`
        }
        aria-pressed={photo.representative === true}
        disabled={photo.representative === true || representativeMutation.isPending}
        onClick={() => representativeMutation.mutate(photo.photoId)}
      >
        <span aria-hidden="true">{photo.representative === true ? '★' : '☆'}</span>
        대표
      </button>
      <button
        ref={deleteButtonRef}
        className={styles.deleteButton}
        type="button"
        aria-label={`${accessibleName} 삭제`}
        onClick={() => setIsDialogOpen(true)}
      >
        삭제
      </button>
      {representativeMutation.isError && (
        <p className={styles.representativeError} role="alert">
          대표 사진 지정 실패
        </p>
      )}
      <ConfirmDialog
        isOpen={isDialogOpen}
        title="이 사진을 삭제할까요?"
        description={<p>{accessibleName}을 삭제합니다. 다른 사진과 매물은 그대로 유지됩니다.</p>}
        confirmLabel="사진 삭제"
        isConfirming={removeMutation.isPending}
        returnFocusRef={deleteButtonRef}
        onCancel={() => setIsDialogOpen(false)}
        onConfirm={() => void remove()}
      >
        {isDialogOpen && (
          <AuthenticatedPhoto
            config={config}
            propertyId={propertyId}
            photoId={photo.photoId}
            contentUrl={photo.contentUrl}
            alt={`삭제할 ${accessibleName}`}
            className={dialogStyles.photo}
          />
        )}
        {removeMutation.isError && (
          <p className="form-error" role="alert">
            {getPropertyErrorMessage(removeMutation.error)} 사진은 화면에서 제거하지 않았어요.
          </p>
        )}
      </ConfirmDialog>
    </li>
  );
};

export default PropertyPhotoCard;
