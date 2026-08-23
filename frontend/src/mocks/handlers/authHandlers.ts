import { http } from 'msw';
import { failure, success } from '../mockStore';

export const authHandlers = [
  http.post('*/api/auth/demo', () =>
    success({
      accessToken: 'local-msw-demo-access-token',
      tokenType: 'Bearer',
      expiresIn: 28_800,
      member: { memberId: 1, name: '모카 데모', email: 'demo@moca.local' },
    }),
  ),
  http.post('*/api/auth/google', async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    if (
      typeof body.authorizationCode !== 'string' ||
      body.authorizationCode.trim() === '' ||
      typeof body.codeVerifier !== 'string' ||
      body.codeVerifier.trim() === '' ||
      typeof body.nonce !== 'string' ||
      body.nonce.trim() === '' ||
      typeof body.redirectUri !== 'string' ||
      body.redirectUri.trim() === ''
    ) {
      return failure('INVALID_REQUEST', 400);
    }
    return success({
      accessToken: 'local-msw-access-token',
      tokenType: 'Bearer',
      expiresIn: 28_800,
      member: { memberId: 1, name: '이자취', email: 'jachwi@example.com' },
    });
  }),
  http.get('*/api/members/me', () => success({ id: 1, name: '이자취', email: 'jachwi@example.com' })),
];
