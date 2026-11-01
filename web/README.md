# Inventory Web (React + Vite frontend)

React 19 + Vite frontend for the Inventory Tracking System. It signs users in with Firebase Authentication
(Email/Password) and calls the Spring Boot API in `../api` with `Authorization: Bearer <Firebase ID token>`.

## Configuration

1. In the Firebase console of the **non-production** project:
   - Authentication → Sign-in method → enable **Email/Password**.
   - Project settings → General → Your apps → add a **Web app** (if none exists) and copy its config.
   - Authentication → Settings → Authorized domains must include `localhost`.
2. Copy `.env.example` to `.env.local` (git-ignored) and fill in:

| Variable | Firebase config field |
|---|---|
| `VITE_FIREBASE_API_KEY` | `apiKey` |
| `VITE_FIREBASE_AUTH_DOMAIN` | `authDomain` |
| `VITE_FIREBASE_PROJECT_ID` | `projectId` |
| `VITE_FIREBASE_APP_ID` | `appId` |

Without these values the app shows a "Firebase is not configured" page instead of the login screen.

The backend must use the **same** Firebase project (`GOOGLE_APPLICATION_CREDENTIALS`, see `../api/README.md`),
otherwise every request is rejected with 401. Admin rights come only from the backend allowlist
`APP_SECURITY_ADMIN_EMAILS` plus a verified email.

## Running

```powershell
# 1. Start the backend first (see ../api/README.md) — it listens on http://localhost:8080
# 2. Then, in another terminal:
Set-Location .\web
npm install
npm run dev        # http://localhost:5173 — /api is proxied to http://localhost:8080
```

## Tests and build

```powershell
npm test           # Vitest + React Testing Library (Firebase and the API are mocked; no network)
npm run build      # writes web/dist
```

`web/dist/` is not yet listed in the root `.gitignore`; do not commit it.

## Notes

- Stock quantities and totals arrive from the API as decimal strings and are formatted with `BigInt`
  (`src/utils/format.js`), so values above 2^53 are shown exactly.
- Hiding admin buttons and routes is for usability only; the backend enforces every permission.
