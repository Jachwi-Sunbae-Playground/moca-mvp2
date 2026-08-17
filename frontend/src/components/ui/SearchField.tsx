import { useId } from 'react';
import type { FormEvent } from 'react';
import Icon from './Icon';
import styles from './SearchField.module.css';

type SearchFieldProps = {
  label: string;
  value: string;
  placeholder?: string;
  maxLength?: number;
  disabled?: boolean;
  onValueChange: (value: string) => void;
  onSubmit: () => void;
  onClear?: () => void;
  showSubmitButton?: boolean;
};

const SearchField = ({
  label,
  value,
  placeholder,
  maxLength,
  disabled = false,
  onValueChange,
  onSubmit,
  onClear,
  showSubmitButton = true,
}: SearchFieldProps) => {
  const inputId = useId();

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit();
  };

  const clear = () => {
    onValueChange('');
    onClear?.();
  };

  return (
    <form
      className={`${styles.search} ${showSubmitButton ? '' : styles.withoutSubmit}`}
      role="search"
      onSubmit={submit}
    >
      <label className={styles.srOnly} htmlFor={inputId}>
        {label}
      </label>
      <Icon name="search" size={18} className={styles.searchIcon} />
      <input
        id={inputId}
        value={value}
        maxLength={maxLength}
        disabled={disabled}
        placeholder={placeholder}
        enterKeyHint="search"
        onChange={(event) => onValueChange(event.target.value)}
      />
      {value.length > 0 && (
        <button className={styles.clear} type="button" aria-label="검색어 지우기" disabled={disabled} onClick={clear}>
          <Icon name="close" size={17} />
        </button>
      )}
      {showSubmitButton && (
        <button className={styles.submit} type="submit" disabled={disabled}>
          검색
        </button>
      )}
    </form>
  );
};

export default SearchField;
