import { useId } from 'react';
import type { InputHTMLAttributes, ReactNode } from 'react';
import styles from './TextField.module.css';

type TextFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'children'> & {
  label: string;
  helpText?: string;
  error?: string;
  suffix?: ReactNode;
  fieldClassName?: string;
};

const TextField = ({
  label,
  helpText,
  error,
  suffix,
  id,
  className,
  fieldClassName,
  ...inputProps
}: TextFieldProps) => {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const helpId = helpText === undefined ? undefined : `${inputId}-help`;
  const errorId = error === undefined ? undefined : `${inputId}-error`;
  const describedBy = [helpId, errorId].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`${styles.field} ${fieldClassName ?? ''}`}>
      <label htmlFor={inputId}>{label}</label>
      <div className={styles.control}>
        <input
          {...inputProps}
          id={inputId}
          className={className}
          aria-invalid={error === undefined ? undefined : true}
          aria-describedby={describedBy}
        />
        {suffix !== undefined && <span className={styles.suffix}>{suffix}</span>}
      </div>
      {helpText !== undefined && (
        <p id={helpId} className={styles.help}>
          {helpText}
        </p>
      )}
      {error !== undefined && (
        <p id={errorId} className={styles.error}>
          {error}
        </p>
      )}
    </div>
  );
};

export default TextField;
