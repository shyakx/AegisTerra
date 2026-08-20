# Architecture Overview

## Summary
This document defines the target architecture for AegisTerra and acts as the reference for implementation decisions.

## Backend Architecture
The backend follows Clean Architecture with these layers:
- domain: business entities and rules
- application: use cases
- infrastructure: persistence and integrations
- presentation: REST controllers and DTOs
- configuration: app configuration
- shared: reusable utilities and abstractions
- security: auth, authorization, token handling
- integration: external service connectors
- audit: audit event generation
- common: common models and exceptions

## Frontend Architecture
The frontend is structured around feature-based modules:
- app: bootstrap and app shell
- pages: route-level pages
- layouts: shared layouts
- features: domain-specific UI modules
- components: reusable UI primitives
- hooks: custom hooks
- services: API client helpers
- api: typed API contracts
- store: Redux state management
- routes: route definitions
- types: shared TypeScript models
- assets: static media
- theme: design tokens and theme helpers
- utils: shared utilities

## Design Principles
- Secure by default
- Modular by domain
- Testable and auditable
- Scalable and maintainable
- GIS-aware from the beginning
