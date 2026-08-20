import { apiClient } from './client';

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type PlatformUser = {
  id: string;
  username: string;
  email: string;
  displayName: string | null;
  role: string;
  roles: string[];
  status: string;
};

export type AuditLogEntry = {
  id: string;
  action: string;
  actorUserId: string | null;
  resourceType: string | null;
  resourceId: string | null;
  correlationId: string | null;
  detailsJson: string | null;
  createdAt: string;
};

export type RoleView = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  systemRole: boolean;
};

export type PermissionView = {
  id: string;
  code: string;
  name: string;
  resource: string;
  action: string;
};

function qs(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== '') search.set(k, String(v));
  });
  const s = search.toString();
  return s ? `?${s}` : '';
}

export const usersApi = {
  search: (params: { q?: string; status?: string; page?: number; size?: number } = {}) =>
    apiClient<PageResponse<PlatformUser>>(`/api/v1/users${qs(params)}`),
  get: (id: string) => apiClient<PlatformUser>(`/api/v1/users/${id}`),
  create: (body: {
    username: string;
    email: string;
    password: string;
    displayName?: string;
    roles: string[];
  }) =>
    apiClient<PlatformUser>('/api/v1/users', {
      method: 'POST',
      body: JSON.stringify(body)
    }),
  update: (
    id: string,
    body: { email?: string; displayName?: string; status?: string; roles?: string[] }
  ) =>
    apiClient<PlatformUser>(`/api/v1/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body)
    }),
  disable: (id: string) => apiClient<PlatformUser>(`/api/v1/users/${id}/disable`, { method: 'POST' }),
  enable: (id: string) => apiClient<PlatformUser>(`/api/v1/users/${id}/enable`, { method: 'POST' }),
  remove: (id: string) => apiClient<void>(`/api/v1/users/${id}`, { method: 'DELETE' }),
  audit: (id: string) => apiClient<AuditLogEntry[]>(`/api/v1/users/${id}/audit`),
  listRoles: () => apiClient<RoleView[]>('/api/v1/roles'),
  listPermissions: () => apiClient<PermissionView[]>('/api/v1/permissions')
};
