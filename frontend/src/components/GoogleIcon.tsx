type GoogleIconProps = {
  className?: string;
};

const GoogleIcon = ({ className }: GoogleIconProps) => (
  <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
    <path
      fill="#4285f4"
      d="M21.6 12.2c0-.7-.1-1.4-.2-2H12v3.9h5.4a4.7 4.7 0 0 1-2 3v2.6h3.3c1.9-1.8 2.9-4.4 2.9-7.5Z"
    />
    <path
      fill="#34a853"
      d="M12 22c2.7 0 5-.9 6.7-2.3l-3.3-2.6c-.9.6-2.1 1-3.4 1a5.9 5.9 0 0 1-5.5-4.1H3.1v2.6A10.1 10.1 0 0 0 12 22Z"
    />
    <path fill="#fbbc05" d="M6.5 14a6.1 6.1 0 0 1 0-3.9V7.4H3.1a10.1 10.1 0 0 0 0 9.2L6.5 14Z" />
    <path fill="#ea4335" d="M12 6c1.5 0 2.9.6 4 1.6l3-3A10 10 0 0 0 3.1 7.4l3.4 2.7A5.9 5.9 0 0 1 12 6Z" />
  </svg>
);

export default GoogleIcon;
