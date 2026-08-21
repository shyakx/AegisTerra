import { apiClient } from './client';
import type { PageResponse } from './agriculture';

export type AgriculturalLoan = {
  id: string;
  loanNumber: string;
  farmerId: string;
  farmId: string;
  policyId: string | null;
  borrowerName: string;
  farmerCode: string;
  districtId: string;
  lenderName: string;
  purpose: string;
  principal: number;
  currency: string;
  insuranceEmbedPct: number;
  status: string;
  repaymentRisk: string;
  cropCondition: string;
  disbursedAt: string | null;
  maturityAt: string;
  createdAt: string;
};

export const lendingApi = {
  search: (params: Record<string, string | number | undefined> = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v != null && v !== '') qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs}` : '';
    return apiClient<PageResponse<AgriculturalLoan>>(`/api/v1/loans${suffix}`);
  },
  get: (id: string) => apiClient<AgriculturalLoan>(`/api/v1/loans/${id}`)
};
