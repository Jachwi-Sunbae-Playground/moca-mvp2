import { ApiError } from '../apis/apiClient';
import PageHeading from '../components/PageHeading';
import PropertyForm from '../components/PropertyForm';
import { useCreateProperty } from '../hooks/query/usePropertyMutations';
import type { PublicConfig } from '../types/PublicConfig';
import { useNavigate } from 'react-router-dom';

type CreatePropertyPageProps = { config: PublicConfig };

const CreatePropertyPage = ({ config }: CreatePropertyPageProps) => {
  const navigate = useNavigate();
  const createMutation = useCreateProperty(config);
  const mutationError = createMutation.error instanceof ApiError ? createMutation.error : null;

  return (
    <main className="property-page">
      <div className="page-container page-container--form">
        <PageHeading
          title="새 매물 등록"
          description="나중에 알아보기 쉬운 기본 정보를 입력해 주세요."
          backTo="/properties"
          backLabel="매물 목록"
        />
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
