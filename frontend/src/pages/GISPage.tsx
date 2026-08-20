import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Map, NavigationControl } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { climateApi } from '../api/climate';
import { climateIntelApi } from '../api/climateIntel';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { PageHeader, PageActionLink } from '../components/PageHeader';

const LAYERS = [
  { id: 'stations', label: 'Weather stations', color: '#10b981' },
  { id: 'risk', label: 'Farm risk points', color: '#f59e0b' },
  { id: 'footprints', label: 'Satellite footprints', color: '#0ea5e9' }
] as const;

export default function GISPage() {
  const { hasPermission } = useAuth();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<Map | null>(null);
  const [visible, setVisible] = useState({ stations: true, risk: true, footprints: false });
  const [ready, setReady] = useState(false);

  const stationsQuery = useQuery({
    queryKey: ['gis-stations'],
    queryFn: () => climateApi.mapStations(),
    enabled: hasPermission('climate:read')
  });
  const footprintsQuery = useQuery({
    queryKey: ['gis-footprints'],
    queryFn: () => climateApi.mapFootprints(),
    enabled: hasPermission('climate:read') && visible.footprints
  });
  const riskQuery = useQuery({
    queryKey: ['gis-risk'],
    queryFn: () => climateIntelApi.mapRisk(),
    enabled: hasPermission('climate-intel:read')
  });

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    el.innerHTML = '';
    const map = new Map({
      container: el,
      center: [30.06, -1.94],
      zoom: 8.2,
      style: {
        version: 8,
        sources: {
          basemap: {
            type: 'raster',
            tiles: [
              'https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
              'https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
              'https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png'
            ],
            tileSize: 256,
            attribution: '© OpenStreetMap © CARTO'
          }
        },
        layers: [{ id: 'basemap', type: 'raster', source: 'basemap' }]
      }
    });
    map.addControl(new NavigationControl({ showCompass: false }), 'top-right');
    map.on('load', () => setReady(true));
    mapRef.current = map;
    return () => {
      map.remove();
      mapRef.current = null;
      setReady(false);
    };
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !ready) return;

    const syncGeoJson = (sourceId: string, layerId: string, color: string, data: GeoJSON.FeatureCollection | undefined, show: boolean) => {
      if (!data) return;
      if (map.getSource(sourceId)) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (map.getSource(sourceId) as any).setData(data);
      } else {
        map.addSource(sourceId, { type: 'geojson', data });
        map.addLayer({
          id: layerId,
          type: 'circle',
          source: sourceId,
          paint: {
            'circle-radius': 6,
            'circle-color': color,
            'circle-stroke-width': 1.5,
            'circle-stroke-color': '#ffffff'
          }
        });
      }
      if (map.getLayer(layerId)) {
        map.setLayoutProperty(layerId, 'visibility', show ? 'visible' : 'none');
      }
    };

    if (stationsQuery.data) {
      syncGeoJson('stations-src', 'stations-lyr', '#10b981', stationsQuery.data as GeoJSON.FeatureCollection, visible.stations);
    }
    if (riskQuery.data) {
      // Risk features may lack geometry — filter to those with Point geometry
      const withGeom: GeoJSON.FeatureCollection = {
        type: 'FeatureCollection',
        features: (riskQuery.data.features ?? []).filter(
          (f) => f.geometry && (f.geometry as { type?: string }).type === 'Point'
        ) as GeoJSON.Feature[]
      };
      syncGeoJson('risk-src', 'risk-lyr', '#f59e0b', withGeom, visible.risk);
    }
    if (footprintsQuery.data && visible.footprints) {
      syncGeoJson(
        'foot-src',
        'foot-lyr',
        '#0ea5e9',
        footprintsQuery.data as GeoJSON.FeatureCollection,
        visible.footprints
      );
    }
  }, [ready, stationsQuery.data, riskQuery.data, footprintsQuery.data, visible]);

  const error =
    stationsQuery.error instanceof ApiError
      ? stationsQuery.error.message
      : riskQuery.error instanceof ApiError
        ? riskQuery.error.message
        : null;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="GIS operations"
        title="National spatial workspace"
        description="Weather stations, climate risk overlays, and farm boundary editing entry points."
        actions={
          <>
            <PageActionLink to="/farms">Farms</PageActionLink>
            <PageActionLink to="/climate/map">Climate map</PageActionLink>
            <PageActionLink to="/climate-intel" variant="primary">
              Risk dashboard
            </PageActionLink>
          </>
        }
      />

      {error ? (
        <p className="text-sm text-danger" role="alert">
          {error}
        </p>
      ) : null}

      <div className="grid gap-4 lg:grid-cols-[220px_1fr]">
        <aside className="rounded-2xl border border-border bg-surface p-4 shadow-sm">
          <p className="text-sm font-semibold">Layers</p>
          <ul className="mt-3 space-y-2">
            {LAYERS.map((layer) => (
              <li key={layer.id}>
                <label className="flex cursor-pointer items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={visible[layer.id]}
                    onChange={(e) => setVisible((v) => ({ ...v, [layer.id]: e.target.checked }))}
                  />
                  <span className="inline-block h-2.5 w-2.5 rounded-full" style={{ background: layer.color }} />
                  {layer.label}
                </label>
              </li>
            ))}
          </ul>
          <div className="mt-6 border-t border-border pt-4 text-sm text-textSecondary">
            <p className="font-medium text-textPrimary">Legend</p>
            <p className="mt-2">Station points use Climate Data GeoJSON.</p>
            <p className="mt-1">
              Edit farm polygons from a farm detail →{' '}
              <Link className="text-primary hover:underline" to="/farms">
                Boundary editor
              </Link>
              .
            </p>
          </div>
        </aside>
        <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-sm">
          <div ref={containerRef} className="h-[560px] w-full" role="img" aria-label="National GIS map" />
        </div>
      </div>
    </div>
  );
}
