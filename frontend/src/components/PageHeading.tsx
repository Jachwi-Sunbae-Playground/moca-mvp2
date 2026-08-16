import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import styles from './PageHeading.module.css';

type PageHeadingProps = {
  title: string;
  description?: string;
  backTo?: string;
  backLabel?: string;
  focusOnMount?: boolean;
};

const PageHeading = ({ title, description, backTo, backLabel = '뒤로', focusOnMount = false }: PageHeadingProps) => {
  const headingRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (focusOnMount) headingRef.current?.focus();
  }, [focusOnMount]);

  return (
    <header className={styles.heading}>
      {backTo !== undefined && (
        <Link className={styles.backLink} to={backTo}>
          ← {backLabel}
        </Link>
      )}
      <h1 ref={headingRef} tabIndex={-1}>
        {title}
      </h1>
      {description !== undefined && <p>{description}</p>}
    </header>
  );
};

export default PageHeading;
