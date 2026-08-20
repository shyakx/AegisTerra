import { Map, Marker, NavigationControl } from 'maplibre-gl';
import { useEffect, useRef, useState } from 'react';
import type { Polygon, Position } from 'geojson';
import 'maplibre-gl/dist/maplibre-gl.css';
import { agriApi } from '../api/agriculture';

type Props = {
  initialGeoJson?: string | null;
  readOnly?: boolean;
  onChange: (geoJson: string | null, areaHa: number | null, valid: boolean, reason: string | null) => void;
};

type Point = { lng: number; lat: number };

function ringToPolygon(ring: Point[]): Polygon {
  const coords: Position[] = ring.map((p) => [p.lng, p.lat]);
  const first = coords[0];
  const last = coords[coords.length - 1];
  if (first[0] !== last[0] || first[1] !== last[1]) {
    coords.push([...first]);
  }
  return { type: 'Polygon', coordinates: [coords] };
}

function parseInitial(geoJson: string | null | undefined): Point[] {
  if (!geoJson) return [];
  try {
    const geometry = JSON.parse(geoJson) as Polygon;
    if (geometry.type !== 'Polygon' || !geometry.coordinates?.[0]?.length) return [];
    const ring = geometry.coordinates[0];
    const open =
      ring.length > 1 &&
      ring[0][0] === ring[ring.length - 1][0] &&
      ring[0][1] === ring[ring.length - 1][1]
        ? ring.slice(0, -1)
        : ring;
    return open.map(([lng, lat]) => ({ lng, lat }));
  } catch {
    return [];
  }
}

