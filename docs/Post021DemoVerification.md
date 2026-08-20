# Post-021 Demo Verification

## Date
2026-08-08

## Scope
Validate the real application after the 021 RC2 operational demo-data seed and confirm the live authentication path works through the production-style security stack.

## Final outcome
Authentication is working correctly in the live local stack and no security bypass was introduced.

## Verification evidence

### 1) Backend health
The backend was started in the local profile and the health endpoint responded successfully:
- HTTP status: 200
- Response: {"status":"UP","groups":["liveness","readiness"]}

### 2) Real login request
A direct POST request to the live endpoint succeeded with the documented bootstrap admin credentials:
- Endpoint: POST /api/v1/auth/login
- Username: admin
- Password: Admin@1234!Aa
- Result: HTTP 200
- Returned cookies: at and rt
- Response body included the authenticated admin user with SYSTEM_ADMIN role

### 3) Browser verification
The application was launched in the browser and the real login flow completed successfully:
- URL: http://localhost:3000/login
- Username entered: admin
- Password entered: Admin@1234!Aa
- Result: redirect to the app home page at /
- UI rendered: “Operations Command Center” with the dashboard loaded and signed-in admin session

## Root cause of the earlier failure
The earlier failure was not caused by the authentication architecture itself. The app was not running under the correct local backend profile / port state, so the login request was being attempted against an inconsistent runtime environment. Once the backend was restarted in the correct local profile, the production-style auth flow succeeded without any weakening or bypass.

## Conclusion
- The app-level authentication flow is valid and working.
- The login path remains cookie-based, JWT-backed, and secured without bypasses.
- The 021 demo-data verification path is valid and the application is usable for the intended demo scenario.
