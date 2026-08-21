import { queryClient, authenticationQueryKey } from './queryClient';

export type AuthenticationSession = {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
};

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

  state = { session, terminationReason: null };

  const remainingMilliseconds = session.expiresAt - Date.now();

  if (remainingMilliseconds <= 0) {
    clearAuthentication('expired');
    return session;
  }

  expirationTimer = setTimeout(() => {
    clearAuthentication('expired');
  }, remainingMilliseconds);

  emitChange();
  return session;
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
  state = { session: null, terminationReason: null };
  queryClient.clear();
  emitChange();
};
