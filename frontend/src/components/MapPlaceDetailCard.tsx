import type { NearbyPlace } from '../types/Map';
import MapCategoryIcon from './MapCategoryIcon';
import { getMapCategoryLabel } from './mapPresentation';
import Icon from './ui/Icon';
import styles from './MapPlaceDetailCard.module.css';

type MapPlaceDetailCardProps = {
  place: NearbyPlace;
  avoidControls?: boolean;
  onClose: () => void;
};

const MapPlaceDetailCard = ({ place, avoidControls = false, onClose }: MapPlaceDetailCardProps) => (
  <section
    className={styles.card}
    data-category={place.category}
    data-avoid-controls={avoidControls || undefined}
    aria-label={`${place.name} 시설 상세`}
    aria-live="polite"
  >
    <span className={styles.iconWrap}>
      <MapCategoryIcon category={place.category} className={styles.icon} />
    </span>
    <div className={styles.content}>
      <span>{getMapCategoryLabel(place.category)}</span>
      <strong>{place.name}</strong>
      <p>
        <b>{place.distanceMeters}m</b>
        <span aria-hidden="true">·</span>
        {place.address || '주소 정보 없음'}
      </p>
    </div>
    <button type="button" aria-label={`${place.name} 상세 닫기`} onClick={onClose}>
      <Icon name="close" size={16} />
    </button>
  </section>
);

export default MapPlaceDetailCard;
