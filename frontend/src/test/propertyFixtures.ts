export const memberFixture = {
  id: 1,
  name: '이자취',
  passwordProtected: false,
};

export const propertySummaryFixture = {
  propertyId: 10,
  name: '신림역 원룸',
  depositAmount: 10_000_000,
  monthlyRentAmount: 550_000,
  discoverySource: { type: 'URL', value: 'https://example.com/listings/10' },
  location: {
    address: '서울 관악구 신림로 12',
    roadAddress: '서울 관악구 신림로 12',
    jibunAddress: '서울 관악구 신림동 10-1',
    latitude: 37.484,
    longitude: 126.929,
  },
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
  stages: [
    {
      stage: 'ONLINE_PHONE' as const,
      applied: true,
      propertyChecklistId: 71,
      checklistName: '온라인 확인 기본',
      sourceChecklistId: 7,
      progress: {
        totalCount: 10,
        completedCount: 7,
        goodCount: 5,
        cautionCount: 2,
        unconfirmedCount: 3,
        progressRate: 70,
      },
    },
    {
      stage: 'ON_SITE' as const,
      applied: true,
      propertyChecklistId: 72,
      checklistName: '현장 확인 기본',
      sourceChecklistId: 8,
      progress: {
        totalCount: 12,
        completedCount: 8,
        goodCount: 5,
        cautionCount: 3,
        unconfirmedCount: 4,
        progressRate: 66,
      },
    },
    {
      stage: 'PRE_CONTRACT' as const,
      applied: false,
      propertyChecklistId: null,
      checklistName: null,
      sourceChecklistId: null,
      progress: {
        totalCount: 0,
        completedCount: 0,
        goodCount: 0,
        cautionCount: 0,
        unconfirmedCount: 0,
        progressRate: 0,
      },
    },
  ],
  lastActivityAt: '2026-08-10T07:30:00Z',
};

export const secondPropertySummaryFixture = {
  ...propertySummaryFixture,
  propertyId: 11,
  name: '망원동 투룸',
  discoverySource: { type: 'TEXT', value: '동네 중개사 추천' },
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
  stages: [
    {
      ...propertySummaryFixture.stages[0],
      progress: {
        totalCount: 12,
        completedCount: 12,
        goodCount: 8,
        cautionCount: 4,
        unconfirmedCount: 0,
        progressRate: 100,
      },
    },
    {
      ...propertySummaryFixture.stages[1],
      applied: false,
      propertyChecklistId: null,
      checklistName: null,
      sourceChecklistId: null,
      progress: propertySummaryFixture.stages[2].progress,
    },
    propertySummaryFixture.stages[2],
  ],
};

export const propertyDetailFixture = {
  ...propertySummaryFixture,
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

type PropertyPhotoFixtureInput = typeof photoFixture & { representative?: boolean };

export const propertyPhotoResponseFixture = (photo: PropertyPhotoFixtureInput = photoFixture) => ({
  id: photo.photoId,
  propertyId: 10,
  url: photo.contentUrl,
  contentType: photo.contentType,
  sizeBytes: photo.sizeBytes,
  representative: 'representative' in photo ? Boolean(photo.representative) : false,
  createdAt: photo.createdAt,
});

type PropertyDetailFixtureInput = Omit<typeof propertyDetailFixture, 'photoPreview'> & {
  photoPreview: {
    totalCount: number;
    photos: typeof propertyDetailFixture.photoPreview.photos;
  };
};

export const propertyDetailResponseFixture = (property: PropertyDetailFixtureInput = propertyDetailFixture) => ({
  id: property.propertyId,
  name: property.name,
  depositAmount: property.depositAmount,
  monthlyRentAmount: property.monthlyRentAmount,
  discoverySource: property.discoverySource.value,
  photos: property.photoPreview.photos.map((photo) => ({
    id: photo.photoId,
    url: photo.contentUrl,
    contentType: 'image/jpeg',
    sizeBytes: 245_760,
    representative: photo.photoId === property.representativePhoto?.photoId,
    createdAt: photo.createdAt,
  })),
  overallProgress: property.progress,
});

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
      address:
        typeof property.location === 'object' && property.location !== null
          ? (property.location as { address?: string | null }).address
          : null,
      roadAddress:
        typeof property.location === 'object' && property.location !== null
          ? (property.location as { roadAddress?: string | null }).roadAddress
          : null,
      jibunAddress:
        typeof property.location === 'object' && property.location !== null
          ? (property.location as { jibunAddress?: string | null }).jibunAddress
          : null,
      latitude:
        typeof property.location === 'object' && property.location !== null
          ? (property.location as { latitude?: number | null }).latitude
          : null,
      longitude:
        typeof property.location === 'object' && property.location !== null
          ? (property.location as { longitude?: number | null }).longitude
          : null,
      representativePhoto:
        representativePhoto == null
          ? null
          : {
              id: representativePhoto.photoId,
              url: representativePhoto.contentUrl,
              contentType: representativePhoto.contentType,
            },
      overallProgress: property.progress,
      stages: property.stages,
      photoCount: property.photoCount,
      lastActivityAt: property.lastActivityAt,
    };
  }),
});
