import { render, screen } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import PropertyAppLayout from './PropertyAppLayout';

describe('모바일 앱 셸', () => {
  it('현재 화면과 주요 메뉴 네 개를 함께 표시한다', () => {
    render(
      <MemoryRouter initialEntries={['/properties']}>
        <Routes>
          <Route element={<Outlet context={{ memberId: 1, displayName: '이자취', passwordProtected: false }} />}>
            <Route element={<PropertyAppLayout />}>
              <Route path="/properties" element={<h1>매물 목록</h1>} />
            </Route>
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: '매물 목록' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '홈' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: '체크리스트' })).toHaveAttribute('href', '/checklists');
    expect(screen.getByRole('link', { name: '마이' })).toHaveAttribute('href', '/me');
  });
});
