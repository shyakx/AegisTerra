# Coding Standards

## General Principles
- Prefer readability and maintainability over cleverness
- Keep modules focused and cohesive
- Reuse shared abstractions
- Avoid placeholder logic and mock-only behavior
- Write tests for business-critical behavior

## Backend Expectations
- Use Java 21 and Spring Boot 3.x
- Follow Clean Architecture boundaries
- Use DTOs for transport and domain objects for business logic
- Handle errors consistently with a global exception framework
- Write structured logs with correlation IDs

## Frontend Expectations
- Use TypeScript strictly
- Keep components small and composable
- Prefer feature-based modules over monolithic screens
- Use shared UI primitives for all repeated patterns
