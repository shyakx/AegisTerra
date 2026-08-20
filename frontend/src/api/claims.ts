import { apiClient } from './client';
import type { PageResponse } from './insurance';

export type Claim = {
  id: string;
  claimNumber: string;
  policyId: string;
  claimTypeCode: string | null;
  farmerId: string | null;
  farmId: string | null;
  seasonId: string | null;
  cropId: string | null;
  causeOfLoss: string | null;
  description: string | null;
  claimedAmount: number;
  assessedAmount: number | null;
  approvedAmount: number | null;
  currency: string;
  incidentDate: string;
  status: string;
  coverageSnapshotJson: string | null;
  financialSnapshotJson: string | null;
  workflowInstanceId: string | null;
  workflowDefinitionCode: string | null;
  fraudTier: string | null;
  fraudScore: number | null;
  submittedAt: string | null;
  closedAt: string | null;
  reasonCode: string | null;
  correlationId: string | null;
};

export type ClaimType = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  nature: string;
  workflowDefinitionCode: string;
  assessmentProfileJson: string | null;
  requiresGeo: boolean;
  status: string;
};

export type ClaimEvidence = {
  id: string;
  claimId: string;
  documentType: string;
  title: string;
  documentId: string | null;
  storageUri: string | null;
  contentSha256: string | null;
  latitude: number | null;
  longitude: number | null;
  capturedAt: string | null;
  source: string | null;
  status: string;
  createdAt: string;
};

export type ClaimAssessment = {
  id: string;
  claimId: string;
  inspectionId: string | null;
  methodsJson: string;
  findingsJson: string | null;
  recommendedAmount: number;
  assessedAmount?: number | null;
  currency: string;
  confidence: number | null;
  fraudHintsJson: string | null;
  assessorId: string | null;
  accepted: boolean;
  notes: string | null;
  assessedAt: string | null;
  createdAt?: string;
};

export type ClaimInspection = {
  id: string;
  claimId: string;
  inspectorId: string | null;
  scheduledAt: string | null;
  completedAt: string | null;
  checkInLatitude: number | null;
  checkInLongitude: number | null;
  findingsJson: string | null;
  checklistJson: string | null;
  notes: string | null;
  status: string;
  createdAt: string;
};

export type ClaimTimelineEntry = {
  kind: string;
  fromStatus: string | null;
  toStatus: string | null;
  reason: string | null;
  actorId: string | null;
  occurredAt: string;
};

export type ClaimDraft = {
  id: string;
  ownerUserId: string;
  claimId: string | null;
  currentStep: number;
  payloadJson: string;
  status: string;
  expiresAt: string | null;
  updatedAt: string;
};

export type ClaimSubmitPayload = {
  policyId: string;
  claimTypeCode: string;
  incidentDate: string;
  description: string;
  claimedAmount: number;
  currency?: string;
  farmId?: string;
  seasonId?: string;
  cropId?: string;
  causeOfLoss?: string;
  correlationId?: string;
};

export type WorkflowInstance = {
  id: string;
  currentStepCode: string;
  status: string;
  events: Array<{
    id: string;
    eventType: string;
    fromStepCode: string | null;
    toStepCode: string | null;
    actionCode: string | null;
    message: string | null;
    occurredAt: string;
  }>;
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

export const claimsApi = {
  search: (params: Record<string, string | number | undefined>) =>
    apiClient<PageResponse<Claim>>(`/api/v1/claims${qs(params)}`),

  get: (id: string) => apiClient<Claim>(`/api/v1/claims/${id}`),

  create: (payload: ClaimSubmitPayload) =>
    apiClient<Claim>('/api/v1/claims', { method: 'POST', body: JSON.stringify(payload) }),

  submit: (id: string) => apiClient<Claim>(`/api/v1/claims/${id}/submit`, { method: 'POST' }),

  cancel: (id: string, reason: string) =>
    apiClient<Claim>(`/api/v1/claims/${id}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    }),

  timeline: (id: string) => apiClient<ClaimTimelineEntry[]>(`/api/v1/claims/${id}/timeline`),

  listTypes: () => apiClient<ClaimType[]>('/api/v1/claim-types'),

  listEvidence: (claimId: string) =>
    apiClient<ClaimEvidence[]>(`/api/v1/claims/${claimId}/evidence`),

  addEvidence: (
    claimId: string,
    payload: {
      documentType: string;
      title: string;
      storageUri?: string;
      source?: string;
      latitude?: number;
      longitude?: number;
    }
  ) =>
    apiClient<ClaimEvidence>(`/api/v1/claims/${claimId}/evidence`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  listAssessments: (claimId: string) =>
    apiClient<ClaimAssessment[]>(`/api/v1/claims/${claimId}/assessments`),

  createAssessment: (
    claimId: string,
    payload: {
      methodsJson: string;
      findingsJson?: string;
      recommendedAmount: number;
      currency?: string;
      confidence?: number;
      notes?: string;
      accepted?: boolean;
    }
  ) =>
    apiClient<ClaimAssessment>(`/api/v1/claims/${claimId}/assessments`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  listInspections: (claimId: string) =>
    apiClient<ClaimInspection[]>(`/api/v1/claims/${claimId}/inspections`),

  createInspection: (
    claimId: string,
    payload: {
      scheduledAt?: string;
      findingsJson?: string;
      checklistJson?: string;
      notes?: string;
      complete?: boolean;
    }
  ) =>
    apiClient<ClaimInspection>(`/api/v1/claims/${claimId}/inspections`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  createDraft: (currentStep: number, payloadJson: string) =>
    apiClient<ClaimDraft>('/api/v1/claims/drafts', {
      method: 'POST',
      body: JSON.stringify({ currentStep, payloadJson })
    }),

  updateDraft: (id: string, currentStep: number, payloadJson: string) =>
    apiClient<ClaimDraft>(`/api/v1/claims/drafts/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ currentStep, payloadJson })
    }),

  submitDraft: (id: string) =>
    apiClient<Claim>(`/api/v1/claims/drafts/${id}/submit`, { method: 'POST' }),

  report: (name: 'by-status' | 'by-type') =>
    apiClient<{ reportCode: string; rows: Record<string, unknown>[]; totalCount: number; totalAmount: number }>(
      `/api/v1/claims/reports/${name}`
    ),

  getWorkflowInstance: (instanceId: string) =>
    apiClient<WorkflowInstance>(`/api/v1/workflows/instances/${instanceId}`)
};
