# Authentication, Session, Employee Management, and Audit Logging (Current State)

## Authentication Flow
- Users log in via `/api/auth/login` with username (email) and password.
- On success, a session cookie is issued for API and UI access.
- On failure, an audit log is created for the attempt.

## Password Update
- Users can update their password via `/api/auth/update-password`.
- Passwords are stored hashed (BCrypt) in the database.
- Audit logs are created for password changes.

## Employee Management
- Only managers and admins can add employees. There is no public registration endpoint for employees.
- Employee creation is handled via protected endpoints.

## Audit Logging
- All login, password update, and agent execution actions are logged in the `AuditLog` table.
- Each log entry records username, action (LOGIN_SUCCESS, LOGIN_FAIL, PASSWORD_UPDATE, AGENT_EXECUTE_SUCCESS, AGENT_EXECUTE_DENIED), timestamp, and details.

## Example Endpoints
- `POST /api/auth/login` — Login with username and password.
  - **Request:**
    ```json
    {
      "username": "employee1@company.com",
      "password": "yourpassword"
    }
    ```
  - **Response:**
    ```json
    {
      "email": "employee1@company.com",
      "role": "EMPLOYEE"
    }
    ```
  - **Usage:**
    Use the session cookie for all protected endpoints.

- `PUT /api/auth/update-password` — Update password for user.
  - **Request:**
    ```json
    {
      "email": "employee1@company.com",
      "newPassword": "newsecurepassword"
    }
    ```
  - **Response:**
    ```json
    {
      "status": "success"
    }
    ```
  - **Usage:**
    Password is updated and stored as a BCrypt hash.

## Checking Authentication
- For any protected endpoint, include the session cookie.
- If the session is valid and the user has the required role, access is granted. Otherwise, a 401 Unauthorized or 403 Forbidden response is returned.

## Security Notes
- Session cookies are used for authentication.
- Passwords are hashed using BCrypt.
- Audit logs provide traceability for all authentication and management actions.

---

*Legacy JWT/refresh token flow is no longer used. All authentication is now session-based.*
