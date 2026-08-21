import { apiClient } from './client';
import type { PageResponse } from './agriculture';

export type InsuredInputSale = {
  id: string;
  receiptNumber: string;
  aggregatorName: string;
  farmerId: string;
  farmerName: string;
  farmerCode: string;
  productType: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  premiumEmbedded: number;
  currency: string;
  soldAt: string;
  status: string;
};

export type FarmerRecommendation = {
  id: string;
  farmId: string;
  farmerId: string;
  title: string;
  body: string;
  kind: string;
  severity: string;
  generatedAt: string;
};

export const aggregatorApi = {
  listSales: (params: Record<string, string | number | undefined> = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v != null && v !== '') qs.set(k, String(v));
    });
    const suffix = qs.toString() ? `?${qs}` : '';
    return apiClient<PageResponse<InsuredInputSale>>(`/api/v1/insured-inputs${suffix}`);
  }
};

export const guidanceApi = {
  list: () => apiClient<FarmerRecommendation[]>('/api/v1/recommendations')
};
