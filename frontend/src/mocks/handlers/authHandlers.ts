import { http } from 'msw';
import { failure, obsoleteEndpoint, success } from '../mockStore';

export const authHandlers = [
  http.post('*/api/auth/google', async ({ request }) => {
    const body = (await request.json()) as { authorizationCode?: unknown };
    if (typeof body.authorizationCode !== 'string' || body.authorizationCode.trim() === '') {
      return failure('INVALID_REQUEST', 400);
    }
    return success({
      accessToken: 'local-msw-access-token',
      tokenType: 'Bearer',
      expiresInSeconds: 28_800,
      isNewMember: false,
      member: { id: 1, name: '이자취', email: 'jachwi@example.com' },
    });
  }),
  http.get('*/api/members', () => success({ id: 1, name: '이자취', email: 'jachwi@example.com' })),
  http.get('*/api/members/me', obsoleteEndpoint),
];
