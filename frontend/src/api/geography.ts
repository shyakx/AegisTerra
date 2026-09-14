import { apiClient } from './client';

export type Province = {
  id: string;
  code: string;
  name: string;
  status: string;
};

export type AgroecologicalZone = {
  id: string;
  code: string;
  name: string;
  status: string;
};

export type AgroecologicalSubzone = {
  id: string;
  code: string;
  name: string;
  status: string;
  zoneId: string;
  zoneCode: string | null;
  zoneName: string | null;
};

export type District = {
  id: string;
  code: string;
  name: string;
  status: string;
  provinceId: string | null;
  provinceCode: string | null;
  provinceName: string | null;
  agroecologicalSubzoneId: string | null;
  agroecologicalSubzoneCode: string | null;
  agroecologicalSubzoneName: string | null;
  agroecologicalZoneId: string | null;
  agroecologicalZoneCode: string | null;
  agroecologicalZoneName: string | null;
};

function qs(params: Record<string, string | undefined | null>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.length > 0) {
      search.set(key, String(value));
    }
  });
  const value = search.toString();
  return value ? `?${value}` : '';
}

export const geographyApi = {
  listProvinces: () => apiClient<Province[]>('/api/v1/provinces'),
  getProvince: (id: string) => apiClient<Province>(`/api/v1/provinces/${id}`),
  listDistricts: (provinceId?: string | null) =>
    apiClient<District[]>(`/api/v1/districts${qs({ provinceId })}`),
  getDistrict: (id: string) => apiClient<District>(`/api/v1/districts/${id}`),
  listAgroecologicalZones: () => apiClient<AgroecologicalZone[]>('/api/v1/agroecological-zones'),
  getAgroecologicalZone: (id: string) =>
    apiClient<AgroecologicalZone>(`/api/v1/agroecological-zones/${id}`),
  listAgroecologicalSubzones: (params?: { zoneId?: string; districtId?: string }) =>
    apiClient<AgroecologicalSubzone[]>(
      `/api/v1/agroecological-subzones${qs({
        zoneId: params?.zoneId,
        districtId: params?.districtId
      })}`
    ),
  getAgroecologicalSubzone: (id: string) =>
    apiClient<AgroecologicalSubzone>(`/api/v1/agroecological-subzones/${id}`)
};
