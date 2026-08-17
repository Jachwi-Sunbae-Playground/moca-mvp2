import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import type { UpdatePropertyRequestDto } from '../apis/dtos/PropertyDto';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import PropertyForm from '../components/PropertyForm';
import PropertyPhotoManager from '../components/PropertyPhotoManager';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyDetail } from '../hooks/query/useProperties';
import { useUpdateProperty } from '../hooks/query/usePropertyMutations';
import type { PublicConfig } from '../types/PublicConfig';
import { formatAmountForInput } from '../utils/propertyForm';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './EditPropertyPage.module.css';

type EditPropertyPageProps = { config: PublicConfig };

const EditPropertyPage = ({ config }: EditPropertyPageProps) => {
  const { propertyId: propertyIdParam } = useParams();
  const propertyId = parsePositiveId(propertyIdParam);

  if (propertyId === null) {
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state">
            <strong>올바른 매물 주소가 아니에요.</strong>
            <Link to="/properties">매물 목록으로 돌아가기</Link>
          </div>
        </div>
      </main>
    );
  }

  return <ResolvedEditPropertyPage config={config} propertyId={propertyId} />;
};

const ResolvedEditPropertyPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const navigate = useNavigate();
  const property = usePropertyDetail(config, propertyId);
  const updateMutation = useUpdateProperty(config, propertyId);
  const [formNotice, setFormNotice] = useState<string | null>(null);

  if (property.isPending)
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state" role="status">
            <span className="spinner" />
            매물 정보를 불러오는 중이에요.
          </div>
        </div>
      </main>
    );
  if (property.isError)
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state content-state--error">
            <strong>
              {property.error instanceof ApiError && property.error.code === 'PROPERTY_NOT_FOUND'
                ? '매물을 찾을 수 없어요.'
                : '매물 정보를 불러오지 못했어요.'}
            </strong>
            <span>{getPropertyErrorMessage(property.error)}</span>
            <Link to="/properties">매물 목록으로 돌아가기</Link>
          </div>
        </div>
      </main>
    );

  const initial = property.data;
  const mutationError = updateMutation.error instanceof ApiError ? updateMutation.error : null;

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <TopNavigation
          title="매물 정보 수정"
          backTo={`/properties/${propertyId}`}
          backLabel="매물 정보 수정 닫기"
          navigationIcon="close"
        />
        <PropertyPhotoManager config={config} propertyId={propertyId} showHeading />
        <p className={styles.description}>수정할 값을 눌러 내용을 변경해 주세요.</p>
        <PropertyForm
          initialValues={{
            name: initial.name,
            depositAmount: formatAmountForInput(initial.depositAmount),
            monthlyRentAmount: formatAmountForInput(initial.monthlyRentAmount),
            discoverySource: initial.discoverySource.value,
          }}
          submitLabel="변경사항 저장"
          isSubmitting={updateMutation.isPending}
          mutationError={mutationError}
          formNotice={formNotice}
          variant="detail"
          onSubmit={(input) => {
            const changes: UpdatePropertyRequestDto = {};
            if (input.name !== initial.name) changes.name = input.name;
            if (input.depositAmount !== initial.depositAmount) changes.depositAmount = input.depositAmount;
            if (input.monthlyRentAmount !== initial.monthlyRentAmount)
              changes.monthlyRentAmount = input.monthlyRentAmount;
            if (input.discoverySource !== initial.discoverySource.value)
              changes.discoverySource = input.discoverySource;

            if (Object.keys(changes).length === 0) {
              setFormNotice('변경된 내용이 없어 서버에 요청하지 않았어요.');
              return;
            }

            setFormNotice(null);
            void updateMutation
              .mutateAsync(changes)
              .then(() => navigate(`/properties/${propertyId}`, { replace: true }))
              .catch(() => undefined);
          }}
        />
      </div>
    </main>
  );
};

export default EditPropertyPage;
