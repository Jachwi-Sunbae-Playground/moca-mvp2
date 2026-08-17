import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react';
import { Link } from 'react-router-dom';
import type { LinkProps } from 'react-router-dom';
import styles from './Tabs.module.css';

type TabsProps = HTMLAttributes<HTMLDivElement> & {
  children: ReactNode;
};

type TabListProps = HTMLAttributes<HTMLElement> & {
  children: ReactNode;
  label: string;
};

type TabProps = Omit<LinkProps, 'children'> & {
  children: ReactNode;
  selected?: boolean;
};

type TabButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  children: ReactNode;
  selected?: boolean;
};

export const Tabs = ({ children, className, ...props }: TabsProps) => (
  <div {...props} className={`${styles.tabs} ${className ?? ''}`}>
    {children}
  </div>
);

export const TabList = ({ children, label, className, ...props }: TabListProps) => (
  <nav {...props} className={`${styles.tabList} ${className ?? ''}`} aria-label={label}>
    {children}
  </nav>
);

export const Tab = ({ children, selected = false, className, ...props }: TabProps) => (
  <Link {...props} className={`${styles.tab} ${className ?? ''}`} aria-current={selected ? 'page' : undefined}>
    {children}
  </Link>
);

export const TabButton = ({ children, selected = false, className, type = 'button', ...props }: TabButtonProps) => (
  <button {...props} type={type} role="tab" className={`${styles.tab} ${className ?? ''}`} aria-selected={selected}>
    {children}
  </button>
);
