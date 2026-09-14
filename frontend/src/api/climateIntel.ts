import { apiClient } from './client';
import type { PageResponse } from './insurance';
import type { GeoJsonFeatureCollection } from './climate';

export type FarmRiskScore = {
  id: string;
  farmId: string;
  score: number;
  confidence: number | null;
  grade: string | null;
  windowStart: string | null;
  windowEnd: string | null;
  componentsJson: string | null;
  ruleSetCode: string | null;
  ruleVersion: string | null;
  modelVersion: string;
  calculatedAt: string;
};

export type FarmClimateProfile = {
  id: string;
  farmId: string;
  generatedAt: string;
  ruleVersion: string | null;
  snapshotJson: string;
  latestRiskScore: number | null;
  latestRiskGrade: string | null;
};

export type ClimateTimelineItem = {
  occurredAt: string;
  eventType: string;
  severity: string | null;
  title: string;
  detail: string | null;
  evidenceJson: string | null;
};

export type WeatherSummary = {
  farmId: string;
  from: string;
  to: string;
  rainfallTotalMm: number | null;
  meanTempC: number | null;
  dryDays: number | null;
  wetDays: number | null;
  narrative: string;
  metricsJson: string | null;
};

export type SeasonSummary = {
  farmId: string;
  seasonCode: string | null;
  grade: string | null;
  metricsJson: string;
  narrative: string | null;
  generatedAt: string;
};

export type AezZoneHeat = {
  code: string;
  name: string;
  meanScore: number | null;
  grade: string | null;
  districtCount: number;
  farmCount: number;
};

export type AezSubzoneHeat = {
  code: string;
  name: string;
  zoneCode: string;
  zoneName: string;
  meanScore: number | null;
  grade: string | null;
  districtCount: number;
  farmCount: number;
};

export type AezRiskSummary = {
  zones: AezZoneHeat[];
  subzones: AezSubzoneHeat[];
  unmappedDistrictCodes: string[];
  unmappedCount: number;
  generatedAt: string;
};

export type YieldYearPoint = {
  year: number;
  meanYieldTHa: number | null;
  sampleCount: number;
};

export type CropYieldOutlook = {
  cropCode: string;
  cropName: string;
  pastMeanYieldTHa: number | null;
  recentMeanYieldTHa: number | null;
  predictedYieldTHa: number | null;
  yieldChangePctRecentVsPast: number | null;
  outlookLabel: string;
  narrative: string;
  farmCount: number;
  seasonSampleCount: number;
  yearlySeries: YieldYearPoint[];
};

export type YieldClimateOutlook = {
  referenceYear: number;
  pastWindowStartYear: number;
  pastWindowEndYear: number;
  recentWindowStartYear: number;
  recentWindowEndYear: number;
  nationalMeanRiskScore: number | null;
  nationalRiskGrade: string | null;
  crops: CropYieldOutlook[];
  methodology: string;
  generatedAt: string;
};

export type MaizeMlFeatureRow = {
  harvestYear: number;
  meanYieldTHa: number;
  sampleCount: number;
  farmCount: number;
  lag1YieldTHa: number | null;
  lag2YieldTHa: number | null;
  seasonRainMm: number | null;
  yearIndex: number | null;
};

export type MaizeMlSpike = {
  cropCode: string;
  featureRowCount: number;
  features: MaizeMlFeatureRow[];
  leaveOneYearOut: Array<{
    holdoutYear: number;
    actualYieldTHa: number;
    naiveLastYearPred: number | null;
    ruleRecentMeanPred: number | null;
    linearLagRainPred: number | null;
    absErrorNaive: number | null;
    absErrorRule: number | null;
    absErrorLinear: number | null;
  }>;
  metrics: {
    folds: number;
    maeNaiveLastYear: number | null;
    maeRuleRecentMean: number | null;
    maeLinearLagRain: number | null;
    bestMethod: string;
    linearBeatsRule: boolean;
  };
  verdict: string;
  notes: string;
  generatedAt: string;
};

export type DistrictRiskProfile = {
  districtCode: string;
  meanScore: number | null;
  p90Score: number | null;
  farmCount: number;
  openAlertCount: number;
  grade: string | null;
  metricsJson: string | null;
  generatedAt: string;
  provinceCode?: string | null;
  provinceName?: string | null;
  agroecologicalZoneCode?: string | null;
  agroecologicalZoneName?: string | null;
  agroecologicalSubzoneCode?: string | null;
  agroecologicalSubzoneName?: string | null;
};

