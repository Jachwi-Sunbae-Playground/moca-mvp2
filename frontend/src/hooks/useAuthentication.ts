import { useSyncExternalStore } from 'react';
import { getAuthenticationSnapshot, subscribeAuthentication } from '../app/authStore';

export const useAuthentication = () =>
  useSyncExternalStore(subscribeAuthentication, getAuthenticationSnapshot, getAuthenticationSnapshot);
