import { useEffect, useId, useRef } from 'react';
import type { ReactNode, RefObject } from 'react';
import styles from './ConfirmDialog.module.css';

type ConfirmDialogProps = {
  isOpen: boolean;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  isConfirming: boolean;
  returnFocusRef: RefObject<HTMLElement | null>;
  onCancel: () => void;
  onConfirm: () => void;
  onAfterClose?: () => void;
  children?: ReactNode;
  tone?: 'danger' | 'primary';
};

const ConfirmDialog = ({
  isOpen,
  title,
  description,
  confirmLabel,
  isConfirming,
  returnFocusRef,
  onCancel,
  onConfirm,
  onAfterClose,
  children,
  tone = 'danger',
}: ConfirmDialogProps) => {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const cancelButtonRef = useRef<HTMLButtonElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;

    if (dialog === null) {
      return;
    }

    if (isOpen && !dialog.open) {
      dialog.showModal();
      cancelButtonRef.current?.focus();
      return;
    }

    if (!isOpen && dialog.open) {
      dialog.close();
      returnFocusRef.current?.focus();
      onAfterClose?.();
    }
  }, [isOpen, onAfterClose, returnFocusRef]);

  return (
    <dialog
      ref={dialogRef}
      className={styles.dialog}
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      onCancel={(event) => {
        event.preventDefault();
        if (!isConfirming) onCancel();
      }}
      onKeyDown={(event) => {
        if (event.key !== 'Escape') return;

        event.preventDefault();
        if (!isConfirming) onCancel();
      }}
      onClose={() => {
        if (isOpen && !isConfirming) onCancel();
      }}
    >
      <h2 id={titleId}>{title}</h2>
      <div id={descriptionId} className={styles.description}>
        {description}
      </div>
      {children}
      <div className={styles.actions}>
        <button
          ref={cancelButtonRef}
          className="secondary-button"
          type="button"
          disabled={isConfirming}
          onClick={onCancel}
        >
          취소
        </button>
        <button
          className={tone === 'danger' ? 'danger-button' : 'primary-button'}
          type="button"
          disabled={isConfirming}
          onClick={onConfirm}
        >
          {isConfirming ? '처리 중…' : confirmLabel}
        </button>
      </div>
    </dialog>
  );
};

export default ConfirmDialog;
