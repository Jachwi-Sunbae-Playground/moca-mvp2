import { useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import PropertyForm from '../components/PropertyForm';
import TopNavigation from '../components/ui/TopNavigation';
import { useCreateProperty } from '../hooks/query/usePropertyMutations';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './CreatePropertyPage.module.css';

type CreatePropertyPageProps = { config: PublicConfig };

const CreatePropertyPage = ({ config }: CreatePropertyPageProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const createMutation = useCreateProperty(config);
  const mutationError = createMutation.error instanceof ApiError ? createMutation.error : null;
  const selectedLocation =
    (location.state as {
      roadAddress?: string;
      jibunAddress?: string;
      latitude?: number;
      longitude?: number;
    } | null) ?? {};

  return (
    <main className={styles.page}>
      <div className={styles.content}>
        <TopNavigation title="새 매물 등록" backTo="/properties" backLabel="매물 등록 닫기" navigationIcon="close" />
        <h1 className={styles.heading}>기본 정보를 입력해주세요</h1>
        <PropertyForm
          initialValues={{
            name: '',
            depositAmount: '',
            monthlyRentAmount: '',
            discoverySource: '',
            roadAddress: selectedLocation.roadAddress,
            jibunAddress: selectedLocation.jibunAddress,
            latitude: selectedLocation.latitude,
            longitude: selectedLocation.longitude,
          }}
          submitLabel="매물 등록"
          isSubmitting={createMutation.isPending}
          mutationError={mutationError}
          onSelectLocation={() => navigate('/map/select-location', { state: { returnTo: '/properties/new' } })}
          onSubmit={(input) => {
            void createMutation
              .mutateAsync(input)
              .then((created) => navigate(`/properties/${created.propertyId}`, { replace: true }))
              .catch(() => undefined);
          }}
        />
      </div>
    </main>
  );
};

export default CreatePropertyPage;
