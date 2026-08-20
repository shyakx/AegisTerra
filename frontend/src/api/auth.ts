import { apiClient } from './client';

export type LoginRequest = {
  username: string;
  password: string;
  rememberMe?: boolean;
};

export type AuthUser = {
  id: string;
  username: string;
  email: string;
  displayName?: string | null;
  roles: string[];
  permissions: string[];
  mustChangePassword?: boolean;
  farmerId?: string | null;
};

export type LoginResponse = {
  user: AuthUser;
};

export type MessageResponse = {
  message: string;
};

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  return apiClient<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
    skipAuthRetry: true
  });
}

export async function refreshSession(): Promise<void> {
  await apiClient<void>('/api/v1/auth/refresh', {
    method: 'POST',
    skipAuthRetry: true
  });
}

export async function logout(): Promise<void> {
  await apiClient<void>('/api/v1/auth/logout', {
    method: 'POST',
    skipAuthRetry: true
  });
}

export async function fetchMe(): Promise<AuthUser> {
  return apiClient<AuthUser>('/api/v1/auth/me');
}

export async function updateProfile(payload: {
  email?: string;
  displayName?: string;
}): Promise<AuthUser> {
  return apiClient<AuthUser>('/api/v1/auth/profile', {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function changePassword(payload: {
  currentPassword: string;
  newPassword: string;
}): Promise<MessageResponse> {
  return apiClient<MessageResponse>('/api/v1/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function forgotPassword(email: string): Promise<MessageResponse> {
  return apiClient<MessageResponse>('/api/v1/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
    skipAuthRetry: true
  });
}

export async function resetPassword(payload: {
  token: string;
  newPassword: string;
}): Promise<MessageResponse> {
  return apiClient<MessageResponse>('/api/v1/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(payload),
    skipAuthRetry: true
  });
}
