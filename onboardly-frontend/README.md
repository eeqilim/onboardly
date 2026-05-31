# onboardly-frontend

React + Vite frontend for the Onboardly employee and HR portal.

The app includes employee registration, login, onboarding, personal information, visa status management, housing, facility reports, and HR management pages. It uses Ant Design for the main UI components and axios service modules for backend API calls.

## Tech Stack

- React 18
- Vite
- React Router v7
- Ant Design v6
- Axios

## Getting Started

Install dependencies:

```bash
npm install
```

Run the local dev server:

```bash
npm run dev
```

## Project Structure

```
src/
  api/axios.js           Shared axios instance with JWT + 401 interceptors
  auth/
    AuthContext.jsx      Global auth state (token, user, role flags)
    AuthGuards.jsx       Route guards by role and onboarding status
    authStorage.js       localStorage read/write helpers
  services/              One module per backend domain
  components/
    Navbar.jsx           Role-aware top navigation
    auth/                AuthCard, AuthPage shells for login/register
    common/              PageCard, PageHeader, StatusAlert, DocumentPreviewModal
  pages/
    Login.jsx
    employee/            Employee-facing pages
    hr/                  HR-facing pages
```

## User Flows

### Employee flow

1. **Invitation.** HR generates a registration token from `/hr/hiring-management`. The backend emails the employee a link of the form `http://localhost:5173/register?token=<token>`.
2. **Registration** (`/register`). The employee opens the link, enters their email, username, and password. On success they are redirected to `/login`.
3. **Login** (`/login`). On successful login the frontend immediately calls `POST /employee/onboarding/start` to initialize the application workflow, then routes the user based on their `onboardingStatus`:
   - `NOT_STARTED` or `REJECTED` → `/employee/onboarding`
   - `PENDING` → `/employee/waiting-review`
   - `APPROVED` → `/employee/home`
4. **Onboarding form** (`/employee/onboarding`). Multi-section form: name and identity, address, contact info, citizenship and work authorization, driver's license, reference, emergency contacts.
5. **Onboarding documents** (`/employee/onboarding/documents`). For each required document type (W-4, I-9, Company Policy, …), the employee downloads the template and uploads a completed copy. Documents are stored in S3 via presigned URLs.
6. **Waiting review** (`/employee/waiting-review`). Read-only holding page until HR approves or rejects the application.
7. **Approved employee pages.** Once HR approves:
   - `/employee/home` — welcome dashboard
   - `/employee/personal-info` — edit profile sections (name, address, contact, employment, emergency contacts) and avatar; each section saves independently
   - `/employee/visa-status` — STEM-OPT 4-step workflow (I-983 → I-20 → OPT Receipt → OPT EAD), only shown for `F1_OPT_STEM` work authorization
   - `/employee/housing` — view assigned house and co-residents
   - `/employee/facility-reports` — create and comment on facility issues for the assigned house

### HR flow

1. **Login** (`/login`). HR users are routed to `/hr/home`.
2. **Hiring Management** (`/hr/hiring-management`). Send registration invitations to new employees, view pending registrations.
3. **Application review.** When an employee submits onboarding, HR can review the form (`/hr/form-application/:id`) and the uploaded documents (`/hr/received-documents/:id`), then approve or reject with optional feedback.
4. **Employee Profiles** (`/hr/employee-profiles`, `/hr/employee-profile/:employeeId`). Paginated, searchable list of all employees with full per-employee profile and document view.
5. **Visa Status Management** (`/hr/visa-status-management`). Review STEM-OPT step uploads from non-citizen employees and approve or request changes.
6. **Housing Management** (`/hr/housing-management`, `/hr/housing-details/:houseId`, `/hr/housing-form`, `/hr/housing/facility-form/:houseId`). Create houses and facilities, assign employees to houses, view residents, review facility reports submitted by employees.

## Backend API

The axios client is configured to call the API Gateway at:

```txt
http://localhost:8080
```

Service modules are organized by backend area:

- `src/services/authService.js` — login, registration token generation, register
- `src/services/employeeService.js` — profile, contacts, documents, avatar, HR employee list/search/housing-assign
- `src/services/onboardingService.js` — onboarding templates, document upload/preview/download, HR review
- `src/services/visaService.js` — STEM-OPT progress, step uploads, HR review
- `src/services/housingService.js` — houses, landlords, facilities, facility reports + comments
- `src/services/applicationService.js` — generic application workflow advance/review/list

All requests share one axios instance. The request interceptor reads the JWT from `localStorage` and attaches `Authorization: Bearer <jwt>`; the response interceptor clears stored auth and redirects to `/login` on a `401`.

## Registration Links

The frontend route for employee registration is:

```txt
http://localhost:5173/register
```

The backend generates the registration token and emails the link to the employee. The frontend requires `token` and supports an optional `email` query parameter:

```txt
http://localhost:5173/register?token=<registration-token>
http://localhost:5173/register?token=<registration-token>&email=<employee-email>
```

If the email is included in the link, the email field is pre-filled and locked. If not, the employee enters their email during registration.

> Note: the backend currently emails the link with only the `token` query parameter. The `&email=` form is supported by the frontend but not yet emitted by the backend.

## Routes

Public:

- `/login`
- `/register`

Employee (require employee login):

- `/employee/onboarding`
- `/employee/onboarding/documents`
- `/employee/waiting-review`

Approved employee (require `onboardingStatus === "APPROVED"`):

- `/employee/home`
- `/employee/personal-info`
- `/employee/visa-status`
- `/employee/housing`
- `/employee/facility-reports`

HR (require HR role):

- `/hr/home`
- `/hr/employee-profiles`
- `/hr/employee-profile/:employeeId`
- `/hr/hiring-management`
- `/hr/form-application/:id`
- `/hr/received-documents/:id`
- `/hr/housing-management`
- `/hr/housing-details/:houseId`
- `/hr/housing-form`
- `/hr/housing/facility-form/:houseId`
- `/hr/visa-status-management`

## Auth Lifecycle

Auth is JWT-based. The token and user object live in `localStorage`; `AuthContext` mirrors them into React state so the UI reacts to login/logout. Route guards in `src/auth/AuthGuards.jsx` gate access by role and onboarding status.

```
┌─────────────┐  user enters creds
│  Login.jsx  │ ──────────────────────┐
└─────────────┘                       ▼
                            ┌──────────────────┐
                            │ POST /auth/login │
                            │   → JWT + user   │
                            └────────┬─────────┘
                                     │
                                     ▼
                     localStorage.setItem("jwt", token)
                     localStorage.setItem("user", ...)
                                     │
                                     ▼
                           AuthContext state updated
                                     │
            ┌────────────────────────┼────────────────────────┐
            ▼                        ▼                        ▼
     any page calls          AuthGuards check          Navbar reads
     a service module        isHR / isEmployee /       user.username
            │                isApproved
            ▼
     axios interceptor
     reads localStorage("jwt")
            │
            ▼
     adds "Authorization: Bearer <jwt>"
            │
            ▼
     sends to http://localhost:8080
            │
            ▼
    ┌───────────────┬─────────────────┐
    │ 200/4xx OK    │ 401 Unauthorized│
    └───────┬───────┴─────────┬───────┘
            │                 │
            ▼                 ▼
     returned to caller   clearStoredAuth()
                          window.location.href = "/login"
```
