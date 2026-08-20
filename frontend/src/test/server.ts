import { HttpResponse, http } from 'msw';
import { setupServer } from 'msw/node';
import { visitDetailFixture } from './visitFixtures';

const success = (data: unknown) => HttpResponse.json({ code: 'SUCCESS', message: '요청에 성공했습니다.', data });

export const server = setupServer(
  http.get('*/api/members', () => success({ id: 1, name: '이자취', email: 'jachwi@example.com' })),
  http.get('*/api/checklists', () => success({ totalCount: 0, items: [] })),
  http.get('*/api/visits/:visitId', () => success(visitDetailFixture)),
  http.get('*/api/properties/:propertyId/memo', ({ params }) =>
    success({
      propertyId: Number(params.propertyId),
      items: [
        { systemMemoItemId: 1, label: '집 주소', displayOrder: 1, content: '' },
        { systemMemoItemId: 2, label: '입주 가능일', displayOrder: 2, content: '' },
        { systemMemoItemId: 3, label: '가계약금', displayOrder: 3, content: '' },
        { systemMemoItemId: 4, label: '방 옵션', displayOrder: 4, content: '' },
        { systemMemoItemId: 5, label: '관리비 및 공과금', displayOrder: 5, content: '' },
        { systemMemoItemId: 6, label: '통학 통근 시간', displayOrder: 6, content: '' },
      ],
      freeMemo: '',
    }),
  ),
  http.get('*/api/properties/:propertyId/checklists', ({ params }) =>
    success({
      propertyId: Number(params.propertyId),
      overallProgress: {
        totalCount: 0,
        completedCount: 0,
        goodCount: 0,
        cautionCount: 0,
        unconfirmedCount: 0,
        progressRate: 0,
      },
      stages: ['ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT'].map((stage) => ({
        stage,
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
      })),
    }),
  ),
  http.get(
    '*/api/properties/:propertyId/photos/:photoId/content',
    () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
  ),
);
