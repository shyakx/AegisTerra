# ADR-004: Use React with TypeScript for the Web Client

## Status

Accepted

## Date

2026-08-05

## Context

AegisTerra needs a rich operational web UI: dense data tables, forms, dashboards, and an interactive GIS map for farm boundaries and risk overlays. Users include administrators and field-facing roles who need a responsive SPA. The team already scaffolded React 18 + Vite + TypeScript + Tailwind.

Requirements favoring a component SPA:

- Complex client-side workflows (claims, inspections, GIS)
- Ability to adopt MapLibre, charts, and data grids
- Strong typing for DTO contracts shared with backend conceptually

## Decision

Use **React 18+** with **TypeScript**, **Vite**, **React Router**, **TanStack Query** for server state, and **Tailwind CSS** for styling as the primary web client stack.

Redux Toolkit may be used for cross-cutting client session/UI state only when React Query and local component state are insufficient — not as a duplicate server cache.

## Consequences

### Positive

- Large ecosystem for GIS, forms, tables, and enterprise UI patterns
- TypeScript improves contract safety at the edge
- Vite provides fast local feedback

### Negative / costs

- SPA security must be designed carefully (tokens, XSS, route guards)
- Without discipline, pages become untested local-state prototypes (current debt)

## Alternatives considered

| Alternative | Why not chosen |
|-------------|----------------|
| Server-rendered only (Thymeleaf/JSP) | Poor fit for interactive GIS and dense ops UI |
| Angular | Viable enterprise option; React already scaffolded and adequate |
| Vue | Viable; ecosystem preference and existing code favor React |
| Desktop thick client | Distribution/ops burden for national rollout |

## Implementation note

Current UI is a shell with unused dependencies. Follow `DevelopmentRoadmap.md` for wiring real APIs and introducing MapLibre only when Farm GIS is in scope.
