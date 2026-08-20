import { apiClient } from './client';
import type { PageResponse } from './insurance';

export type ClimateProvider = {
  id: string;
  code: string;
  displayName: string;
  enabled: boolean;
  capabilities: string | null;
  status: string;
};

export type WeatherStation = {
  id: string;
  code: string;
  name: string;
  elevationM: number | null;
  providerCode: string | null;
  externalStationId: string | null;
  districtCode: string | null;
  longitude: number | null;
  latitude: number | null;
  status: string;
};

export type WeatherObservation = {
  id: string;
  stationId: string;
  observedAt: string;
  variableCode: string | null;
  value: number | null;
  unit: string | null;
  qualityFlag: string | null;
  providerCode: string | null;
  temperatureC: number | null;
  rainfallMm: number | null;
  humidityPct: number | null;
  windSpeedMs: number | null;
};

export type ClimateImportJob = {
  id: string;
  jobNumber: string;
  providerCode: string;
  jobType: string;
  status: string;
  rowsRead: number;
  rowsAccepted: number;
  rowsRejected: number;
  startedAt: string | null;
  completedAt: string | null;
  errorSummary: string | null;
  createdAt: string;
};

export type ClimateDataset = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  datasetType: string;
  providerCode: string | null;
  status: string;
  timeStart: string | null;
  timeEnd: string | null;
};

export type ClimateQualityReport = {
  id: string;
  scopeType: string;
  scopeId: string | null;
  periodStart: string | null;
  periodEnd: string | null;
  metricsJson: string;
  overallGrade: string | null;
  generatedAt: string;
};

export type ClimateRasterLayer = {
  id: string;
  code: string;
  name: string;
  providerCode: string | null;
  variableCode: string | null;
  storageUri: string | null;
  format: string | null;
  status: string;
};

export type SatelliteObservation = {
  id: string;
  farmId: string | null;
  productId: string;
  productVersion: string | null;
  observedAt: string;
  cloudCoverPct: number | null;
  storageUri: string | null;
  providerCode: string | null;
  qualityFlag: string | null;
};

export type GeoJsonFeatureCollection = {
  type: 'FeatureCollection';
  features: Array<{
    type: 'Feature';
    geometry: unknown;
    properties: Record<string, unknown>;
  }>;
};

export type ClimateDashboard = {
  providerCount: number;
  enabledProviderCount: number;
  stationCount: number;
  observationCountRecent: number;
  openImportJobs: number;
  latestQualityGrade: string | null;
};

function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const encoded = search.toString();
  return encoded ? `?${encoded}` : '';
}

export const climateApi = {
  dashboard: () => apiClient<ClimateDashboard>('/api/v1/climate/dashboard'),

  listProviders: () => apiClient<ClimateProvider[]>('/api/v1/climate/providers'),

  listStations: (params: { q?: string; status?: string; page?: number; size?: number } = {}) =>
    apiClient<PageResponse<WeatherStation>>(`/api/v1/climate/stations${qs(params)}`),

  getStation: (id: string) => apiClient<WeatherStation>(`/api/v1/climate/stations/${id}`),

  createStation: (body: {
    code: string;
    name: string;
    longitude: number;
    latitude: number;
    elevationM?: number;
    providerCode?: string;
    externalStationId?: string;
    districtCode?: string;
  }) =>
    apiClient<WeatherStation>('/api/v1/climate/stations', {
      method: 'POST',
      body: JSON.stringify(body)
    }),

  listObservations: (params: {
    stationId?: string;
    from: string;
    to: string;
    variableCode?: string;
    qualityFlag?: string;
    page?: number;
    size?: number;
  }) => apiClient<PageResponse<WeatherObservation>>(`/api/v1/climate/observations${qs(params)}`),

  listSatellite: (params: { from?: string; to?: string; farmId?: string; page?: number; size?: number } = {}) =>
    apiClient<PageResponse<SatelliteObservation>>(`/api/v1/climate/satellite-observations${qs(params)}`),

  listRasters: (params: { page?: number; size?: number } = {}) =>
    apiClient<PageResponse<ClimateRasterLayer>>(`/api/v1/climate/raster-layers${qs(params)}`),

  listDatasets: (params: { page?: number; size?: number; status?: string } = {}) =>
    apiClient<PageResponse<ClimateDataset>>(`/api/v1/climate/datasets${qs(params)}`),

  getDataset: (id: string) => apiClient<ClimateDataset>(`/api/v1/climate/datasets/${id}`),

  listImportJobs: (params: { page?: number; size?: number; status?: string } = {}) =>
    apiClient<PageResponse<ClimateImportJob>>(`/api/v1/climate/import-jobs${qs(params)}`),

  getImportJob: (id: string) => apiClient<ClimateImportJob>(`/api/v1/climate/import-jobs/${id}`),

  startImportJob: (body: {
    providerCode: string;
    jobType?: string;
    stationCode?: string;
    observations?: Array<{
      observedAt: string;
      variableCode: string;
      value: number;
      unit?: string;
    }>;
    csvContent?: string;
  }) =>
    apiClient<ClimateImportJob>('/api/v1/climate/import-jobs', {
      method: 'POST',
      body: JSON.stringify(body)
    }),

  listQualityReports: (params: { page?: number; size?: number } = {}) =>
    apiClient<PageResponse<ClimateQualityReport>>(`/api/v1/climate/quality-reports${qs(params)}`),

  mapStations: () => apiClient<GeoJsonFeatureCollection>('/api/v1/climate/map/stations'),

  mapFootprints: () => apiClient<GeoJsonFeatureCollection>('/api/v1/climate/map/footprints'),

  report: (name: string) => apiClient<Record<string, unknown>>(`/api/v1/climate/reports/${name}`)
};
