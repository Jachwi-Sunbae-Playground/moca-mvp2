import PageHeading from '../components/PageHeading';
import { ButtonLink } from '../components/ui/Button';
import './UpcomingFeaturePage.css';

type UpcomingFeature = 'compare' | 'export' | 'tips';

const featureContent: Record<UpcomingFeature, { eyebrow: string; title: string; description: string }> = {
  compare: {
    eyebrow: '비교표',
    title: '매물 비교는 준비 중이에요',
    description: '여러 매물의 조건과 체크 결과를 한눈에 비교하는 기능을 다음 범위에서 제공할 예정이에요.',
  },
  export: {
    eyebrow: '내보내기',
    title: '기록 내보내기는 준비 중이에요',
    description: '저장한 매물과 체크리스트 기록을 파일로 내보내는 기능을 다음 범위에서 제공할 예정이에요.',
  },
  tips: {
    eyebrow: '선배 팁',
    title: '선배 팁은 준비 중이에요',
    description: '자취 경험을 바탕으로 한 상황별 팁을 다음 범위에서 제공할 예정이에요.',
  },
};

const UpcomingFeaturePage = ({ feature }: { feature: UpcomingFeature }) => {
  const content = featureContent[feature];

  return (
    <main className="property-page upcoming-feature-page">
      <div className="page-container page-container--form">
        <PageHeading title={content.title} backTo="/me" backLabel="마이페이지" focusOnMount />
        <section className="upcoming-feature-card" aria-labelledby="upcoming-feature-heading">
          <h2 id="upcoming-feature-heading">1차 MVP에서는 안내만 제공해요</h2>
          <p>{content.description}</p>
          <div className="upcoming-feature-card__actions">
            <ButtonLink to="/properties">내 매물 보기</ButtonLink>
            <ButtonLink variant="secondary" to="/checklists">
              체크리스트 보기
            </ButtonLink>
          </div>
        </section>
      </div>
    </main>
  );
};

export default UpcomingFeaturePage;