export default function BoundaryMapEditor({ initialGeoJson, readOnly = false, onChange }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<Map | null>(null);
  const markersRef = useRef<Marker[]>([]);
  const pointsRef = useRef<Point[]>([]);
  const [points, setPoints] = useState<Point[]>(() => parseInitial(initialGeoJson));
  const [complete, setComplete] = useState(() => parseInitial(initialGeoJson).length >= 3);
  const [mapReady, setMapReady] = useState(false);
  const [areaHa, setAreaHa] = useState<number | null>(null);
  const [validation, setValidation] = useState<string | null>(null);
  const [status, setStatus] = useState('Click the map to place boundary corners');

  pointsRef.current = points;

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    container.innerHTML = '';
    setMapReady(false);

    // Carto tiles send CORS headers (OSM tile CDN does not with MapLibre fetch).
    const map = new Map({
      container,
      maxZoom: 18,
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
            attribution: '© OpenStreetMap © CARTO',
            maxzoom: 18
          },
          draft: {
            type: 'geojson',
            data: { type: 'FeatureCollection', features: [] }
          }
        },
        layers: [
          { id: 'basemap', type: 'raster', source: 'basemap' },
          {
            id: 'draft-fill',
            type: 'fill',
            source: 'draft',
            paint: { 'fill-color': '#0f766e', 'fill-opacity': 0.25 },
            layout: { visibility: 'none' }
          },
          {
            id: 'draft-line',
            type: 'line',
            source: 'draft',
            paint: { 'line-color': '#0f766e', 'line-width': 2 }
          }
        ]
      },
      center: [30.06, -1.95],
      zoom: 13
    });

    map.addControl(new NavigationControl(), 'top-right');
    mapRef.current = map;

    const addPoint = (lng: number, lat: number) => {
      if (readOnly) return;
      const next = [...pointsRef.current, { lng, lat }];
      setComplete(false);
      setPoints(next);
      setStatus(
        next.length < 3
          ? `Corner ${next.length} placed — need at least 3`
          : `${next.length} corners — click Complete polygon when ready`
      );
    };

    // Capture-phase listener on the container — does not depend on MapLibre's click event.
    const onContainerClick = (event: MouseEvent) => {
      if (readOnly || event.button !== 0) return;
      const target = event.target as HTMLElement | null;
      if (target?.closest('.maplibregl-ctrl, .maplibregl-ctrl-attrib, button, a')) return;

      const rect = container.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      if (x < 0 || y < 0 || x > rect.width || y > rect.height) return;

      const lngLat = map.unproject([x, y]);
      addPoint(lngLat.lng, lngLat.lat);
    };

    if (!readOnly) {
      container.addEventListener('click', onContainerClick, true);
    }

    const markReady = () => {
      map.resize();
      if (!readOnly) {
        map.getCanvas().style.cursor = 'crosshair';
      }
      setMapReady(true);
    };

    if (map.loaded()) {
      markReady();
    } else {
      map.once('load', markReady);
    }

    const resizeTimer = window.setTimeout(() => map.resize(), 150);

    return () => {
      window.clearTimeout(resizeTimer);
      if (!readOnly) {
        container.removeEventListener('click', onContainerClick, true);
      }
      markersRef.current.forEach((m) => m.remove());
      markersRef.current = [];
      map.remove();
      mapRef.current = null;
      container.innerHTML = '';
    };
  }, [readOnly]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady) return;

    markersRef.current.forEach((m) => m.remove());
    markersRef.current = points.map((p, index) => {
      const el = document.createElement('div');
      el.className =
        'flex h-6 w-6 items-center justify-center rounded-full border-2 border-white bg-teal-700 text-[10px] font-semibold text-white shadow';
      el.textContent = String(index + 1);
      return new Marker({ element: el }).setLngLat([p.lng, p.lat]).addTo(map);
    });

    const source = map.getSource('draft') as { setData: (data: unknown) => void } | undefined;
    if (!source) return;

    if (complete && points.length >= 3) {
      map.setLayoutProperty('draft-fill', 'visibility', 'visible');
      source.setData({
        type: 'FeatureCollection',
        features: [{ type: 'Feature', properties: {}, geometry: ringToPolygon(points) }]
      });
      return;
    }

    map.setLayoutProperty('draft-fill', 'visibility', 'none');

    if (points.length < 2) {
      source.setData({ type: 'FeatureCollection', features: [] });
      return;
    }

    source.setData({
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates: points.map((p) => [p.lng, p.lat])
          }
        }
      ]
    });
  }, [points, complete, mapReady]);

  useEffect(() => {
    if (!complete || points.length < 3) {
      if (!complete) {
        setAreaHa(null);
        setValidation(null);
        onChange(null, null, false, points.length ? 'Complete the polygon to continue' : 'Draw a farm boundary polygon');
      }
      return;
    }

    const geoJson = JSON.stringify(ringToPolygon(points));
    let cancelled = false;
    void (async () => {
      try {
        const result = await agriApi.validateGeometry(geoJson);
        if (cancelled) return;
        setAreaHa(result.areaHa);
        setValidation(result.valid ? null : result.reason);
        onChange(geoJson, result.areaHa, result.valid, result.reason);
        setStatus(result.valid ? 'Boundary ready' : result.reason ?? 'Invalid geometry');
      } catch {
        if (cancelled) return;
        setValidation('Unable to validate geometry');
        onChange(geoJson, null, false, 'Unable to validate geometry');
        setStatus('Unable to validate geometry');
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [complete, points]);

  function completePolygon() {
    if (points.length < 3) {
      setStatus('Place at least 3 corners before completing');
      return;
    }
    setComplete(true);
  }

  function undoPoint() {
    if (readOnly) return;
    setComplete(false);
    setPoints((current) => current.slice(0, -1));
    setStatus('Last corner removed');
  }

  function clearPolygon() {
    if (readOnly) return;
    setComplete(false);
    setPoints([]);
    setAreaHa(null);
    setValidation(null);
    setStatus('Click the map to place boundary corners');
    onChange(null, null, false, 'Draw a farm boundary polygon');
  }

  function useTemplateBoundary() {
    if (readOnly) return;
    const corners: Point[] = [
      { lng: 30.058, lat: -1.948 },
      { lng: 30.062, lat: -1.948 },
      { lng: 30.062, lat: -1.952 },
      { lng: 30.058, lat: -1.952 }
    ];
    setPoints(corners);
    setComplete(true);
    setStatus('Kigali plot template applied — validating…');
    mapRef.current?.easeTo({ center: [30.06, -1.95], zoom: 14 });
  }

  return (
    <div className="space-y-3">
      {!readOnly ? (
        <div className="rounded-xl border border-border bg-background px-3 py-2 text-sm text-textSecondary">
          <p className="font-medium text-textPrimary">{status}</p>
          <p className="mt-1">
            Click the map to add corners (3+). Then press <strong>Complete polygon</strong>. Use Undo / Clear to
            adjust.
          </p>
        </div>
      ) : null}

      <div
        ref={containerRef}
        className="relative z-0 h-[420px] w-full cursor-crosshair overflow-hidden rounded-2xl border border-border"
        role="application"
        aria-label="Farm boundary map editor"
      />

      {!readOnly ? (
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={completePolygon}
            disabled={points.length < 3 || complete}
            className="rounded-xl bg-primary px-3 py-2 text-sm font-medium text-white disabled:opacity-40"
          >
            Complete polygon
          </button>
          <button
            type="button"
            onClick={undoPoint}
            disabled={points.length === 0}
            className="rounded-xl border border-border px-3 py-2 text-sm disabled:opacity-40"
          >
            Undo last corner
          </button>
          <button
            type="button"
            onClick={clearPolygon}
            disabled={points.length === 0}
            className="rounded-xl border border-border px-3 py-2 text-sm disabled:opacity-40"
          >
            Clear
          </button>
          <button
            type="button"
            onClick={useTemplateBoundary}
            className="rounded-xl border border-dashed border-primary/40 px-3 py-2 text-sm text-primary"
          >
            Use Kigali plot template
          </button>
        </div>
      ) : null}

      <div className="flex flex-wrap gap-4 text-sm">
        <p>
          Corners: <strong>{points.length}</strong>
        </p>
        <p>
          Area: <strong>{areaHa != null ? `${areaHa.toFixed(4)} ha` : '—'}</strong>
        </p>
        {validation ? (
          <p className="text-red-700" role="alert">
            {validation}
          </p>
        ) : complete && areaHa != null ? (
          <p className="text-emerald-700">Geometry valid</p>
        ) : (
          <p className="text-textSecondary">Not completed yet</p>
        )}
      </div>
    </div>
  );
}
