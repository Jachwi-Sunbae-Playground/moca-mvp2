import { Link } from 'react-router-dom';
import type { PropertySummary } from '../types/Property';
import type { PublicConfig } from '../types/PublicConfig';
import { formatManwon } from '../utils/propertyFormat';
import Icon from './ui/Icon';
import ChecklistProgressBar from './ChecklistProgressBar';
import styles from './PropertyCard.module.css';
import AuthenticatedPhoto from './AuthenticatedPhoto';
import { checklistStageMeta } from '../constants/checklist';

type PropertyCardProps = {
  property: PropertySummary;
  thumbnailUrl?: string;
  config?: PublicConfig;
};

const PropertyCard = ({ property, thumbnailUrl, config }: PropertyCardProps) => {
  const resolvedThumbnailUrl = thumbnailUrl ?? property.representativePhoto?.contentUrl;
  return (
    <article>
      <Link className={styles.card} to={`/properties/${property.propertyId}`} aria-label={property.name}>
        <div className={styles.mainInfo}>
          <div className={styles.thumbnail}>
            {resolvedThumbnailUrl === undefined ? (
              <Icon name="image" size={22} />
            ) : config !== undefined && property.representativePhoto !== null ? (
              <AuthenticatedPhoto
                config={config}
                propertyId={property.propertyId}
                photoId={property.representativePhoto.photoId}
                contentUrl={resolvedThumbnailUrl}
                alt=""
              />
            ) : (
              <img src={resolvedThumbnailUrl} alt="" />
            )}
            {property.representativePhoto !== null && <span>대표</span>}
          </div>
          <div className={styles.details}>
            <h2>{property.name}</h2>
            <p className={styles.price}>
              보증금 {formatManwon(property.depositAmount)} / 월세 {formatManwon(property.monthlyRentAmount)}
            </p>
            {property.location.address !== null && <p className={styles.address}>{property.location.address}</p>}
            {property.discoverySource.value.length > 0 && (
              <p className={styles.discoverySource}>발견 경로 · {property.discoverySource.value}</p>
            )}
          </div>
        </div>
        <ol className={styles.stageProgress} aria-label="단계별 체크리스트 진행 현황">
          {property.stages.map((stage, index) => (
            <li key={stage.stage}>
              <div className={styles.stageHeading}>
                <span>
                  {index + 1}단계 · {checklistStageMeta[stage.stage].shortLabel}
                </span>
                <strong>
                  {stage.applied ? `${stage.progress.completedCount}/${stage.progress.totalCount}` : '미적용'}
                </strong>
              </div>
              {stage.applied ? (
                <ChecklistProgressBar progress={stage.progress} compact />
              ) : (
                <span className={styles.inactiveBar} aria-hidden="true" />
              )}
            </li>
          ))}
        </ol>
      </Link>
    </article>
  );
};

export default PropertyCard;