export type NationalRiskDashboard = {
  farmsByGrade: Record<string, number>;
  openCriticalAlerts: number;
  openAlerts: number;
  districtHeat: Array<{ districtCode: string; meanScore: number | null; grade: string | null }>;
  dataCoveragePct: number | null;
  generatedAt: string;
  zoneHeat?: AezZoneHeat[];
  subzoneHeat?: AezSubzoneHeat[];
  unmappedDistrictCodes?: string[];
  unmappedCount?: number;
};

export type ClimateAlert = {
  id: string;
  alertNumber: string;
  alertType: string;
  severity: string;
  scopeType: string;
  scopeId: string | null;
  validFrom: string;
  validTo: string | null;
  ruleVersion: string | null;
  evidenceJson: string | null;
  status: string;
  createdAt: string;
};

export type ClimateIndicator = {
  id: string;
  indicatorType: string;
  subjectType: string;
  subjectId: string | null;
  severity: string | null;
  indexValue: number | null;
  windowStart: string | null;
  windowEnd: string | null;
  detailsJson: string | null;
  calculatedAt: string;
};

export type ClimateIntelJob = {
  id: string;
  jobNumber: string;
  jobType: string;
  status: string;
  subjectsProcessed: number;
  errorSummary: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
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

export const climateIntelApi = {
  nationalDashboard: () =>
    apiClient<NationalRiskDashboard>('/api/v1/climate-intel/national/dashboard'),

  aezRiskSummary: () =>
    apiClient<AezRiskSummary>('/api/v1/climate-intel/aez/risk-summary'),

  aezZoneRiskSummary: () =>
    apiClient<AezZoneHeat[]>('/api/v1/climate-intel/aez/zones/risk-summary'),

  aezSubzoneRiskSummary: () =>
    apiClient<AezSubzoneHeat[]>('/api/v1/climate-intel/aez/subzones/risk-summary'),

  yieldClimateOutlook: () =>
    apiClient<YieldClimateOutlook>('/api/v1/climate-intel/planning/yield-outlook'),

  yieldClimateOutlookCsvUrl: () => '/api/v1/climate-intel/planning/yield-outlook.csv',

  maizeYieldMlSpike: () =>
    apiClient<MaizeMlSpike>('/api/v1/climate-intel/planning/ml-spike/maize'),

  farmRiskScore: (farmId: string, params: { from?: string; to?: string } = {}) =>
    apiClient<FarmRiskScore>(`/api/v1/climate-intel/farms/${farmId}/risk-score${qs(params)}`),

  farmProfile: (farmId: string) =>
    apiClient<FarmClimateProfile>(`/api/v1/climate-intel/farms/${farmId}/profile`),

  farmTimeline: (farmId: string, params: { from?: string; to?: string } = {}) =>
    apiClient<ClimateTimelineItem[]>(`/api/v1/climate-intel/farms/${farmId}/timeline${qs(params)}`),

  weatherSummary: (farmId: string, params: { from: string; to: string }) =>
    apiClient<WeatherSummary>(`/api/v1/climate-intel/farms/${farmId}/weather-summary${qs(params)}`),

  seasonSummary: (farmId: string, params: { seasonCode?: string } = {}) =>
    apiClient<SeasonSummary>(`/api/v1/climate-intel/farms/${farmId}/season-summary${qs(params)}`),

  districtRiskProfile: (code: string) =>
    apiClient<DistrictRiskProfile>(`/api/v1/climate-intel/districts/${encodeURIComponent(code)}/risk-profile`),

  listAlerts: (params: {
    status?: string;
    alertType?: string;
    severity?: string;
    scopeType?: string;
    page?: number;
    size?: number;
  } = {}) => apiClient<PageResponse<ClimateAlert>>(`/api/v1/climate-intel/alerts${qs(params)}`),

  getAlert: (id: string) => apiClient<ClimateAlert>(`/api/v1/climate-intel/alerts/${id}`),

  acknowledgeAlert: (id: string) =>
    apiClient<ClimateAlert>(`/api/v1/climate-intel/alerts/${id}/acknowledge`, { method: 'POST' }),

  recalculate: (body: { farmId?: string; districtCode?: string; from?: string; to?: string } = {}) =>
    apiClient<ClimateIntelJob>('/api/v1/climate-intel/jobs/recalculate', {
      method: 'POST',
      body: JSON.stringify(body)
    }),

  listIndicators: (params: {
    indicatorType?: string;
    subjectType?: string;
    subjectId?: string;
    page?: number;
    size?: number;
  } = {}) => apiClient<PageResponse<ClimateIndicator>>(`/api/v1/climate-intel/indicators${qs(params)}`),

  mapRisk: () => apiClient<GeoJsonFeatureCollection>('/api/v1/climate-intel/map/risk'),

  report: (name: string) => apiClient<Record<string, unknown>>(`/api/v1/climate-intel/reports/${name}`)
};
