import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AppBar from './AppBar';
import { Button, ButtonLink } from './Button';
import TopNavigation from './TopNavigation';
import SearchField from './SearchField';
import TextField from './TextField';

describe('공용 UI 컴포넌트', () => {
  it('Button은 처리 중일 때 중복 동작을 막고 진행 상태를 표시한다', () => {
    render(
      <Button isLoading loadingLabel="저장 중…">
        저장
      </Button>,
    );

    const button = screen.getByRole('button', { name: '저장 중…' });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
  });

  it('TextField는 라벨과 도움말·오류를 입력 요소에 연결한다', () => {
    render(<TextField label="이름" helpText="구분하기 쉬운 이름" error="이름을 입력해 주세요." />);

    const input = screen.getByRole('textbox', { name: '이름' });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('구분하기 쉬운 이름 이름을 입력해 주세요.');
  });

  it('SearchField는 검색어 제출과 초기화를 부모에게 전달한다', async () => {
    const submit = vi.fn();
    const clear = vi.fn();
    const user = userEvent.setup();

    const SearchExample = () => {
      const [value, setValue] = useState('신림');
      return (
        <SearchField label="매물 이름 검색" value={value} onValueChange={setValue} onSubmit={submit} onClear={clear} />
      );
    };

    render(<SearchExample />);
    await user.click(screen.getByRole('button', { name: '검색' }));
    expect(submit).toHaveBeenCalledOnce();

    await user.click(screen.getByRole('button', { name: '검색어 지우기' }));
    expect(screen.getByRole('textbox', { name: '매물 이름 검색' })).toHaveValue('');
    expect(clear).toHaveBeenCalledOnce();
  });

  it('AppBar와 ButtonLink는 이동 목적지를 명시한다', () => {
    render(
      <MemoryRouter>
        <AppBar title="새 매물 등록" backTo="/properties" backLabel="매물 목록으로 돌아가기" />
        <ButtonLink to="/properties/new">새 매물 등록</ButtonLink>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: '매물 목록으로 돌아가기' })).toHaveAttribute('href', '/properties');
    expect(screen.getByRole('link', { name: '새 매물 등록' })).toHaveAttribute('href', '/properties/new');
  });

  it('TopNavigation은 제목·보조정보·뒤로가기·우측 슬롯을 함께 표시한다', () => {
    render(
      <MemoryRouter>
        <TopNavigation
          title="매물 정보"
          meta="2개"
          backTo="/properties"
          backLabel="매물 목록으로 돌아가기"
          endSlot={<button type="button">메뉴</button>}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText('매물 정보')).toBeInTheDocument();
    expect(screen.getByText('2개')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '매물 목록으로 돌아가기' })).toHaveAttribute('href', '/properties');
    expect(screen.getByRole('button', { name: '메뉴' })).toBeInTheDocument();
  });
});
