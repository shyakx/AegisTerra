import { apiClient } from './client';
import type { PageResponse } from './insurance';
import type { WorkflowInstance } from './claims';

export type Settlement = {
  id: string;
  settlementNumber: string;
  sourceModule: string;
  sourceRecordId: string;
  sourceReference: string | null;
  amount: number;
  currency: string;
  exchangeRate: number;
  paymentMethod: string;
  providerCode: string;
  providerReference: string | null;
  beneficiaryName: string | null;
  beneficiaryAccount: string | null;
  financialSnapshotJson: string;
  workflowInstanceId: string | null;
  workflowDefinitionCode: string | null;
  correlationId: string | null;
  reasonCode: string | null;
  failureReason: string | null;
  submittedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  status: string;
};

export type SettlementTimelineEntry = {
  kind?: string;
  fromStatus: string | null;
  toStatus: string | null;
  reason: string | null;
  actorId: string | null;
  occurredAt: string;
};

export type LedgerEntry = {
  id: string;
  ledgerTransactionId: string;
  settlementId: string | null;
  entryNo: number;
  entryType: string;
  accountCode: string;
  amount: number;
  currency: string;
  narration: string | null;
  postedAt: string;
  status: string;
  externalStatementLineId?: string | null;
  reconciliationRef?: string | null;
};

export type LedgerTransaction = {
  id: string;
  transactionNumber: string;
  settlementId: string | null;
  transactionType: string;
  description: string | null;
  currency: string;
  correlationId: string | null;
  postedAt: string;
  status: string;
  entries?: LedgerEntry[];
};

export type PaymentProvider = {
  id: string;
  providerCode: string;
  displayName: string;
  paymentMethod: string;
  enabled: boolean;
  configJson: string | null;
  status: string;
};

export type SettlementReport = {
  reportCode: string;
  rows?: Record<string, unknown>[];
  totalCount?: number;
  totalAmount?: number | null;
  pendingCount?: number;
  completedCount?: number;
  failedCount?: number;
  completedTotal?: number | null;
  byStatus?: Record<string, unknown>[];
  byProvider?: Record<string, unknown>[];
  averageSettlementHours?: number;
};

export type SettlementSearchPayload = {
  q?: string;
  status?: string;
  sourceModule?: string;
  providerCode?: string;
  paymentMethod?: string;
  fromDate?: string;
  toDate?: string;
  minAmount?: number;
  maxAmount?: number;
  page?: number;
  size?: number;
  sort?: string;
};

export type ManualConfirmPayload = {
  providerReference: string;
  notes?: string;
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

export const settlementsApi = {
  list: (params: Record<string, string | number | undefined> = {}) =>
    apiClient<PageResponse<Settlement>>(`/api/v1/settlements${qs(params)}`),

  search: (payload: SettlementSearchPayload) =>
    apiClient<PageResponse<Settlement>>('/api/v1/settlements/search', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  get: (id: string) => apiClient<Settlement>(`/api/v1/settlements/${id}`),

  timeline: (id: string) =>
    apiClient<SettlementTimelineEntry[]>(`/api/v1/settlements/${id}/timeline`),

  settlementLedger: (id: string) =>
    apiClient<LedgerEntry[]>(`/api/v1/settlements/${id}/ledger`),

  manualConfirm: (id: string, payload: ManualConfirmPayload) =>
    apiClient<Settlement>(`/api/v1/settlements/${id}/manual-confirm`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  reports: (params: Record<string, string | number | undefined> = {}) =>
    apiClient<SettlementReport | SettlementReport[]>(`/api/v1/settlements/reports${qs(params)}`),

  report: (name: string, params: Record<string, string | number | undefined> = {}) =>
    apiClient<SettlementReport>(`/api/v1/settlements/reports/${name}${qs(params)}`),

  listLedger: (params: Record<string, string | number | undefined> = {}) =>
    apiClient<PageResponse<LedgerEntry>>(`/api/v1/ledger${qs(params)}`),

  getLedger: (id: string) => apiClient<LedgerEntry | LedgerTransaction>(`/api/v1/ledger/${id}`),

  listPaymentProviders: () => apiClient<PaymentProvider[]>('/api/v1/payment-providers'),

  getWorkflowInstance: (instanceId: string) =>
    apiClient<WorkflowInstance>(`/api/v1/workflows/instances/${instanceId}`)
};
