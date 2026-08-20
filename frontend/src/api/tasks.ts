import { apiClient } from './client';

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type WorkflowTask = {
  id: string;
  instanceId: string;
  stepCode: string;
  taskType: string;
  title: string;
  description: string | null;
  subjectType: string;
  subjectId: string;
  assigneeUserId: string | null;
  assigneeRoleCode: string | null;
  priority: number;
  dueAt: string | null;
  status: string;
  outcome: string | null;
  completedAt: string | null;
  createdAt: string;
  assignments?: TaskAssignment[];
  comments?: TaskComment[];
  timeline?: TimelineEvent[];
};

export type TaskAssignment = {
  id: string;
  action: string;
  fromUserId: string | null;
  toUserId: string | null;
  toRoleCode: string | null;
  reason: string | null;
  occurredAt: string;
};

export type TaskComment = {
  id: string;
  authorId: string;
  body: string;
  visibility: string;
  createdAt: string;
};

export type TimelineEvent = {
  id: string;
  eventType: string;
  fromStepCode: string | null;
  toStepCode: string | null;
  actionCode: string | null;
  message: string | null;
  occurredAt: string;
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

export const tasksApi = {
  search: (params: Record<string, string | number | undefined>) =>
    apiClient<PageResponse<WorkflowTask>>(`/api/v1/tasks${qs(params)}`),
  my: (params: Record<string, string | number | undefined>) =>
    apiClient<PageResponse<WorkflowTask>>(`/api/v1/tasks/my${qs(params)}`),
  get: (id: string) => apiClient<WorkflowTask>(`/api/v1/tasks/${id}`),
  claim: (id: string) => apiClient<WorkflowTask>(`/api/v1/tasks/${id}/claim`, { method: 'POST' }),
  complete: (id: string, body: { outcome?: string; reason?: string; advanceAction?: string }) =>
    apiClient<WorkflowTask>(`/api/v1/tasks/${id}/complete`, {
      method: 'POST',
      body: JSON.stringify(body)
    }),
  cancel: (id: string, reason: string) =>
    apiClient<WorkflowTask>(`/api/v1/tasks/${id}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    }),
  assign: (id: string, body: { userId?: string; roleCode?: string; reason?: string }) =>
    apiClient<WorkflowTask>(`/api/v1/tasks/${id}/assign`, {
      method: 'POST',
      body: JSON.stringify(body)
    }),
  comment: (id: string, body: string) =>
    apiClient<WorkflowTask>(`/api/v1/tasks/${id}/comments`, {
      method: 'POST',
      body: JSON.stringify({ body, visibility: 'INTERNAL' })
    })
};
