import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import BottomActionArea from '../components/ui/BottomActionArea';
import { Button } from '../components/ui/Button';
import InlineNotice from '../components/ui/InlineNotice';
import TextAreaField from '../components/ui/TextAreaField';
import TextField from '../components/ui/TextField';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyDetail, usePropertyMemo } from '../hooks/query/useProperties';
import { useSavePropertyMemoDocument } from '../hooks/query/usePropertyMutations';
import type { PublicConfig } from '../types/PublicConfig';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './PropertyMemoPage.module.css';

const PropertyMemoPage = ({ config }: { config: PublicConfig }) => {
  const propertyId = parsePositiveId(useParams().propertyId);
  if (propertyId === null) {
    return (
      <main className="property-page">
        <div className="content-state">
          <strong>올바른 매물 주소가 아니에요.</strong>
          <Link to="/properties">매물 목록으로 돌아가기</Link>
        </div>
      </main>
    );
  }
  return <ResolvedPropertyMemoPage config={config} propertyId={propertyId} />;
};

const ResolvedPropertyMemoPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const navigate = useNavigate();
  const property = usePropertyDetail(config, propertyId);
  const memo = usePropertyMemo(config, propertyId);
  const saveMemo = useSavePropertyMemoDocument(config, propertyId);
  const [itemValues, setItemValues] = useState<Record<number, string>>({});
  const [freeMemo, setFreeMemo] = useState('');

  useEffect(() => {
    if (memo.data === undefined) return;
    setItemValues(Object.fromEntries(memo.data.items.map((item) => [item.systemMemoItemId, item.content])));
    setFreeMemo(memo.data.freeMemo);
  }, [memo.data]);

  if (property.isPending || memo.isPending) {
    return <div className="content-state">메모를 불러오는 중이에요.</div>;
  }
  if (property.isError || memo.isError) {
    const error = property.error ?? memo.error;
    return (
      <main className="property-page">
        <div className="content-state content-state--error" role="alert">
          <strong>메모를 불러오지 못했어요.</strong>
          <span>{getPropertyErrorMessage(error)}</span>
          <button
            type="button"
            className="inline-button"
            onClick={() => {
              void property.refetch();
              void memo.refetch();
            }}
          >
            다시 시도
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <TopNavigation
        title={`${property.data.name} 메모`}
        backTo={`/properties/${propertyId}`}
        backLabel="매물 상세로 돌아가기"
      />
      <form
        className={styles.form}
        onSubmit={(event) => {
          event.preventDefault();
          void saveMemo
            .mutateAsync({
              items: memo.data.items.map((item) => ({
                propertyMemoItemId: item.propertyMemoItemId,
                content: itemValues[item.systemMemoItemId]?.trim() ?? '',
              })),
              freeMemo: freeMemo.trim(),
            })
            .then(() => navigate(`/properties/${propertyId}`, { replace: true }))
            .catch(() => undefined);
        }}
      >
        <p className={styles.description}>필요한 항목만 적어도 됩니다.</p>
        <section className={styles.memoFields} aria-labelledby="structured-memo-heading">
          <h1 id="structured-memo-heading">기본 메모 양식</h1>
          {memo.data.items.map((item) => (
            <TextField
              key={item.systemMemoItemId}
              label={item.label}
              value={itemValues[item.systemMemoItemId] ?? ''}
              maxLength={100}
              placeholder="필요한 내용을 입력해 주세요."
              onChange={(event) =>
                setItemValues((current) => ({ ...current, [item.systemMemoItemId]: event.target.value }))
              }
            />
          ))}
        </section>
        <section className={styles.freeMemo}>
          <TextAreaField
            label="추가 메모"
            value={freeMemo}
            rows={5}
            maxLength={2_000}
            placeholder="그 외 내용을 자유롭게 적어보세요."
            helpText={`${Array.from(freeMemo).length.toLocaleString('ko-KR')} / 2,000`}
            onChange={(event) => setFreeMemo(event.target.value)}
          />
        </section>
        {saveMemo.isError && <InlineNotice tone="error">메모를 저장하지 못했어요. 다시 시도해 주세요.</InlineNotice>}
        <BottomActionArea>
          <Button type="submit" variant="soft" fullWidth isLoading={saveMemo.isPending} loadingLabel="저장 중…">
            메모 저장
          </Button>
        </BottomActionArea>
      </form>
    </main>
  );
};

export default PropertyMemoPage;
