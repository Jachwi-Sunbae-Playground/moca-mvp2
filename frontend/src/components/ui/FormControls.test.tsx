import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { Button, ButtonLink } from './Button';
import SearchField from './SearchField';
import TextAreaField from './TextAreaField';
import TextField from './TextField';

describe('공용 폼 컨트롤', () => {
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

  it('ButtonLink는 이동 목적지를 명시한다', () => {
    render(
      <MemoryRouter>
        <ButtonLink to="/properties/new">새 매물 등록</ButtonLink>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: '새 매물 등록' })).toHaveAttribute('href', '/properties/new');
  });

  it('TextField는 라벨과 도움말·오류를 입력 요소에 연결한다', () => {
    render(<TextField label="이름" helpText="구분하기 쉬운 이름" error="이름을 입력해 주세요." />);

    const input = screen.getByRole('textbox', { name: '이름' });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('구분하기 쉬운 이름 이름을 입력해 주세요.');
  });

  it('TextAreaField는 라벨과 오류를 입력 요소에 연결한다', () => {
    render(<TextAreaField label="메모" error="메모를 확인해 주세요." />);

    const textarea = screen.getByRole('textbox', { name: '메모' });
    expect(textarea).toHaveAttribute('aria-invalid', 'true');
    expect(textarea).toHaveAccessibleDescription('메모를 확인해 주세요.');
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
});
