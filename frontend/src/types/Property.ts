import type { ChecklistStage } from './Checklist';

export type DiscoverySource = {
  type: 'URL' | 'TEXT';
  value: string;
};

export type PropertySummary = {
  propertyId: number;
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  discoverySource: DiscoverySource;
  representativePhoto: {
    photoId: number;
    contentUrl: string;
    contentType: 'image/jpeg' | 'image/png' | 'image/webp';
  } | null;
  progress: PropertyChecklistProgress;
  /** @deprecated 최종 목록 API는 전체 사진 수를 반환하지 않는다. */
  photoCount: number;
  /** @deprecated 최종 목록 API는 활동 시각을 반환하지 않는다. */
  lastActivityAt: string;
};

export type PropertyPage = {
  content: PropertySummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type PropertyPhotoPreview = {
  photoId: number;
  contentUrl: string;
  createdAt: string;
};

export type PropertyDetail = {
  propertyId: number;
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  maintenanceFeeAmount: number | null;
  discoverySource: DiscoverySource;
  photoPreview: {
    totalCount: number;
    photos: PropertyPhotoPreview[];
  };
  createdAt: string;
  updatedAt: string;
  lastActivityAt: string;
};

export type PropertyBasicInfo = {
  propertyId: number;
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  maintenanceFeeAmount: number | null;
  discoverySource: DiscoverySource;
  updatedAt: string | null;
};

export type PropertyPhoto = {
  photoId: number;
  contentUrl: string;
  contentType: 'image/jpeg' | 'image/png' | 'image/webp';
  sizeBytes: number;
  createdAt: string;
  representative?: boolean;
};

export type PropertyPhotoList = {
  photos: PropertyPhoto[];
  totalCount: number;
};

export type PropertyMemoItem = {
  propertyMemoItemId: number;
  systemMemoItemId: number;
  label: string;
  displayOrder: number;
  content: string;
};

export type PropertyMemoDocument = {
  propertyId: number;
  items: PropertyMemoItem[];
  freeMemo: string;
};

export type PropertyChecklistProgress = {
  totalCount: number;
  completedCount: number;
  goodCount: number;
  cautionCount: number;
  unconfirmedCount: number;
  progressRate: number;
};

export type PropertyChecklistStageSummary = {
  stage: ChecklistStage;
  applied: boolean;
  propertyChecklistId: number | null;
  checklistName: string | null;
  sourceChecklistId: number | null;
  progress: PropertyChecklistProgress;
};

export type PropertyChecklistOverview = {
  propertyId: number;
  overallProgress: PropertyChecklistProgress;
  stages: PropertyChecklistStageSummary[];
};

export type PropertyChecklistItemStatus = 'UNCONFIRMED' | 'GOOD' | 'CAUTION';

export type PropertyChecklistItem = {
  itemId: number;
  systemCheckItemId: number;
  question: string;
  displayOrder: number;
  status: PropertyChecklistItemStatus;
  memo: string;
};

export type PropertyChecklistDetail = {
  propertyChecklistId: number;
  propertyId: number;
  sourceChecklistId: number | null;
  checklistName: string;
  stage: ChecklistStage;
  items: PropertyChecklistItem[];
};
