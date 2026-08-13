import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';

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
    <header className="page-heading">
      {backTo !== undefined && (
        <Link className="back-link" to={backTo}>
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
