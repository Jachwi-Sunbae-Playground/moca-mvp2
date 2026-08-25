import { useMemo, useState } from 'react';
import { fetchPropertyComparisonPdf } from '../apis/propertyApi';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import AuthenticatedPhoto from '../components/AuthenticatedPhoto';
import { Button, ButtonLink } from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import Icon from '../components/ui/Icon';
import InlineNotice from '../components/ui/InlineNotice';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyList } from '../hooks/query/useProperties';
import type { PropertySummary } from '../types/Property';
import type { PublicConfig } from '../types/PublicConfig';
import { formatManwon } from '../utils/propertyFormat';
import styles from './PropertyComparePage.module.css';

const MIN_SELECTION = 2;
const MAX_SELECTION = 5;

type PropertyComparePageProps = { config: PublicConfig };

const StageOverview = ({ property }: { property: PropertySummary }) => (
  <span className={styles.stageOverview}>
    {property.stages.map((stage, index) => (
      <span key={stage.stage} data-applied={stage.applied || undefined}>
        {index + 1}단계 {stage.applied ? `${stage.progress.completedCount}/${stage.progress.totalCount}` : '미적용'}
      </span>
    ))}
  </span>
);

const PropertyComparePage = ({ config }: PropertyComparePageProps) => {
  const properties = usePropertyList(config);
  const items = properties.data?.pages.flatMap((page) => page.content) ?? [];
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [isExporting, setIsExporting] = useState(false);
  const [exportError, setExportError] = useState(false);
  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

  const toggle = (propertyId: number) => {
    setExportError(false);
    setSelectedIds((current) => {
      if (current.includes(propertyId)) return current.filter((id) => id !== propertyId);
      return current.length >= MAX_SELECTION ? current : [...current, propertyId];
    });
  };

  const downloadPdf = async () => {
    if (selectedIds.length < MIN_SELECTION || selectedIds.length > MAX_SELECTION) return;
    setIsExporting(true);
    setExportError(false);
    try {
      const blob = await fetchPropertyComparisonPdf(config, selectedIds);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `moca-property-comparison-${new Date().toISOString().slice(0, 10)}.pdf`;
      document.body.append(anchor);
      anchor.click();
      anchor.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch {
      setExportError(true);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <main className={styles.page}>
      <TopNavigation title="비교할 매물 선택" backTo="/properties" />
      <div className={styles.content}>
        <section className={styles.intro} aria-labelledby="compare-intro-heading">
          <span>기록 비교 PDF</span>
          <h2 id="compare-intro-heading">함께 볼 매물을 골라 주세요.</h2>
          <p>2~5개 매물의 기본 정보, 사진, 메모와 체크 결과를 그대로 모아 드려요.</p>
          <InlineNotice>점수나 추천 없이 저장한 사실만 보여 드립니다. 최종 판단은 직접 해 주세요.</InlineNotice>
        </section>

        <div className={styles.selectionHeader} aria-live="polite">
          <strong>{selectedIds.length}개 선택</strong>
          <span>최대 {MAX_SELECTION}개</span>
        </div>

        {properties.isPending && (
          <div className={styles.loading} role="status">
            <span className={styles.spinner} aria-hidden="true" />
            매물을 불러오는 중이에요.
          </div>
        )}

        {properties.isError && (
          <div className={styles.errorState}>
            <InlineNotice tone="error">{getPropertyErrorMessage(properties.error)}</InlineNotice>
            <Button variant="secondary" fullWidth onClick={() => void properties.refetch()}>
              다시 시도
            </Button>
          </div>
        )}

        {properties.isSuccess && items.length < MIN_SELECTION && (
          <EmptyState
            variant="plain"
            title="비교하려면 매물이 2개 필요해요."
            description="매물을 하나 더 등록한 뒤 기록을 나란히 확인해 보세요."
            action={<ButtonLink to="/properties/new">매물 등록하기</ButtonLink>}
          />
        )}

        {properties.isSuccess && items.length >= MIN_SELECTION && (
          <ul className={styles.propertyList} aria-label="비교할 매물 목록">
            {items.map((property) => {
              const selected = selectedSet.has(property.propertyId);
              const disabled = !selected && selectedIds.length >= MAX_SELECTION;
              return (
                <li key={property.propertyId}>
                  <label className={styles.propertyOption} data-selected={selected || undefined}>
                    <input
                      type="checkbox"
                      checked={selected}
                      disabled={disabled}
                      onChange={() => toggle(property.propertyId)}
                    />
                    <span className={styles.checkmark} aria-hidden="true">
                      {selected && '✓'}
                    </span>
                    <span className={styles.thumbnail}>
                      {property.representativePhoto === null ? (
                        <Icon name="image" size={20} />
                      ) : (
                        <AuthenticatedPhoto
                          config={config}
                          propertyId={property.propertyId}
                          photoId={property.representativePhoto.photoId}
                          contentUrl={property.representativePhoto.contentUrl}
                          alt=""
                        />
                      )}
                    </span>
                    <span className={styles.propertyInfo}>
                      <strong>{property.name}</strong>
                      <span>
                        보증금 {formatManwon(property.depositAmount)} / 월세 {formatManwon(property.monthlyRentAmount)}
                      </span>
                      {property.discoverySource.value.length > 0 && (
                        <span>발견 경로 · {property.discoverySource.value}</span>
                      )}
                      <StageOverview property={property} />
                    </span>
                  </label>
                </li>
              );
            })}
          </ul>
        )}

        {exportError && <InlineNotice tone="error">PDF를 만들지 못했어요. 잠시 후 다시 시도해 주세요.</InlineNotice>}
      </div>

      {properties.isSuccess && items.length >= MIN_SELECTION && (
        <div className={styles.actionBar}>
          <p>
            {selectedIds.length < MIN_SELECTION
              ? `${MIN_SELECTION - selectedIds.length}개 더 선택해 주세요.`
              : `${selectedIds.length}개 매물의 모든 기록을 PDF로 만들어요.`}
          </p>
          <Button
            fullWidth
            disabled={selectedIds.length < MIN_SELECTION}
            isLoading={isExporting}
            loadingLabel="PDF 만드는 중…"
            onClick={() => void downloadPdf()}
          >
            선택한 {selectedIds.length}개 PDF 받기
          </Button>
        </div>
      )}
    </main>
  );
};

export default PropertyComparePage;
