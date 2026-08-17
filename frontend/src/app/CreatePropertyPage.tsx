import { useNavigate } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import PropertyForm from '../components/PropertyForm';
import TopNavigation from '../components/ui/TopNavigation';
import { useCreateProperty } from '../hooks/query/usePropertyMutations';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './CreatePropertyPage.module.css';

type CreatePropertyPageProps = { config: PublicConfig };

const CreatePropertyPage = ({ config }: CreatePropertyPageProps) => {
  const navigate = useNavigate();
  const createMutation = useCreateProperty(config);
  const mutationError = createMutation.error instanceof ApiError ? createMutation.error : null;

  return (
    <main className={styles.page}>
      <div className={styles.content}>
        <TopNavigation title="새 매물 등록" backTo="/properties" backLabel="매물 등록 닫기" navigationIcon="close" />
        <PropertyForm
          initialValues={{ name: '', depositAmount: '', monthlyRentAmount: '', discoverySource: '' }}
          submitLabel="매물 등록"
          isSubmitting={createMutation.isPending}
          mutationError={mutationError}
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
