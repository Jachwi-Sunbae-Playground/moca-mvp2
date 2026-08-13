import type { ChecklistStage } from './Checklist';
import type { VisitStatus, VisitSummary } from './Visit';

export type DiscoverySource = {
  type: 'URL' | 'TEXT';
  value: string;
};

export type RecentVisit = {
  visitId: number;
  status: VisitStatus;
  startedAt: string;
  completedAt: string | null;
  summary: VisitSummary;
};

export type PropertySummary = {
  propertyId: number;
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  discoverySource: DiscoverySource;
  recentVisit: RecentVisit | null;
  photoCount: number;
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

export type PropertyPreVisitMemo = {
  viewingSchedule: string;
  moveInAvailability: string;
  provisionalDeposit: string;
  roomOptions: string;
  maintenanceAndUtilities: string;
  commuteTime: string;
  governmentSupport: string;
  additionalMemo: string;
  savedAt: string | null;
};

/** @deprecated content는 v1.0 화면 호환 별칭이다. 신규 코드는 additionalMemo를 사용한다. */
export type PropertyMemo = PropertyPreVisitMemo & {
  content: string;
};

export type PropertyActiveChecklist = {
  stage: ChecklistStage;
  checklistId: number;
  name: string;
  itemCount: number;
};

export type PropertyPhotoPreview = {
  photoId: number;
  contentUrl: string;
  createdAt: string;
};

export type DeletionImpact = {
  visitCount: number;
  photoCount: number;
  activeChecklistCount: number;
};

export type PropertyDetail = {
  propertyId: number;
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  discoverySource: DiscoverySource;
  memo: PropertyMemo;
  activeChecklists: PropertyActiveChecklist[];
  recentVisit: RecentVisit | null;
  photoPreview: {
    totalCount: number;
    photos: PropertyPhotoPreview[];
  };
  deletionImpact: DeletionImpact;
  createdAt: string;
  updatedAt: string;
  lastActivityAt: string;
};

export type PropertyBasicInfo = {
  propertyId: number;
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  discoverySource: DiscoverySource;
  updatedAt: string;
};

export type PropertyPhoto = {
  photoId: number;
  contentUrl: string;
  contentType: 'image/jpeg' | 'image/png' | 'image/webp';
  sizeBytes: number;
  createdAt: string;
};

export type PropertyPhotoList = {
  photos: PropertyPhoto[];
  totalCount: number;
};
