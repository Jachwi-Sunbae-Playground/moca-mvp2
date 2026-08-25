import { http } from 'msw';
import { failure, success } from '../mockStore';

export const authHandlers = [
  http.post('*/api/auth/nickname', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    if (typeof body.nickname !== 'string' || body.nickname.trim() === '') {
      return failure('INVALID_REQUEST', 400);
    }
    return success({
      accessToken: 'local-msw-access-token',
      tokenType: 'Bearer',
      expiresIn: 28_800,
      member: { memberId: 1, name: body.nickname.trim(), passwordProtected: typeof body.password === 'string' },
    });
  }),
  http.get('*/api/members/me', () => success({ id: 1, name: '이자취', passwordProtected: false })),
];
