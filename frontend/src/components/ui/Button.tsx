import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Link } from 'react-router-dom';
import type { LinkProps } from 'react-router-dom';
import styles from './Button.module.css';

export type ButtonVariant = 'primary' | 'soft' | 'secondary' | 'neutral' | 'danger' | 'text';

type SharedButtonProps = {
  children: ReactNode;
  variant?: ButtonVariant;
  fullWidth?: boolean;
  className?: string;
};

type ButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> &
  SharedButtonProps & {
    isLoading?: boolean;
    loadingLabel?: string;
  };

type ButtonLinkProps = Omit<LinkProps, 'children'> & SharedButtonProps;

const buttonClassName = (variant: ButtonVariant, fullWidth: boolean, className: string | undefined) =>
  [styles.button, styles[variant], fullWidth ? styles.fullWidth : '', className ?? ''].filter(Boolean).join(' ');

export const Button = ({
  children,
  variant = 'primary',
  fullWidth = false,
  className,
  isLoading = false,
  loadingLabel = '처리 중…',
  disabled,
  type = 'button',
  ...buttonProps
}: ButtonProps) => (
  <button
    {...buttonProps}
    className={buttonClassName(variant, fullWidth, className)}
    type={type}
    disabled={disabled === true || isLoading}
    aria-busy={isLoading || undefined}
  >
    {isLoading && <span className={styles.spinner} aria-hidden="true" />}
    <span>{isLoading ? loadingLabel : children}</span>
  </button>
);

export const ButtonLink = ({
  children,
  variant = 'primary',
  fullWidth = false,
  className,
  ...linkProps
}: ButtonLinkProps) => (
  <Link {...linkProps} className={buttonClassName(variant, fullWidth, className)}>
    {children}
  </Link>
);
