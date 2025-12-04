# Authentication, Refresh Tokens, Employee Management, and Audit Logging

## Authentication Flow
- Users log in via `/api/auth/login` with username and password.
- On success, a JWT token is issued for API access.
- On failure, an audit log is created for the attempt.

## Refresh Token Flow
- Users can request a new access token via `/api/auth/refresh` with their refresh token.
- On success, a new JWT and refresh token are issued, and the refresh token is updated in the database.
- On failure, an audit log is created for the attempt.

## Employee Management
- Only managers can add employees. There is no public registration endpoint for employees.
- Employee creation should be handled via a protected manager endpoint.

## Audit Logging
- All login and refresh actions are logged in the `AuditLog` table.
- Each log entry records username, action (LOGIN_SUCCESS, LOGIN_FAIL, REFRESH_SUCCESS, REFRESH_FAIL), timestamp, and details.

## Example Endpoints
- `POST /api/auth/login` — Login with username and password.
  - **Request:**
    ```json
    {
      "username": "employee1",
      "password": "yourpassword"
    }
    ```
  - **Response:**
    ```json
    {
      "token": "<JWT token>"
    }
    ```
  - **Usage:**
    Use the returned token in the `Authorization: Bearer <token>` header for all protected endpoints.

- `POST /api/auth/refresh` — Refresh access token using refresh token.
  - **Request:**
    ```json
    {
      "refreshToken": "<refresh token>"
    }
    ```
  - **Response:**
    ```json
    {
      "token": "<new JWT token>"
    }
    ```
  - **Usage:**
    Use the new token for continued access. The refresh token is rotated and updated in the database.

- `POST /api/manager/employees` — Manager-only endpoint to add employees.
  - **Request:**
    ```json
    {
      "username": "newemployee",
      "password": "securepassword",
      "roles": ["EMPLOYEE"]
    }
    ```
  - **Authorization:**
    Requires a valid manager JWT token in the `Authorization: Bearer <token>` header.

## Checking Authentication
- For any protected endpoint, include the JWT token in the `Authorization` header:
  ```http
  Authorization: Bearer <token>
  ```
- If the token is valid and the user has the required role, access is granted. Otherwise, a 401 Unauthorized or 403 Forbidden response is returned.

## Security Notes
- JWT tokens are used for stateless authentication.
- Refresh tokens are stored per user and rotated on use.
- Audit logs provide traceability for all authentication and management actions.
