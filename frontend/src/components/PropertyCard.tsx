import { Link } from 'react-router-dom';
import type { PropertySummary } from '../types/Property';
import { formatWon } from '../utils/propertyFormat';
import Icon from './ui/Icon';
import VisitResultBar from './VisitResultBar';
import styles from './PropertyCard.module.css';

type PropertyCardProps = {
  property: PropertySummary;
  thumbnailUrl?: string;
};

const PropertyCard = ({ property, thumbnailUrl }: PropertyCardProps) => {
  const summary = property.recentVisit?.summary;
  const progressLabel =
    property.recentVisit === null ? '미완료' : property.recentVisit.status === 'IN_PROGRESS' ? '작성 중' : null;

  return (
    <article>
      <Link className={styles.card} to={`/properties/${property.propertyId}`} aria-label={property.name}>
        {progressLabel !== null && <span className={styles.progressLabel}>{progressLabel}</span>}
        <div className={styles.mainInfo}>
          <div className={styles.thumbnail}>
            {thumbnailUrl === undefined ? <Icon name="image" size={22} /> : <img src={thumbnailUrl} alt="" />}
            <span aria-label={`사진 ${property.photoCount}장`}>{property.photoCount}장</span>
          </div>
          <div className={styles.details}>
            <h2>{property.name}</h2>
            <p className={styles.price}>
              보증금 {formatWon(property.depositAmount)} / 월세 {formatWon(property.monthlyRentAmount)}
            </p>
          </div>
        </div>
        {summary === undefined ? (
          <div className={styles.emptyVisit}>
            <span className={styles.emptyBar} aria-hidden="true" />
            <span>아직 방문 확인 기록이 없어요.</span>
          </div>
        ) : (
          <VisitResultBar summary={summary} />
        )}
      </Link>
    </article>
  );
};

export default PropertyCard;
