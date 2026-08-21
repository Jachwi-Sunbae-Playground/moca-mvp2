import StatusPanel from '../components/StatusPanel';
import { ButtonLink } from '../components/ui/Button';
import { useAuthentication } from '../hooks/useAuthentication';

const NotFoundPage = () => {
  const { session } = useAuthentication();

  return (
    <StatusPanel
      title="페이지를 찾을 수 없어요"
      description="주소를 다시 확인하거나 시작 화면으로 돌아가 주세요."
      tone="error"
      action={
        <ButtonLink to={session === null ? '/login' : '/'}>
          {session === null ? '로그인 화면으로' : '시작 화면으로'}
        </ButtonLink>
      }
    />
  );
};

export default NotFoundPage;
