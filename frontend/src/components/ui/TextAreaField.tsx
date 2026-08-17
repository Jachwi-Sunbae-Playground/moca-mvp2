import { forwardRef, useId } from 'react';
import type { TextareaHTMLAttributes } from 'react';
import styles from './TextField.module.css';

type TextAreaFieldProps = Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'children'> & {
  label: string;
  helpText?: string;
  error?: string;
  fieldClassName?: string;
};

const TextAreaField = forwardRef<HTMLTextAreaElement, TextAreaFieldProps>(
  ({ label, helpText, error, id, className, fieldClassName, ...textareaProps }, ref) => {
    const generatedId = useId();
    const textareaId = id ?? generatedId;
    const helpId = helpText === undefined ? undefined : `${textareaId}-help`;
    const errorId = error === undefined ? undefined : `${textareaId}-error`;
    const describedBy = [helpId, errorId].filter(Boolean).join(' ') || undefined;

    return (
      <div className={`${styles.field} ${fieldClassName ?? ''}`}>
        <label htmlFor={textareaId}>{label}</label>
        <div className={styles.control}>
          <textarea
            {...textareaProps}
            ref={ref}
            id={textareaId}
            className={className}
            aria-invalid={error === undefined ? undefined : true}
            aria-describedby={describedBy}
          />
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
  },
);

TextAreaField.displayName = 'TextAreaField';

export default TextAreaField;
