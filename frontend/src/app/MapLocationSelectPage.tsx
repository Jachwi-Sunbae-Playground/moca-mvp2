import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { reverseGeocode, searchAddress } from '../apis/mapApi';
import MapCanvas from '../components/MapCanvas';
import { Button } from '../components/ui/Button';
import InlineNotice from '../components/ui/InlineNotice';
import SearchField from '../components/ui/SearchField';
import TopNavigation from '../components/ui/TopNavigation';
import type { MapAddress } from '../types/Map';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './MapPage.module.css';

const DEFAULT_LOCATION: MapAddress = {
  address: '서울 중구 세종대로 110',
  roadAddress: '서울 중구 세종대로 110',
  jibunAddress: '서울 중구 태평로1가 31',
  latitude: 37.5665,
  longitude: 126.978,
};

const MapLocationSelectPage = ({ config }: { config: PublicConfig }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const returnTo = (location.state as { returnTo?: string } | null)?.returnTo ?? '/properties/new';
  const editing = returnTo.endsWith('/edit');
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<MapAddress[]>([]);
  const [selected, setSelected] = useState<MapAddress>(DEFAULT_LOCATION);
  const [status, setStatus] = useState<'idle' | 'loading' | 'saving' | 'error'>('idle');

  const submitSearch = async () => {
    if (query.trim() === '') return;
    setStatus('loading');
    try {
      const next = await searchAddress(config, query.trim());
      setResults(next);
      if (next[0] !== undefined) setSelected(next[0]);
      setStatus('idle');
    } catch {
      setStatus('error');
    }
  };

  const selectCoordinates = async (latitude: number, longitude: number) => {
    setSelected((current) => ({ ...current, latitude, longitude }));
    setStatus('loading');
    try {
      setSelected(await reverseGeocode(config, latitude, longitude));
      setStatus('idle');
    } catch {
      setStatus('error');
    }
  };

  return (
    <main className={styles.page}>
      <TopNavigation title="위치 선택" backTo={editing ? returnTo : '/map'} backLabel="이전 화면으로 돌아가기" />
      <div className={styles.locationContent}>
        <h1>어디에 있는 매물인가요?</h1>
        <p>주소를 검색하거나 지도에서 위치를 눌러 주세요.</p>
        <SearchField
          label="주소 검색"
          value={query}
          placeholder="도로명 또는 지번 주소"
          onValueChange={setQuery}
          onSubmit={() => void submitSearch()}
          onClear={() => setResults([])}
        />
        {status === 'loading' && (
          <p role="status" className={styles.helper}>
            주소를 확인하는 중이에요.
          </p>
        )}
        {status === 'error' && <InlineNotice tone="error">주소를 확인하지 못했어요. 다시 시도해 주세요.</InlineNotice>}
        {results.length > 0 && (
          <ul className={styles.addressResults} aria-label="주소 검색 결과">
            {results.map((result) => (
              <li key={`${result.latitude}-${result.longitude}`}>
                <button type="button" onClick={() => setSelected(result)}>
                  <strong>{result.roadAddress ?? result.jibunAddress}</strong>
                  {result.roadAddress !== null && result.jibunAddress !== null && <span>{result.jibunAddress}</span>}
                </button>
              </li>
            ))}
          </ul>
        )}
        <MapCanvas
          config={config}
          center={selected}
          markers={[{ id: 'selected', label: '선택한 위치', tone: 'property', ...selected }]}
          selectedMarkerId="selected"
          interactive
          onSelectLocation={(latitude, longitude) => void selectCoordinates(latitude, longitude)}
        />
        <section className={styles.selectedAddress} aria-live="polite">
          <span>선택한 주소</span>
          <strong>{selected.roadAddress ?? selected.jibunAddress ?? '지도에서 위치를 선택해 주세요'}</strong>
          {selected.roadAddress !== null && selected.jibunAddress !== null && <small>{selected.jibunAddress}</small>}
        </section>
        <Button
          type="button"
          fullWidth
          isLoading={status === 'saving'}
          loadingLabel="위치를 적용하는 중…"
          onClick={() => {
            setStatus('saving');
            navigate(returnTo, {
              replace: true,
              state: {
                roadAddress: selected.roadAddress ?? '',
                jibunAddress: selected.jibunAddress ?? '',
                latitude: selected.latitude,
                longitude: selected.longitude,
              },
            });
          }}
        >
          {editing ? '이 위치 적용하기' : '이 위치로 매물 등록하기'}
        </Button>
      </div>
    </main>
  );
};

export default MapLocationSelectPage;
