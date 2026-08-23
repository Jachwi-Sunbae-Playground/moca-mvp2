declare const __API_BASE_URL__: string;
declare const __GOOGLE_CLIENT_ID__: string;
declare const __GOOGLE_REDIRECT_URI__: string;
declare const __AUTH_MODE__: string;
declare const __MAP_PROVIDER_MODE__: string;
declare const __KAKAO_MAP_JAVASCRIPT_KEY__: string;
declare const __ENABLE_MSW__: boolean;

type KakaoLatLng = { getLat: () => number; getLng: () => number };
type KakaoMapsNamespace = {
  load: (callback: () => void) => void;
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng;
  Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number }) => object;
  Marker: new (options: { map: object; position: KakaoLatLng; title: string }) => object;
  event: {
    addListener: (target: object, eventName: string, callback: (event: { latLng: KakaoLatLng }) => void) => void;
  };
};

interface Window {
  kakao?: { maps: KakaoMapsNamespace };
}

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
