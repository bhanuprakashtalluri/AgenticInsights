# API Endpoint Role Access Matrix (Updated)

This document lists all major API endpoints in the application and specifies which user roles (ADMIN, MANAGER, TEAMLEAD, EMPLOYEE) have access to each endpoint, based on the current codebase and security configuration as of December 9, 2025.

## Role Legend
- ✓ = Endpoint is available to this role
- ✗ = Endpoint is not available to this role
- ? = Endpoint should be restricted, but is not explicitly enforced in code

---

## Auth Endpoints (`/api/auth`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/login`                | POST   |   ✓   |    ✓    |    ✓     |    ✓     | Session login |
| `/update-password`      | PUT    |   ✓   |    ✓    |    ✓     |    ✓     | Password change |
| `/sync-users`           | POST   |   ✓   |    ✗    |    ✗     |    ✗     | Sync user table with employee table |
| `/me`                   | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Get current user info |

---

## Employee Endpoints (`/employees`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/employees`            | POST   |   ✓   |    ✓    |    ✗     |    ✗     | Create employee |
| `/employees`            | GET    |   ✓   |    ✓    |    ✓     |    ✓     | List employees |
| `/employees/single`     | GET    |   ✓   |    ✓    |    ✓     |    ✓*    | Get employee by ID/UUID (*EMPLOYEE can only access their own info) |
| `/employees/single`     | PUT    |   ✓   |    ✓    |    ✗     |    ✗     | Update employee |
| `/employees/single`     | DELETE |   ✓   |    ✓    |    ✗     |    ✗     | Delete employee (now allowed for MANAGER) |

---

## Recognition Endpoints (`/recognitions`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/recognitions`         | POST   |   ✓   |    ✓    |    ✓     |    ✓     | Create recognition |
| `/recognitions`         | GET    |   ✓   |    ✓    |    ✓     |    ✓*    | List recognitions (*EMPLOYEE can only access recognitions sent/received by them) |
| `/recognitions/single`  | GET    |   ✓   |    ✓    |    ✓     |    ✓*    | Get recognition by ID/UUID (*EMPLOYEE only if sender/recipient) |
| `/recognitions/single`  | PUT    |   ✓   |    ✓    |    ✓     |    ✓*    | Update recognition (*EMPLOYEE only if sender/recipient) |
| `/recognitions/single`  | DELETE |   ✓   |    ✓    |    ✗     |    ✗     | Delete recognition |

---

## Recognition Type Endpoints (`/recognition-types`)
| Endpoint                        | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|----------------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/recognition-types`             | POST   |   ✓   |    ✓    |    ✗     |    ✗     | Create recognition type |
| `/recognition-types`             | GET    |   ✓   |    ✓    |    ✓     |    ✓     | List recognition types |
| `/recognition-types/single`      | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Get recognition type by ID/UUID |
| `/recognition-types/single`      | PUT    |   ✓   |    ✓    |    ✗     |    ✗     | Update recognition type |
| `/recognition-types/single`      | DELETE |   ✓   |    ✓    |    ✗     |    ✗     | Delete recognition type |
| `/recognition-types/search`      | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Search recognition types |

---

## Admin Endpoints (`/admin`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/admin/seed/run`       | POST   |   ✓   |    ✗    |    ✗     |    ✗     | Run seed (dev mode) |
| `/admin/dev-mode`       | GET    |   ✓   |    ✗    |    ✗     |    ✗     | Get dev mode status |
| `/admin/dev-mode`       | PATCH  |   ✓   |    ✗    |    ✗     |    ✗     | Set dev mode status |
| `/admin/export`         | GET    |   ✓   |    ✗    |    ✗     |    ✗     | Export data |

---

## Leaderboard Endpoints (`/leaderboard`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/leaderboard/top-senders`    | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Top senders leaderboard |
| `/leaderboard/top-recipients` | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Top recipients leaderboard |

---

## Insights Endpoints (`/insights`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/insights`             | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Global insights |
| `/insights/graph.png`   | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Global graph |
| `/insights/employee/{employeeId}` | GET |   ✓   |    ✓    |    ✓     |    ✓     | Employee insights |
| `/insights/unit/{unitId}`         | GET |   ✓   |    ✓    |    ✓     |    ✓     | Unit insights |
| `/insights/role`        | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Insights by role |
| `/insights/role/graph.png` | GET  |   ✓   |    ✓    |    ✓     |    ✓     | Graph by role |
| `/insights/manager/{managerId}` | GET |   ✓   |    ✓    |    ✓     |    ✓     | Insights by manager |
| `/insights/manager/{managerId}/graph.png` | GET |   ✓   |    ✓    |    ✓     |    ✓     | Graph by manager |

---

## Metrics Endpoints (`/metrics`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/metrics/summary`      | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Metrics summary |

---

## Frontend Routing (`/`)
| Endpoint                | Method | ADMIN | MANAGER | TEAMLEAD | EMPLOYEE | Notes |
|-------------------------|--------|:-----:|:-------:|:--------:|:--------:|-------|
| `/` (frontend)          | GET    |   ✓   |    ✓    |    ✓     |    ✓     | Serves React app |

---

## Notes
- Endpoints like `/admin/*`, `/api/auth/sync-users`, and `/recognition-types` (POST/PUT/DELETE) should be restricted to ADMIN only, but may not be explicitly enforced in code. Consider adding `@PreAuthorize` or similar annotations for these.
- Employees can only access their own info and recognitions (sent/received by them).
- Managers can now delete employees.
- This table is based on current code and may need updates if you change security configuration or add new endpoints.

---

If you need a more detailed mapping for any specific controller or want to enforce stricter access, let me know!

