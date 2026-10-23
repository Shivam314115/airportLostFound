# Return Gate — Airport Lost Property Frontend

A standalone, responsive React frontend for an airport lost-property experience. This package intentionally contains **no seeded reports, sample passengers, mock match scores, fabricated recovery claims, or demo authentication**. It does not call the Spring Boot backend, PostgreSQL, or any live service.

## Design direction

Return Gate uses a refined aviation visual language: deep terminal navy, cool porcelain surfaces, Atlantic teal, and a restrained amber waypoint. The airplane-and-route mark gives the wordmark a product-specific identity without relying on a generic logo tile or decorative gradients.

Typography is deliberately split by role:

- **SF / Apple system typography** for body text and controls where the native font is available.
- **Manrope** as the locally bundled cross-platform UI fallback, loaded through `@fontsource/manrope`.
- **Montserrat** for editorial headings and the wordmark, loaded locally through `@fontsource/montserrat`.
- No unlicensed Apple font files are included; Linux and Windows use Manrope for the UI and Montserrat for display.

## Component choices

This project uses a small, consistent set of tools rather than combining every alternative UI framework:

- React + TypeScript + Vite
- Radix UI Dialog for modal focus and keyboard behavior, custom styled to fit Return Gate
- Lucide React for one consistent icon system
- A semantic CSS token system for colours, spacing, typography, states, and responsive layout

MUI, Ant Design, Mantine, Chakra UI, HeroUI, and the other listed component frameworks are alternatives, not ingredients to install together. Mixing multiple full UI systems would create style conflicts and unnecessary bundle weight.

## Requirements

- Node.js 20 or later (Node 22 recommended)
- npm

## Run locally

```bash
npm install
npm run dev
```

Vite prints the local development URL, normally `http://localhost:5173`. If that port is occupied, use:

```bash
npm run dev -- --port 5174
```

Production build:

```bash
npm run build
npm run preview
```

## What works in this frontend-only version

- Responsive navigation between Discover, My reports, and Possible matches
- Search input and category selection states
- Report form with validation provided by the browser; it is not submitted or stored
- Sign-in interface that explains authentication is not connected
- Empty states for report search, personal reports, and match results
- Service information dialog
- Keyboard-accessible Radix dialogs, visible focus states, and reduced-motion styling

## No dummy data and no persistence

There are no item records or passenger fixtures in this package. Search reports zero currently available records because no live data service is configured. The passenger area and possible match area show truthful empty states rather than pretend accounts or fabricated match results. Form values remain only in transient component memory while the page is open; the app does not save them to local storage, send them to a server, or report false success.

Do not enter real personal information in this standalone preview.

## Future backend integration (not implemented)

A future API adapter can be introduced without rewriting the visual components. The current Spring Boot backend uses session authentication and CSRF protection. Before connecting, verify its current DTOs, API error shape, CORS/origin requirements, CSRF lifecycle, and session-cookie settings. Likely endpoints include:

- `GET /api/auth/csrf`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/lost-reports`
- `POST /api/lost-reports`
- `GET /api/matches`

For lost-report creation, do not send a `passengerId` in the request body; the backend should derive ownership from the authenticated identity.

## Project structure

```text
src/
  App.tsx                   Screens, navigation, empty states and preview forms
  main.tsx                  React entry point and local Montserrat font imports
  styles.css                Visual tokens, typography, layout and responsive rules
  components/ui/Dialog.tsx  Radix-based accessible dialog primitive
```

## Verification

Run `npm install` and `npm run build` after extracting the archive. Browser-based visual QA should include a wide desktop viewport and a narrow mobile viewport. No claim of a backend-connected flow is made by this frontend-only package.
