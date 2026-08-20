# Frontend setup

## Prerequisites

- Node.js 20+
- npm 10+

## Install and run

```powershell
cd frontend
copy .env.example .env
npm install
npm run dev
```

Or from the `AegisTerra` root: `.\start-frontend.ps1`

Dev server: http://127.0.0.1:3000  
API calls to `/api/*` are proxied to `VITE_DEV_API_PROXY` (default `http://localhost:8080`).

## Scripts

| Script | Purpose |
|--------|---------|
| `npm run dev` | Vite development server |
| `npm run typecheck` | TypeScript project build check |
| `npm run lint` | ESLint |
| `npm run format:check` | Prettier check |
| `npm run build` | Production build |

## Demo (RC 1.0)

- Script: [`docs/DemoDressRehearsal.md`](../docs/DemoDressRehearsal.md)
- Freeze checklist: [`docs/DemoFreezeChecklist.md`](../docs/DemoFreezeChecklist.md)
- API smoke: `..\scripts\demo-smoke.ps1` (backend must be running)

## State ownership

- **TanStack Query** — server/API state
- **Component state** — ephemeral UI
- **Redux** — not in use (reintroduce only with a documented cross-cutting need)
