import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import type { UpdatePropertyRequestDto } from '../apis/dtos/PropertyDto';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import PropertyForm from '../components/PropertyForm';
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
        <h1 className={styles.heading}>매물 정보를 수정해주세요</h1>
        <PropertyForm
          initialValues={{
            name: initial.name,
            depositAmount: formatAmountForInput(initial.depositAmount),
            monthlyRentAmount: formatAmountForInput(initial.monthlyRentAmount),
            maintenanceFeeAmount:
              initial.maintenanceFeeAmount === null ? '' : formatAmountForInput(initial.maintenanceFeeAmount),
            discoverySource: initial.discoverySource.value,
          }}
          submitLabel="변경사항 저장"
          isSubmitting={updateMutation.isPending}
          mutationError={mutationError}
          formNotice={formNotice}
          variant="detail"
          onSubmit={(input) => {
            const changes: UpdatePropertyRequestDto = input;
            const isUnchanged =
              input.name === initial.name &&
              input.depositAmount === initial.depositAmount &&
              input.monthlyRentAmount === initial.monthlyRentAmount &&
              input.maintenanceFeeAmount === initial.maintenanceFeeAmount &&
              (input.discoverySource ?? '') === initial.discoverySource.value;

            if (isUnchanged) {
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
