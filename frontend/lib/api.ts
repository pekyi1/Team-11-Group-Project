// API utility with JWT token attachment and refresh handling

import { getAccessToken, getRefreshToken, setAuthTokens, clearAuthTokens, AuthResponse } from './auth';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

interface FetchOptions extends RequestInit {
  skipAuth?: boolean;
}

const syncTokenToCookie = (token: string) => {
  // Set token as cookie for middleware to read
  document.cookie = `access_token=${token}; path=/; max-age=3600`;
};

const clearTokenCookie = () => {
  document.cookie = 'access_token=; path=/; max-age=0';
};

export const apiCall = async (
  endpoint: string,
  options: FetchOptions = {}
): Promise<Response> => {
  const { skipAuth = false, ...fetchOptions } = options;
  const url = `${API_BASE}${endpoint}`;

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers,
  };

  // Attach access token if not skipped
  if (!skipAuth) {
    const token = getAccessToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
      syncTokenToCookie(token);
    }
  }

  let response = await fetch(url, {
    ...fetchOptions,
    headers,
  });

  // Handle token refresh on 401
  if (response.status === 401 && !skipAuth) {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        const refreshResponse = await fetch(`${API_BASE}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });

        if (refreshResponse.ok) {
          const newAuthData: AuthResponse = await refreshResponse.json();
          setAuthTokens(newAuthData);
          syncTokenToCookie(newAuthData.accessToken);

          // Retry original request with new token
          headers['Authorization'] = `Bearer ${newAuthData.accessToken}`;
          response = await fetch(url, {
            ...fetchOptions,
            headers,
          });
        } else {
          clearAuthTokens();
          clearTokenCookie();
          window.location.href = '/login';
        }
      } catch (error) {
        console.error('Token refresh failed:', error);
        clearAuthTokens();
        clearTokenCookie();
        window.location.href = '/login';
      }
    } else {
      clearAuthTokens();
      clearTokenCookie();
      window.location.href = '/login';
    }
  }

  return response;
};

