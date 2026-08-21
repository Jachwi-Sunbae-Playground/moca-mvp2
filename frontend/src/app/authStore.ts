import { queryClient, authenticationQueryKey } from './queryClient';

export type AuthenticationSession = {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
};

export const authenticationSessionStorageKey = 'jachwi-sunbae:authentication-session:v1';

export type AuthenticationTerminationReason = 'expired' | 'unauthorized' | 'logout' | null;

type AuthenticationState = {
  session: AuthenticationSession | null;
  terminationReason: AuthenticationTerminationReason;
};

type AuthenticationInput = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

const listeners = new Set<() => void>();
let expirationTimer: ReturnType<typeof setTimeout> | null = null;
let authenticationRevision = 0;
let authenticationStorage: Storage | null = null;
let state: AuthenticationState = {
  session: null,
  terminationReason: null,
};

const emitChange = () => {
  listeners.forEach((listener) => listener());
};

const clearExpirationTimer = () => {
  if (expirationTimer !== null) {
    clearTimeout(expirationTimer);
    expirationTimer = null;
  }
};

const clearAuthenticationClientState = () => {
  void queryClient.cancelQueries({ queryKey: authenticationQueryKey });
  queryClient.removeQueries({ queryKey: authenticationQueryKey });
  queryClient.getMutationCache().clear();
};

const getAuthenticationStorage = (): Storage | null => {
  if (authenticationStorage !== null) {
    return authenticationStorage;
  }

  try {
    authenticationStorage = window.sessionStorage;
    return authenticationStorage;
  } catch {
    return null;
  }
};

const removeStoredAuthentication = () => {
  try {
    getAuthenticationStorage()?.removeItem(authenticationSessionStorageKey);
  } catch {
    // Web Storage를 사용할 수 없는 환경에서는 메모리 인증만 정리한다.
  }
};

const persistAuthentication = (session: AuthenticationSession) => {
  try {
    getAuthenticationStorage()?.setItem(authenticationSessionStorageKey, JSON.stringify(session));
  } catch {
    // 저장 실패가 로그인 자체를 막지 않도록 메모리 인증을 유지한다.
  }
};

const isAuthenticationSession = (value: unknown): value is AuthenticationSession => {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const session = value as Record<string, unknown>;

  return (
    typeof session.accessToken === 'string' &&
    session.accessToken.length > 0 &&
    session.tokenType === 'Bearer' &&
    typeof session.expiresAt === 'number' &&
    Number.isFinite(session.expiresAt)
  );
};

const scheduleAuthenticationExpiration = (session: AuthenticationSession) => {
  const remainingMilliseconds = session.expiresAt - Date.now();

  if (remainingMilliseconds <= 0) {
    clearAuthentication('expired');
    return;
  }

  expirationTimer = setTimeout(() => {
    clearAuthentication('expired');
  }, remainingMilliseconds);
};

export const calculateExpiresAt = (expiresIn: number, now = Date.now()): number => {
  if (!Number.isFinite(expiresIn) || expiresIn <= 0) {
    return now;
  }

  return now + expiresIn * 1_000;
};

export const clearAuthentication = (reason: AuthenticationTerminationReason) => {
  clearExpirationTimer();
  authenticationRevision += 1;
  state = { session: null, terminationReason: reason };
  removeStoredAuthentication();
  clearAuthenticationClientState();
  emitChange();
};

export const setAuthentication = ({
  accessToken,
  tokenType,
  expiresIn,
}: AuthenticationInput): AuthenticationSession => {
  clearExpirationTimer();
  authenticationRevision += 1;
  clearAuthenticationClientState();

  const session = {
    accessToken,
    tokenType,
    expiresAt: calculateExpiresAt(expiresIn),
  };

  if (session.expiresAt <= Date.now()) {
    clearAuthentication('expired');
    return session;
  }

  state = { session, terminationReason: null };
  persistAuthentication(session);
  scheduleAuthenticationExpiration(session);

  emitChange();
  return session;
};

export const restoreAuthentication = (storage?: Storage): AuthenticationSession | null => {
  clearExpirationTimer();
  authenticationRevision += 1;
  authenticationStorage = storage ?? getAuthenticationStorage();

  let storedSession: string | null;

  try {
    storedSession = authenticationStorage?.getItem(authenticationSessionStorageKey) ?? null;
  } catch {
    state = { session: null, terminationReason: null };
    return null;
  }

  if (storedSession === null) {
    state = { session: null, terminationReason: null };
    return null;
  }

  let parsedSession: unknown;

  try {
    parsedSession = JSON.parse(storedSession);
  } catch {
    removeStoredAuthentication();
    state = { session: null, terminationReason: null };
    return null;
  }

  if (!isAuthenticationSession(parsedSession) || parsedSession.expiresAt <= Date.now()) {
    removeStoredAuthentication();
    state = {
      session: null,
      terminationReason: isAuthenticationSession(parsedSession) ? 'expired' : null,
    };
    return null;
  }

  state = { session: parsedSession, terminationReason: null };
  scheduleAuthenticationExpiration(parsedSession);
  return parsedSession;
};

export const getAccessToken = (): string | null => {
  const { session } = state;

  if (session === null) {
    return null;
  }

  if (session.expiresAt <= Date.now()) {
    clearAuthentication('expired');
    return null;
  }

  return session.accessToken;
};

export const getAuthenticationRevision = (): number => authenticationRevision;

export const getAuthenticationSnapshot = (): AuthenticationState => state;

export const subscribeAuthentication = (listener: () => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const resetAuthenticationForTests = () => {
  clearExpirationTimer();
  authenticationRevision += 1;
  removeStoredAuthentication();
  authenticationStorage = null;
  state = { session: null, terminationReason: null };
  queryClient.clear();
  emitChange();
};
