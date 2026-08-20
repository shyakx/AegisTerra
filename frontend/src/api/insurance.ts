import { apiClient } from './client';
import { downloadCsv } from './agriculture';

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type InsuranceProduct = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  pricingStrategyCode: string;
  status: string;
};

export type CoveragePackage = {
  id: string;
  productId: string;
  policyTypeId: string | null;
  code: string;
  name: string;
  coverageLevelPct: number | null;
  maxSumInsured: number | null;
  status: string;
};

export type PolicyType = {
  id: string;
  code: string;
  name: string;
  productId: string | null;
};

export type PremiumQuote = {
  id: string;
  productId: string;
  coveragePackageId: string;
  netAmount: number;
  grossAmount: number;
  baseAmount: number;
  coverageAmount: number;
  currency: string;
  breakdownJson: string;
  expiresAt: string;
};

export type Policy = {
  id: string;
  policyNumber: string;
  farmerId: string;
  farmId: string;
  policyTypeId: string | null;
  productId: string | null;
  coveragePackageId: string | null;
  coverageAmount: number;
  premiumAmount: number;
  currency: string;
  startDate: string;
  endDate: string;
  status: string;
  transitionReason: string | null;
};

export type PolicyDocument = {
  id: string;
  documentType: string;
  versionNo: number;
  contentText: string;
  qrPayload: string | null;
  signatureStatus: string;
};

export type InsuranceReport = {
  reportCode: string;
  rows: Record<string, unknown>[];
  totalCount: number;
  totalAmount: number | null;
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

async function transition(id: string, action: string, reason: string) {
  return apiClient<Policy>(`/api/v1/policies/${id}/${action}`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  });
}

export const insuranceApi = {
  listProducts: () => apiClient<InsuranceProduct[]>('/api/v1/insurance-products'),
  listPackages: (productId: string) =>
    apiClient<CoveragePackage[]>(`/api/v1/coverage-packages${qs({ productId })}`),
  listPolicyTypes: () => apiClient<PolicyType[]>('/api/v1/policy-types'),

  quote: (body: unknown) =>
    apiClient<PremiumQuote>('/api/v1/premiums/quote', { method: 'POST', body: JSON.stringify(body) }),
  getQuote: (id: string) => apiClient<PremiumQuote>(`/api/v1/premium-quotes/${id}`),

  searchPolicies: (params: Record<string, string | number | undefined>) =>
    apiClient<PageResponse<Policy>>(`/api/v1/policies${qs(params)}`),
  getPolicy: (id: string) => apiClient<Policy>(`/api/v1/policies/${id}`),
  submitPolicy: (body: unknown) =>
    apiClient<Policy>('/api/v1/policies', { method: 'POST', body: JSON.stringify(body) }),

  approve: (id: string, reason = 'approved') => transition(id, 'approve', reason),
  reject: (id: string, reason: string) => transition(id, 'reject', reason),
  markPaid: (id: string, reason = 'paid') => transition(id, 'mark-premium-paid', reason),
  activate: (id: string, reason = 'activated') => transition(id, 'activate', reason),
  suspend: (id: string, reason: string) => transition(id, 'suspend', reason),
  reinstate: (id: string, reason = 'reinstated') => transition(id, 'reinstate', reason),
  cancel: (id: string, reason: string) => transition(id, 'cancel', reason),
  renew: (id: string, reason = 'renewal') => transition(id, 'renew', reason),

  listDocuments: (id: string) => apiClient<PolicyDocument[]>(`/api/v1/policies/${id}/documents`),
  regenerateDocuments: (id: string, documentType: string) =>
    apiClient<PolicyDocument>(
      `/api/v1/policies/${id}/documents/regenerate?documentType=${encodeURIComponent(documentType)}`,
      { method: 'POST' }
    ),

  exportPolicies: async (params: Record<string, string | undefined>) => {
    const csv = await apiClient<string>(`/api/v1/policies/export${qs(params)}`);
    downloadCsv('policies.csv', csv);
  },

  report: (path: string) => apiClient<InsuranceReport>(`/api/v1/insurance/reports/${path}`)
};

export { downloadCsv };
