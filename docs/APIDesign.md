# API Design

## API Principles
- Versioned APIs under `/api/v1`
- RESTful resource-oriented design
- Structured request and response DTOs
- Consistent pagination, filtering, and sorting
- OpenAPI documentation from the start

## Core API Areas
- Authentication / User management
- Farmer / Farm / Crop / Season (Phase 4)
- Policy / Premium (Phase 5)
- **Workflow definitions & instances (Phase 6A)** — see `API.md`
- Claims and payouts (later — must reuse workflow)
- Notifications and reporting (later stages)

## Endpoint Conventions
- `GET /api/v1/{resource}` for collection listing
- `GET /api/v1/{resource}/{id}` for single resource retrieval
- `POST /api/v1/{resource}` for creation
- `PUT /api/v1/{resource}/{id}` for full update
- `PATCH /api/v1/{resource}/{id}` for partial update
- `DELETE /api/v1/{resource}/{id}` for deletion

## Security Expectations
- JWT bearer authentication for protected endpoints
- Refresh token rotation for session management
- RBAC and permission-based checks
- Standardized error responses
