import { Link } from 'react-router-dom';
import type { PropertySummary } from '../types/Property';
import { formatDateTime, formatWon } from '../utils/propertyFormat';
import VisitSummaryPanel from './VisitSummaryPanel';

type PropertyCardProps = {
  property: PropertySummary;
};

const PropertyCard = ({ property }: PropertyCardProps) => (
  <article className="property-card">
    <div className="property-card__topline">
      <h2>
        <Link to={`/properties/${property.propertyId}`}>{property.name}</Link>
      </h2>
      <span className="photo-count" aria-label={`사진 ${property.photoCount}장`}>
        사진 {property.photoCount}장
      </span>
    </div>
    <p className="property-card__price">
      보증금 {formatWon(property.depositAmount)} · 월세 {formatWon(property.monthlyRentAmount)}
    </p>
    <p className="property-card__source">
      <span>발견 경로</span> {property.discoverySource.value}
    </p>
    <VisitSummaryPanel recentVisit={property.recentVisit} compact />
    <p className="property-card__activity">최근 활동 {formatDateTime(property.lastActivityAt)}</p>
  </article>
);

export default PropertyCard;
