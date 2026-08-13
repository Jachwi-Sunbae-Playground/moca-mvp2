type FeatureIconProps = {
  name: 'check' | 'compare' | 'archive';
};

const FeatureIcon = ({ name }: FeatureIconProps) => {
  if (name === 'check') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <rect x="5" y="4" width="14" height="17" rx="2" />
        <path d="m8 12 2.5 2.5L16 9" />
        <path d="M9 4V2.5h6V4" />
      </svg>
    );
  }

  if (name === 'compare') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 7h16M4 7l4-4M4 7l4 4M20 17H4m16 0-4-4m4 4-4 4" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M7 18h10a4 4 0 0 0 .5-8A6 6 0 0 0 6 8.5 4.8 4.8 0 0 0 7 18Z" />
    </svg>
  );
};

export default FeatureIcon;
