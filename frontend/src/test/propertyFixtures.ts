export const memberFixture = {
  id: 1,
  name: '이자취',
  email: 'jachwi@example.com',
};

export const propertySummaryFixture = {
  propertyId: 10,
  name: '신림역 원룸',
  depositAmount: 10_000_000,
  monthlyRentAmount: 550_000,
  maintenanceFeeAmount: 50_000,
  discoverySource: { type: 'URL', value: 'https://example.com/listings/10' },
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
      representativePhoto:
        representativePhoto == null
          ? null
          : {
              id: representativePhoto.photoId,
              url: representativePhoto.contentUrl,
              contentType: representativePhoto.contentType,
            },
      overallProgress: property.progress,
    };
  }),
});
