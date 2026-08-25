declare const __API_BASE_URL__: string;
declare const __MAP_PROVIDER_MODE__: string;
declare const __KAKAO_MAP_JAVASCRIPT_KEY__: string;
declare const __ENABLE_MSW__: boolean;

type KakaoLatLng = { getLat: () => number; getLng: () => number };
type KakaoMap = {
  getCenter: () => KakaoLatLng;
  getLevel: () => number;
  setCenter: (center: KakaoLatLng) => void;
  setLevel: (level: number) => void;
};
type KakaoCustomOverlay = { setMap: (map: KakaoMap | null) => void };
type KakaoCircle = { setMap: (map: KakaoMap | null) => void };
type KakaoMapsNamespace = {
  load: (callback: () => void) => void;
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng;
  Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number }) => KakaoMap;
  CustomOverlay: new (options: {
    map: KakaoMap;
    position: KakaoLatLng;
    content: HTMLElement;
    xAnchor?: number;
    yAnchor?: number;
    zIndex?: number;
  }) => KakaoCustomOverlay;
  Circle: new (options: {
    map: KakaoMap;
    center: KakaoLatLng;
    radius: number;
    strokeWeight: number;
    strokeColor: string;
    strokeOpacity: number;
    fillColor: string;
    fillOpacity: number;
  }) => KakaoCircle;
  event: {
    addListener: (target: object, eventName: string, callback: (event: { latLng: KakaoLatLng }) => void) => void;
    removeListener: (target: object, eventName: string, callback: (event: { latLng: KakaoLatLng }) => void) => void;
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
