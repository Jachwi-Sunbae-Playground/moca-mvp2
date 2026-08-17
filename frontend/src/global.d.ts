declare const __API_BASE_URL__: string;
declare const __GOOGLE_CLIENT_ID__: string;
declare const __GOOGLE_REDIRECT_URI__: string;
declare const __ENABLE_MSW__: boolean;

declare module '*.svg' {
  const source: string;
  export default source;
}

declare module '*.png' {
  const source: string;
  export default source;
}

declare module '*.jpg' {
  const source: string;
  export default source;
}

declare module '*.module.css' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}

declare module '*.css';
