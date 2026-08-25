import { QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { setAuthentication } from '../app/authStore';
import { queryClient } from '../app/queryClient';
import { server } from '../test/server';
import type { PublicConfig } from '../types/PublicConfig';
import AuthenticatedPhoto from './AuthenticatedPhoto';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
};

describe('인증 사진 표시', () => {
  it('Bearer로 Blob을 읽어 Object URL로 표시하고 unmount에서 해제한다', async () => {
    setAuthentication({ accessToken: 'photo-token', tokenType: 'Bearer', expiresIn: 60 });
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/photos/81/content`, ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer photo-token');
        return new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } });
      }),
    );

    const rendered = render(
      <QueryClientProvider client={queryClient}>
        <AuthenticatedPhoto
          config={config}
          propertyId={10}
          photoId={81}
          contentUrl="/api/properties/10/photos/81/content"
          alt="업로드 순 1번째 사진"
        />
      </QueryClientProvider>,
    );

    const image = await screen.findByRole('img', { name: '업로드 순 1번째 사진' });
    expect(image).toHaveAttribute('src', expect.stringMatching(/^blob:test-photo-/));
    expect(document.body.innerHTML).not.toContain('photo-token');
    expect(document.body.innerHTML).not.toContain('/api/properties/10/photos/81/content');

    rendered.unmount();
    await waitFor(() => expect(URL.revokeObjectURL).toHaveBeenCalled());
    expect(vi.mocked(URL.createObjectURL)).toHaveBeenCalledOnce();
  });

  it('사진 읽기 실패를 접근 가능한 대체 상태와 재시도로 표시한다', async () => {
    setAuthentication({ accessToken: 'photo-token', tokenType: 'Bearer', expiresIn: 60 });
    let attempts = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/photos/81/content`, () => {
        attempts += 1;
        return attempts === 1
          ? HttpResponse.json({ code: 'PHOTO_READ_FAILED', message: 'internal', errors: [] }, { status: 500 })
          : new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } });
      }),
    );

    render(
      <QueryClientProvider client={queryClient}>
        <AuthenticatedPhoto
          config={config}
          propertyId={10}
          photoId={81}
          contentUrl="/api/properties/10/photos/81/content"
          alt="업로드 순 1번째 사진"
        />
      </QueryClientProvider>,
    );

    expect(await screen.findByRole('group', { name: '업로드 순 1번째 사진 불러오기 실패' })).toBeInTheDocument();
    screen.getByRole('button', { name: '다시 시도' }).click();
    expect(await screen.findByRole('img', { name: '업로드 순 1번째 사진' })).toBeInTheDocument();
  });
});
