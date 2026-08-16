import { useRef, useState } from 'react';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { useRemovePropertyPhoto } from '../hooks/query/usePropertyMutations';
import type { PropertyPhoto } from '../types/Property';
import type { PublicConfig } from '../types/PublicConfig';
import AuthenticatedPhoto from './AuthenticatedPhoto';
import ConfirmDialog from './ConfirmDialog';
import dialogStyles from './ConfirmDialog.module.css';

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
    <li className="photo-grid__item">
      <AuthenticatedPhoto
        config={config}
        propertyId={propertyId}
        photoId={photo.photoId}
        contentUrl={photo.contentUrl}
        alt={accessibleName}
      />
      <button
        ref={deleteButtonRef}
        className="photo-delete-button"
        type="button"
        aria-label={`${accessibleName} 삭제`}
        onClick={() => setIsDialogOpen(true)}
      >
        삭제
      </button>
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
