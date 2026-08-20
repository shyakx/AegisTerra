import { apiClient } from './client';

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type NotificationItem = {
  id: string;
  channel: string;
  title: string;
  body: string;
  eventType: string | null;
  subjectType: string | null;
  subjectId: string | null;
  correlationId: string | null;
  templateCode: string | null;
  sentAt: string | null;
  readAt: string | null;
  status: string;
  createdAt: string;
};

export type NotificationPreference = {
  channel: string;
  eventType: string;
  enabled: boolean;
};

function qs(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.length > 0) {
      search.set(key, String(value));
    }
  });
  const value = search.toString();
  return value ? `?${value}` : '';
}

export const notificationsApi = {
  list: (params: Record<string, string | number | undefined> = {}) =>
    apiClient<PageResponse<NotificationItem>>(`/api/v1/notifications${qs(params)}`),
  unread: (params: Record<string, string | number | undefined> = {}) =>
    apiClient<PageResponse<NotificationItem>>(`/api/v1/notifications/unread${qs(params)}`),
  unreadCount: () => apiClient<{ count: number }>('/api/v1/notifications/unread-count'),
  get: (id: string) => apiClient<NotificationItem>(`/api/v1/notifications/${id}`),
  markRead: (id: string) =>
    apiClient<NotificationItem>(`/api/v1/notifications/${id}/read`, { method: 'POST' }),
  preferences: () => apiClient<NotificationPreference[]>('/api/v1/notification-preferences'),
  updatePreferences: (body: NotificationPreference[]) =>
    apiClient<NotificationPreference[]>('/api/v1/notification-preferences', {
      method: 'PUT',
      body: JSON.stringify(body)
    })
};
