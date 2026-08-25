import { render, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { PublicConfig } from '../types/PublicConfig';
import MapCanvas from './MapCanvas';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  mapProviderMode: 'kakao',
  kakaoMapJavaScriptKey: 'test-key',
};

class FakeLatLng {
  constructor(
    private readonly latitude: number,
    private readonly longitude: number,
  ) {}

  getLat = () => this.latitude;
  getLng = () => this.longitude;
}

class FakeOverlay {
  setMap = vi.fn();
}

describe('Kakao 지도 상태 동기화', () => {
  type FakeMapInstance = {
    center: FakeLatLng;
    level: number;
    getCenter: () => FakeLatLng;
    getLevel: () => number;
    setCenter: ReturnType<typeof vi.fn>;
    setLevel: ReturnType<typeof vi.fn>;
  };
  let maps: FakeMapInstance[];

  beforeEach(() => {
    maps = [];
    class FakeMap {
      center: FakeLatLng;
      level: number;
      getCenter = () => this.center;
      getLevel = () => this.level;
      setCenter = vi.fn((center: FakeLatLng) => {
        this.center = center;
      });
      setLevel = vi.fn((level: number) => {
        this.level = level;
      });

      constructor(_: HTMLElement, options: { center: FakeLatLng; level: number }) {
        this.center = options.center;
        this.level = options.level;
        maps.push(this);
      }
    }

    Object.defineProperty(window, 'kakao', {
      configurable: true,
      value: {
        maps: {
          load: (callback: () => void) => callback(),
          LatLng: FakeLatLng,
          Map: FakeMap,
          CustomOverlay: FakeOverlay,
          Circle: FakeOverlay,
          event: { addListener: vi.fn(), removeListener: vi.fn() },
        },
      },
    });
  });

  afterEach(() => {
    Object.defineProperty(window, 'kakao', { configurable: true, value: undefined });
  });

  it('중심 좌표만 갱신될 때 사용자가 바꾼 확대 단계를 되돌리지 않는다', async () => {
    const { rerender } = render(
      <MapCanvas config={config} center={{ latitude: 37.5665, longitude: 126.978 }} level={5} />,
    );
    await waitFor(() => expect(maps).toHaveLength(1));
    const [map] = maps;
    map.level = 3;

    rerender(<MapCanvas config={config} center={{ latitude: 37.567, longitude: 126.979 }} level={5} />);

    await waitFor(() => expect(map.setCenter).toHaveBeenCalledOnce());
    expect(map.setLevel).not.toHaveBeenCalled();
    expect(map.getLevel()).toBe(3);
  });
});
