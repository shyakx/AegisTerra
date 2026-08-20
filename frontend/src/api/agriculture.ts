import { apiClient } from './client';

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Farmer = {
  id: string;
  farmerCode: string | null;
  householdId: string | null;
  firstName: string;
  lastName: string;
  nationalId: string;
  phoneNumber: string;
  email: string | null;
  districtId: string | null;
  villageId: string | null;
  status: string;
};

export type Household = {
  id: string;
  code: string;
  headName: string | null;
  status: string;
};

export type Farm = {
  id: string;
  farmerId: string;
  farmCode: string;
  farmName: string;
  farmSizeHa: number | null;
  cropType: string | null;
  status: string;
};

export type FarmBoundary = {
  id: string;
  farmId: string;
  geoJson: string | null;
  source: string | null;
  areaHa: number | null;
  status: string;
};

export type Plot = {
  id: string;
  farmId: string;
  plotCode: string;
  name: string | null;
  geoJson: string | null;
  areaHa: number | null;
  status: string;
};

export type Crop = {
  id: string;
  code: string;
  name: string;
  scientificName: string | null;
  status: string;
};

export type Season = {
  id: string;
  code: string;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
};

export type CropSeason = {
  id: string;
  farmId: string;
  plotId: string | null;
  cropId: string;
  seasonId: string;
  plantedAreaHa: number | null;
  status: string;
};

export type RegistrationDraft = {
  id: string;
  currentStep: number;
  payloadJson: string;
  status: string;
  expiresAt: string | null;
  farmerId: string | null;
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

export const agriApi = {
  searchFarmers: (params: Record<string, string | number | undefined>) =>
    apiClient<PageResponse<Farmer>>(`/api/v1/farmers${qs(params)}`),
  getFarmer: (id: string) => apiClient<Farmer>(`/api/v1/farmers/${id}`),
  createFarmer: (body: unknown) =>
    apiClient<Farmer>('/api/v1/farmers', { method: 'POST', body: JSON.stringify(body) }),
  exportFarmers: async (params: Record<string, string | number | undefined>) => {
    const data = await apiClient<string>(`/api/v1/farmers/export${qs(params)}`);
    return data;
  },

  searchHouseholds: (params: Record<string, string | number | undefined>) =>
    apiClient<PageResponse<Household>>(`/api/v1/households${qs(params)}`),
  createHousehold: (body: unknown) =>
    apiClient<Household>('/api/v1/households', { method: 'POST', body: JSON.stringify(body) }),

  searchFarms: (params: Record<string, string | number | undefined>) =>
    apiClient<PageResponse<Farm>>(`/api/v1/farms${qs(params)}`),
  getFarm: (id: string) => apiClient<Farm>(`/api/v1/farms/${id}`),
  createFarm: (body: unknown) =>
    apiClient<Farm>('/api/v1/farms', { method: 'POST', body: JSON.stringify(body) }),
  exportFarms: (params: Record<string, string | number | undefined>) =>
    apiClient<string>(`/api/v1/farms/export${qs(params)}`),

  listBoundaries: (farmId: string) =>
    apiClient<FarmBoundary[]>(`/api/v1/farm-boundaries${qs({ farmId })}`),
  validateGeometry: (geoJson: string) =>
    apiClient<{ valid: boolean; reason: string | null; areaHa: number | null }>(
      '/api/v1/farm-boundaries/validate',
      { method: 'POST', body: JSON.stringify({ geoJson }) }
    ),
  saveBoundary: (body: unknown) =>
    apiClient<FarmBoundary>('/api/v1/farm-boundaries', { method: 'POST', body: JSON.stringify(body) }),
  updateBoundary: (id: string, body: unknown) =>
    apiClient<FarmBoundary>(`/api/v1/farm-boundaries/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body)
    }),

  listPlots: (farmId: string) => apiClient<Plot[]>(`/api/v1/plots${qs({ farmId })}`),
  createPlot: (body: unknown) =>
    apiClient<Plot>('/api/v1/plots', { method: 'POST', body: JSON.stringify(body) }),
  deletePlot: (id: string) => apiClient<void>(`/api/v1/plots/${id}`, { method: 'DELETE' }),

  listCrops: () => apiClient<Crop[]>('/api/v1/crops'),
  createCrop: (body: unknown) =>
    apiClient<Crop>('/api/v1/crops', { method: 'POST', body: JSON.stringify(body) }),

  listSeasons: () => apiClient<Season[]>('/api/v1/seasons'),
  createSeason: (body: unknown) =>
    apiClient<Season>('/api/v1/seasons', { method: 'POST', body: JSON.stringify(body) }),

  listCropSeasons: (farmId: string) =>
    apiClient<CropSeason[]>(`/api/v1/crop-seasons${qs({ farmId })}`),
  createCropSeason: (body: unknown) =>
    apiClient<CropSeason>('/api/v1/crop-seasons', { method: 'POST', body: JSON.stringify(body) }),

  listDrafts: () => apiClient<RegistrationDraft[]>('/api/v1/registration-drafts'),
  getDraft: (id: string) => apiClient<RegistrationDraft>(`/api/v1/registration-drafts/${id}`),
  createDraft: (body: { currentStep: number; payloadJson: string }) =>
    apiClient<RegistrationDraft>('/api/v1/registration-drafts', {
      method: 'POST',
      body: JSON.stringify(body)
    }),
  updateDraft: (id: string, body: { currentStep: number; payloadJson: string }) =>
    apiClient<RegistrationDraft>(`/api/v1/registration-drafts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body)
    }),
  submitDraft: (id: string) =>
    apiClient<{ draftId: string; farmerId: string; farmId: string; status: string }>(
      `/api/v1/registration-drafts/${id}/submit`,
      { method: 'POST' }
    )
};

export function downloadCsv(filename: string, content: string) {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}
