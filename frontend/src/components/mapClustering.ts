import type { NearbyPlace } from '../types/Map';
import { getMapCategoryLabel } from './mapPresentation';
import type { MapMarker } from './MapCanvas';

type PlaceCluster = {
  latitude: number;
  longitude: number;
  places: NearbyPlace[];
};

const EARTH_METERS_PER_DEGREE = 111_320;

const clusterDistanceForLevel = (level: number): number => {
  if (level <= 3) return 0;
  if (level === 4) return 55;
  if (level === 5) return 110;
  if (level === 6) return 220;
  return 400 * 2 ** Math.max(0, level - 7);
};

const distanceMeters = (
  left: { latitude: number; longitude: number },
  right: { latitude: number; longitude: number },
): number => {
  const averageLatitude = ((left.latitude + right.latitude) * Math.PI) / 360;
  const latitudeMeters = (left.latitude - right.latitude) * EARTH_METERS_PER_DEGREE;
  const longitudeMeters = (left.longitude - right.longitude) * EARTH_METERS_PER_DEGREE * Math.cos(averageLatitude);
  return Math.hypot(latitudeMeters, longitudeMeters);
};

const addPlace = (cluster: PlaceCluster, place: NearbyPlace) => {
  const count = cluster.places.length;
  cluster.latitude = (cluster.latitude * count + place.latitude) / (count + 1);
  cluster.longitude = (cluster.longitude * count + place.longitude) / (count + 1);
  cluster.places.push(place);
};

const markerFromCluster = (cluster: PlaceCluster): MapMarker => {
  if (cluster.places.length === 1) {
    const [place] = cluster.places;
    return {
      id: `place-${place.providerPlaceId}`,
      latitude: place.latitude,
      longitude: place.longitude,
      label: place.name,
      tone: 'place',
      category: place.category,
      placeId: place.providerPlaceId,
      actionable: true,
    };
  }

  const categories = new Set(cluster.places.map((place) => place.category));
  const category = categories.size === 1 ? cluster.places[0].category : undefined;
  const label =
    category === undefined
      ? `${cluster.places.length}개 시설`
      : `${getMapCategoryLabel(category)} ${cluster.places.length}개`;
  return {
    id: `cluster-${cluster.places
      .map((place) => place.providerPlaceId)
      .sort()
      .join('-')}`,
    latitude: cluster.latitude,
    longitude: cluster.longitude,
    label,
    tone: 'cluster',
    category,
    count: cluster.places.length,
    actionable: true,
  };
};

export const clusterNearbyPlaces = (places: NearbyPlace[], level: number): MapMarker[] => {
  const threshold = clusterDistanceForLevel(level);
  if (threshold === 0) {
    return places.map((place) =>
      markerFromCluster({ latitude: place.latitude, longitude: place.longitude, places: [place] }),
    );
  }
  const clusters: PlaceCluster[] = [];

  [...places]
    .sort(
      (left, right) =>
        left.distanceMeters - right.distanceMeters || left.providerPlaceId.localeCompare(right.providerPlaceId),
    )
    .forEach((place) => {
      const nearest = clusters
        .map((cluster) => ({ cluster, distance: distanceMeters(cluster, place) }))
        .filter(({ cluster, distance }) => cluster.places[0]?.category === place.category && distance <= threshold)
        .sort((left, right) => left.distance - right.distance)[0]?.cluster;
      if (nearest === undefined) {
        clusters.push({ latitude: place.latitude, longitude: place.longitude, places: [place] });
        return;
      }
      addPlace(nearest, place);
    });

  return clusters.map(markerFromCluster);
};
