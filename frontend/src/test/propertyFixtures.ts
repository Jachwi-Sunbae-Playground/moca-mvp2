export const memberFixture = {
  id: 1,
  name: '이자취',
  email: 'jachwi@example.com',
};

export const recentVisitFixture = {
  visitId: 31,
  status: 'COMPLETED',
  startedAt: '2026-08-09T05:20:00Z',
  completedAt: '2026-08-09T06:00:00Z',
  summary: {
    totalCount: 22,
    checkedCount: 15,
    goodCount: 10,
    cautionCount: 5,
    unconfirmedCount: 7,
  },
};

export const propertySummaryFixture = {
  propertyId: 10,
  name: '신림역 원룸',
  depositAmount: 10_000_000,
  monthlyRentAmount: 550_000,
  maintenanceFeeAmount: 50_000,
  discoverySource: { type: 'URL', value: 'https://example.com/listings/10' },
  recentVisit: recentVisitFixture,
  photoCount: 2,
  representativePhoto: {
    photoId: 81,
    contentUrl: '/api/properties/10/photos/81/content',
    contentType: 'image/jpeg' as const,
  },
  progress: {
    totalCount: 22,
    completedCount: 15,
    goodCount: 10,
    cautionCount: 5,
    unconfirmedCount: 7,
    progressRate: 68,
  },
  lastActivityAt: '2026-08-10T07:30:00Z',
};

export const secondPropertySummaryFixture = {
  ...propertySummaryFixture,
  propertyId: 11,
  name: '망원동 투룸',
  discoverySource: { type: 'TEXT', value: '동네 중개사 추천' },
  recentVisit: null,
  photoCount: 0,
  representativePhoto: null,
  progress: {
    totalCount: 12,
    completedCount: 12,
    goodCount: 8,
    cautionCount: 4,
    unconfirmedCount: 0,
    progressRate: 100,
  },
};

export const propertyDetailFixture = {
  ...propertySummaryFixture,
  memo: {
    viewingSchedule: '8월 20일 오후 2시 방문',
    moveInAvailability: '9월 1일부터 입주 가능',
    provisionalDeposit: '가계약금 30만 원',
    roomOptions: '냉장고와 세탁기 포함',
    maintenanceAndUtilities: '관리비와 전기·가스 별도',
    commuteTime: '학교까지 버스로 20분',
    governmentSupport: '중소기업 청년 대출 가능 여부 확인',
    additionalMemo: '채광 다시 확인',
    content: '채광 다시 확인',
    savedAt: '2026-08-10T07:40:00Z',
  },
  activeChecklists: [{ stage: 'ON_SITE', checklistId: 7, name: '집에서 확인', itemCount: 12 }],
  photoPreview: {
    totalCount: 2,
    photos: [
      {
        photoId: 81,
        contentUrl: '/api/properties/10/photos/81/content',
        createdAt: '2026-08-10T07:35:00Z',
      },
    ],
  },
  deletionImpact: { visitCount: 2, photoCount: 2, activeChecklistCount: 1 },
  createdAt: '2026-08-10T07:30:00Z',
  updatedAt: '2026-08-10T07:40:00Z',
};

export const photoFixture = {
  photoId: 81,
  contentUrl: '/api/properties/10/photos/81/content',
  contentType: 'image/jpeg',
  sizeBytes: 245_760,
  createdAt: '2026-08-10T07:35:00Z',
};

export const successEnvelope = (data: unknown) => ({
  code: 'SUCCESS',
  message: '요청에 성공했습니다.',
  data,
});

export const errorEnvelope = (code: string, errors: Array<{ field?: string; reason?: string }> = []) => ({
  code,
  message: '서버 내부 상세 메시지',
  errors,
});

export const propertyPageFixture = (content: Array<Record<string, unknown>>) => ({
  totalCount: content.length,
  items: content.map((property) => {
    const representativePhoto = property.representativePhoto as
      { photoId: number; contentUrl: string; contentType: string } | null | undefined;

    return {
      id: property.propertyId,
      name: property.name,
      depositAmount: property.depositAmount,
      monthlyRentAmount: property.monthlyRentAmount,
      discoverySource:
        typeof property.discoverySource === 'object' && property.discoverySource !== null
          ? (property.discoverySource as { value: string }).value
          : property.discoverySource,
      representativePhoto:
        representativePhoto == null
          ? null
          : {
              id: representativePhoto.photoId,
              url: representativePhoto.contentUrl,
              contentType: representativePhoto.contentType,
            },
      progress: property.progress,
    };
  }),
});
