import { apiClient } from './client';

export type CountPair = {
  total: number;
  active?: number;
  open?: number;
  pending?: number;
  completed?: number;
  failed?: number;
  approved?: number;
  rejected?: number;
  draft?: number;
  withBoundary?: number;
  pendingAmount?: number | null;
  completedAmount?: number | null;
  currency?: string | null;
};

export type ExecutiveOverview = {
  generatedAt: string;
  farmers: CountPair;
  farms: CountPair;
  policies: CountPair;
  claims: CountPair;
  settlements: CountPair;
  climate: {
    stations: number;
    recentObservations: number;
    openAlerts: number;
    criticalAlerts: number;
    farmsByRiskGrade: Record<string, number>;
  };
  tasks: { pending: number };
  notifications: { unread: number };
  regional?: Array<{ districtCode: string; meanScore: number | null; grade: string | null; farmCount?: number }>;
  quickLinks?: Array<{ label: string; path: string; permission?: string }>;
};

export const executiveApi = {
  overview: () => apiClient<ExecutiveOverview>('/api/v1/executive/overview'),
  regional: () =>
    apiClient<{ districts: Array<{ districtCode: string; meanScore: number | null; grade: string | null }> }>(
      '/api/v1/executive/regional-summary'
    )
};
