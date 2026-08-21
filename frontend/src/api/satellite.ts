import { apiClient } from './client';

export type SatelliteScene = {
  id: string;
  districtCode: string;
  capturedAt: string;
  sensor: string;
  ndvi: number;
  vegetationHealth: string;
  droughtSeverity: string;
  floodIndex: number;
  environmentalStress: string;
};

export type SatelliteSummary = {
  sceneCount: number;
  stressedDistricts: number;
  highDrought: number;
  generatedAt: string;
  scenes: SatelliteScene[];
};

export const satelliteApi = {
  summary: () => apiClient<SatelliteSummary>('/api/v1/satellite/summary')
};
