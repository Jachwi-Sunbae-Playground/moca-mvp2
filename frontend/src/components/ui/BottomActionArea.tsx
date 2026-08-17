import type { ReactNode } from 'react';
import styles from './BottomActionArea.module.css';

type BottomActionAreaProps = {
  children: ReactNode;
  sticky?: boolean;
  divider?: boolean;
};

const BottomActionArea = ({ children, sticky = true, divider = true }: BottomActionAreaProps) => (
  <div className={`${styles.actions} ${sticky ? styles.sticky : ''}`} data-divider={divider ? 'true' : 'false'}>
    {children}
  </div>
);

export default BottomActionArea;
