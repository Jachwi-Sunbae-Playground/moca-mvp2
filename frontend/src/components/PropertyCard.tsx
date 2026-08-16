import { Link } from 'react-router-dom';
import type { PropertySummary } from '../types/Property';
import { formatDateTime, formatWon } from '../utils/propertyFormat';
import VisitSummaryPanel from './VisitSummaryPanel';
import styles from './PropertyCard.module.css';

type PropertyCardProps = {
  property: PropertySummary;
};

const PropertyCard = ({ property }: PropertyCardProps) => (
  <article className={styles.card}>
    <div className={styles.topline}>
      <h2>
        <Link to={`/properties/${property.propertyId}`}>{property.name}</Link>
      </h2>
      <span className={styles.photoCount} aria-label={`사진 ${property.photoCount}장`}>
        사진 {property.photoCount}장
      </span>
    </div>
    <p className={styles.price}>
      보증금 {formatWon(property.depositAmount)} · 월세 {formatWon(property.monthlyRentAmount)}
    </p>
    <p className={styles.source}>
      <span>발견 경로</span> {property.discoverySource.value}
    </p>
    <VisitSummaryPanel recentVisit={property.recentVisit} compact />
    <p className={styles.activity}>최근 활동 {formatDateTime(property.lastActivityAt)}</p>
  </article>
);

export default PropertyCard;
