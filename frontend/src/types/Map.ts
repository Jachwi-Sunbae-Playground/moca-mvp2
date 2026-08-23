export type MapCategory = 'HOSPITAL' | 'TRANSPORT' | 'SCHOOL' | 'CONVENIENCE' | 'AGENCY';

export type MapAddress = {
  address: string | null;
  roadAddress: string | null;
  jibunAddress: string | null;
  latitude: number;
  longitude: number;
};

export type NearbyPlace = {
  providerPlaceId: string;
  name: string;
  category: MapCategory;
  address: string;
  latitude: number;
  longitude: number;
  distanceMeters: number;
};

export type NearbyResult = {
  center: { latitude: number; longitude: number };
  radius: 500 | 1000 | 2000;
  counts: Record<MapCategory, number>;
  places: NearbyPlace[];
};
