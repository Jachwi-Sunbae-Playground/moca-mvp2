import { useRef, useState } from 'react';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { useUploadPropertyPhoto } from './query/usePropertyMutations';
import type { PublicConfig } from '../types/PublicConfig';

export const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
export const MAX_PROPERTY_PHOTOS = 30;
export const acceptedPhotoTypes = ['image/jpeg', 'image/png', 'image/webp'] as const;

type UploadStatus = 'queued' | 'uploading' | 'success' | 'error';

export type PhotoUploadItem = {
  id: string;
  label: string;
  status: UploadStatus;
  message: string;
};

type PreparedFile = {
  item: PhotoUploadItem;
  file: File | null;
};

const prepareFiles = (files: File[], availableCount: number): PreparedFile[] =>
  files.map((file, index) => {
    const item = {
      id: `${Date.now()}-${index}`,
      label: `선택한 사진 ${index + 1}`,
      status: 'queued' as const,
      message: '업로드 대기 중',
    };

    if (index >= availableCount) {
      return { item: { ...item, status: 'error', message: '매물당 최대 30장까지만 등록할 수 있습니다.' }, file: null };
    }

    if (!acceptedPhotoTypes.some((type) => type === file.type)) {
      return { item: { ...item, status: 'error', message: 'JPEG, PNG 또는 WebP 사진을 선택해 주세요.' }, file: null };
    }

    if (file.size === 0) {
      return { item: { ...item, status: 'error', message: '내용이 없는 파일은 등록할 수 없습니다.' }, file: null };
    }

    if (file.size > MAX_PHOTO_BYTES) {
      return {
        item: { ...item, status: 'error', message: '사진 한 장은 10MiB 이하만 등록할 수 있습니다.' },
        file: null,
      };
    }

    return { item, file };
  });

export const usePhotoUploadQueue = (config: PublicConfig, propertyId: number, currentPhotoCount: number) => {
  const uploadMutation = useUploadPropertyPhoto(config, propertyId);
  const [items, setItems] = useState<PhotoUploadItem[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const uploadLockRef = useRef(false);

  const updateItem = (id: string, status: UploadStatus, message: string) => {
    setItems((current) => current.map((item) => (item.id === id ? { ...item, status, message } : item)));
  };

  const uploadFiles = async (files: File[]) => {
    if (uploadLockRef.current || files.length === 0) {
      return;
    }

    uploadLockRef.current = true;
    setIsUploading(true);
    const prepared = prepareFiles(files, Math.max(0, MAX_PROPERTY_PHOTOS - currentPhotoCount));
    setItems(prepared.map(({ item }) => item));

    for (const { item, file } of prepared) {
      if (file === null) {
        continue;
      }

      updateItem(item.id, 'uploading', '업로드 중');

      try {
        await uploadMutation.mutateAsync(file);
        updateItem(item.id, 'success', '업로드 완료');
      } catch (error) {
        updateItem(item.id, 'error', getPropertyErrorMessage(error));
      }
    }

    uploadLockRef.current = false;
    setIsUploading(false);
  };

  return { items, isUploading, uploadFiles };
};
