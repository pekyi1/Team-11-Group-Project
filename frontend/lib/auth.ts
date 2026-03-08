// Auth utility functions for token management and authentication

const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_KEY = 'user_info';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: 'USER' | 'AGENT' | 'ADMIN';
  departmentName?: string;
  departmentId?: string;
  locationName?: string;
  locationId?: string;
  isActive: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  refreshToken: string;
  user: User;
}

export const getAccessToken = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY);
};

export const getRefreshToken = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(REFRESH_TOKEN_KEY);
};

export const getStoredUser = (): User | null => {
  if (typeof window === 'undefined') return null;
  const stored = localStorage.getItem(USER_KEY);
  console.log(stored)
  return stored ? JSON.parse(stored) : null;
};

export const setAuthTokens = (response: AuthResponse): void => {
  if (typeof window === 'undefined') return;
  localStorage.setItem(TOKEN_KEY, response.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
  localStorage.setItem(USER_KEY, JSON.stringify(response.user));
  document.cookie = `access_token=${response.accessToken}; path=/`;
};

export const clearAuthTokens = (): void => {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);

  document.cookie = "access_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC;";
};

export const isAuthenticated = (): boolean => {
  return getAccessToken() !== null;
};

export const getRoleDashboardPath = (role: string): string => {
  switch (role) {
    case 'AGENT':
      return '/dashboard';
    case 'ADMIN':
      return '/admin-dashboard';
    case 'USER':
    default:
      return '/dashboard';
  }
};

