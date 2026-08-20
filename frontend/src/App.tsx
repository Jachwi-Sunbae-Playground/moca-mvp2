import AppProviders from './app/AppProviders';
import AppRoutes from './app/AppRoutes';
import { getPublicConfig } from './app/publicConfig';
import StatusPanel from './components/StatusPanel';
import type { PublicConfig } from './types/PublicConfig';
import './styles/tokens.css';
import './styles/global.css';
import './styles/utilities.css';

type AppProps = {
  config?: PublicConfig;
};

const App = ({ config }: AppProps) => {
  let resolvedConfig: PublicConfig;

  try {
    resolvedConfig = config ?? getPublicConfig();
  } catch (error) {
    const description = error instanceof Error ? error.message : '공개 환경변수 설정을 확인해 주세요.';

    return <StatusPanel title="애플리케이션 설정이 필요합니다" description={description} tone="error" />;
  }

  return (
    <AppProviders>
      <AppRoutes config={resolvedConfig} />
    </AppProviders>
  );
};

export default App;
