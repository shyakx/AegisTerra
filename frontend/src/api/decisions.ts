import { apiClient } from './client';

export type DecisionType = {
  code: string;
  name: string;
  description: string | null;
  requiresComment: boolean;
  requiresTargetUser: boolean;
  sortOrder: number;
  status: string;
};

export type WorkflowDecision = {
  id: string;
  taskId: string;
  instanceId: string;
  stepCode: string;
  decisionTypeCode: string;
  outcomeCode: string;
  effect: string;
  workflowAction: string | null;
  comment: string | null;
  targetUserId: string | null;
  subjectType: string;
  subjectId: string;
  decidedBy: string;
  decidedAt: string;
  status: string;
};

export const decisionsApi = {
  types: () => apiClient<DecisionType[]>('/api/v1/decisions/types'),
  history: (taskId: string) => apiClient<WorkflowDecision[]>(`/api/v1/tasks/${taskId}/decisions`),
  decide: (
    taskId: string,
    body: { decisionTypeCode: string; comment?: string; targetUserId?: string }
  ) =>
    apiClient<WorkflowDecision>(`/api/v1/tasks/${taskId}/decisions`, {
      method: 'POST',
      body: JSON.stringify(body)
    })
};
