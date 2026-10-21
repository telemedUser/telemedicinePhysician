# Agent Handoff — Physician Module (telemedicine_physician_integration)

> Purpose: give a fresh agent (new account/session) the full working context so work can
> continue from the current step without re-discovery. Read this top-to-bottom first.
> Last updated: after building the D-03 patient review/join page end-to-end over Kafka.
>
> **Patient module** is a separate repo with its own handoff:
> `c:\Users\anasin\Documents\telemedicine_docs\telemedicinePatient_integration\docs\AGENT-HANDOFF.md`
> (patient-booking app :3001 + patient-booking-service :8120; booking P-01→P-13 + join flow P-14→P-17 done).

---

## 0. TL;DR of where we are

Telemedicine platform, **physician module**. Active work is the standalone **`physician-app`**
(:3015) + **`physician-service`** (:8114) in THIS repo (`telemedicine_physician_integration`).

Just completed this session:
- Got the physician-app running locally (it was showing a white screen — fixed a broken
  half-finished edit in `App.tsx`).
- Implemented **per-physician login**: each doctor signs in and sees ONLY their own worklist.
- Built the **D-01 home/dashboard page** (`/home`), synced with the worklist; removed
  **"Physicians"** from the sidebar completely (scope is Home + Virtual consults only — HIS is a
  separate system we are NOT building).

Next up (user will drive): more physician screens (D-NN), user shares prototype screenshots one at a time.

---

## 1. Repos / folders (all under `c:\Users\anasin\Documents\telemedicine_docs\`)

| Folder | What it is | Status |
|--------|-----------|--------|
| `telemedicine_physician_integration` | **ACTIVE repo** for physician work. Full platform copy; we run the standalone physician-app + physician-service. | In active use |
| `telemedicine_physician` | Earlier physician-only copy. Same ports (:8114/:3015) + DB `physician`. Per-physician login was built then REVERTED there. | Legacy — do not run at same time |
| `telemedicinePatient_integration` | **ACTIVE repo** for patient work (patient-booking app :3001 + service :8120). Booking P-01→P-13 + join flow P-14→P-17 built. See its own docs/AGENT-HANDOFF.md. | In active use |
| `telemedicinePatient` | Earlier patient-only copy. Same :8120 + DB `patient_booking`. | Legacy |

Prototype references (self-unpacking HTML, open in a browser): `Bp Telemedicine Physician Prototype.html`,
`Bp Telemedicine Patient Prototype.html`, `PRD-TM-001-v0.2-Blupa-Telemedicine-Platform.pdf`.
Existing repo docs: `docs/Features.md`, `docs/Gaps-and-Flags.md`, `docs/Architecture.md`, `docs/Skills.md`.

---

## 2. Product decisions (apply to ALL work — keep consistent)

- **Language:** English only.
- **Data/locale:** Indian names (Dr. Arjun Khanna, Dr. Meera Nair, Dr. Vikram Reddy, Dr. Lakshmi Iyer…),
  **INR** currency, ABHA/NMC refs.
- **Branding:** neutral **"Connected Care"**. NEVER use the prototype's "blua"/"Bupa", "FONASA",
  "Webpay", "CLP", "Clínica Bupa Santiago", "Santiago (GMT-3)", SAMU/131. Use neutral equivalents,
  INR, emergency 112.
- **Match the prototype's LAYOUT, components, colours** (navy `#10294A`, blue `#0079C8`, Figtree,
  warm paper `#e7e4dd`), NOT its copy/locale. User sends prototype screenshots one at a time
  (physician screens are "D-NN", e.g. D-01). Collect all before implementing a batch.

---

## 3. How to run everything (Windows / PowerShell)

Prereqs: `JAVA_HOME=C:\Program Files\Java\jdk-21.0.12`; Docker infra UP (`telemedicine-postgres-1`,
`-redis-1`, `-consul-1`, `-kafka-1`); `corepack` for pnpm@9.15.0 (pnpm NOT on PATH — use `corepack pnpm`);
Kafka external port **29092**.

### physician-service (:8114)
```
cwd: telemedicine_physician_integration\telemedicine-backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.12"; $env:KAFKA_BOOTSTRAP="localhost:29092"; $env:CONSUL_HOST="localhost"; $env:CONSUL_PORT="8500"; $env:DB_HOST="localhost"; $env:DB_PORT="5432"; $env:DB_NAME="physician"; $env:DB_USER="physician"; $env:DB_PASSWORD="change-me-locally"; $env:REDIS_HOST="localhost"; $env:REDIS_PORT="6379"; $env:REDIS_PASSWORD="change-me-locally"; .\gradlew.bat :physician-service:bootRun --no-daemon --console=plain
```
DB `physician` / user `physician` / pwd `change-me-locally`. `telemedicine.security.mode=DEV`,
`validate-on-migrate:false`. ~60s to boot.

### physician-app (:3015)
```
# one-time: cwd telemedicine_physician_integration\telemedicine-frontend ; corepack pnpm install
cwd: telemedicine_physician_integration\telemedicine-frontend
corepack pnpm exec nx serve physician-app
```
Proxies: `/api`,`/ws` → :8114 ; `/booking-api` → :8120 (reads patient booking details for "View details").
`.env.development`: `VITE_AUTH_MODE=dev`, `VITE_API_BASE_URL=/api`.

> PORT/DB COLLISION: `telemedicine_physician` and `telemedicine_physician_integration` both use
> :8114 / :3015 / DB `physician`. Run only ONE. Active = the **integration** copy. (Same story for
> the patient repos on :8120 / :3001 / DB `patient_booking`.)

---

## 4. Per-physician login — WHAT WAS BUILT THIS SESSION (the current step)

Goal: each physician logs in and sees only the patients booked with them.

### Frontend (physician-app) — all in place
- `src/pages/PhysicianLoginPage.tsx` — sign-in form, POSTs `/api/auth/physician-sign-in`, stores session.
- `src/auth/physicianSession.ts` — localStorage-backed session `{physicianId, displayName, email}`; `currentPhysicianId()` used by the API interceptor.
- `src/components/AppProviders.tsx` — builds the ApiClient with a request interceptor that stamps **`X-Physician-Id: <physicianId>`** on every request.
- `src/App.tsx` — **wired this session**: new `Gated` component shows `PhysicianLoginPage` when no session, else `AuthedShell`. `TopBar` now shows the signed-in doctor (from session) + a **sign-out** button. (Earlier the file had a broken half-edit: it imported login modules but TopBar still referenced `CURRENT_PHYSICIAN` → `ReferenceError` / white screen. Now fixed: CURRENT_PHYSICIAN still used only for the static HIS clinic subline; the user chip + scoping come from the session.)

### Backend (physician-service) — already present, verified working
- `api/PhysicianAuthController.java` — `POST /api/auth/physician-sign-in` {email,password} → {success, data:{physicianId, displayName, email}}. Demo accounts (password `doctor123`): arjun.khanna@connectedcare.in→doc-khanna, meera.nair@…→doc-nair, vikram.reddy@…→doc-reddy, lakshmi.iyer@…→doc-iyer.
- `config/DevPhysicianAuthFilter.java` — reads `X-Physician-Id` header and sets the Spring Security principal to that id (so `currentPhysicianId()` resolves to the signed-in doctor). Falls back to `dev-physician` when absent.
- `config/DevSecurityConfiguration.java` — installs the filter (`addFilterBefore(... UsernamePasswordAuthenticationFilter)`); permit-all dev chain.
- The worklist is scoped by `provider_id` (+ market "IN"). Flyway `V105__worklist_by_physician.sql` re-scopes the demo worklist rows to the real doctor ids (doc-khanna etc.).

### Verified end-to-end (this session)
- Sign-in doc-khanna returns the right identity.
- Worklist scoping: **doc-khanna → 7 items, doc-nair → 1, doc-reddy → 1, doc-iyer → 1**, `dev-physician` → 12 (all). Each physician sees only their own patients.

### Patient arrival signal (how a row flips to "In waiting room"/"Ready")
The patient join flow P-16 (`POST /api/consultations/{ref}/ready` on patient-booking-service :8120,
`ConsultationFlowService.markReady`) publishes `PatientEnteredWaitingRoom` + `DeviceTestCompleted`.
The physician-service `WorklistProjectionListener` consumes both: `onPatientEntered` sets `enteredAt`
and readiness=IN_PROGRESS; `onDeviceTest` sets readiness=READY (if camera+mic ok & bandwidth>0) /
NOT_READY and technicalStatus. **Backend fix made this session (TM-INT-013):** `onPatientEntered`
and the insert-race `copyInto` no longer regress readiness from READY back to IN_PROGRESS — the two
events arrive on independent topics so ordering isn't guaranteed; readiness now only moves forward.
Verified live: fresh arrival with passing device check lands on READY (e.g. doc-khanna TM-8OGHC7).
Smoke-test an arrival: `POST http://localhost:8120/api/consultations/<appointmentId>/ready` with body
`{"patientRef":"...","cameraOk":true,"microphoneOk":true,"bandwidthKbps":15000}`, then re-GET the
worklist (propagates in <~3s).

### The appointment→physician routing (why a doctor sees "their" patients)
Patient bookings (patient-booking-service :8120) publish `AppointmentCreated` with
`physicianRef = doctorId` (e.g. doc-khanna). The physician-service `WorklistProjectionListener`
consumes it and sets `worklist_item.provider_id = physicianRef`. So a booking for doc-khanna shows
on doc-khanna's worklist only. patientRef carries the patient's display name.

---

## 5. Physician-app screens already built (reworked to prototype earlier)

Routes in `src/App.tsx`; pages in `src/pages/`; shared design primitives in `src/ui/primitives.tsx`;
theme in `src/styles/theme.css`.
- **Home / dashboard** (`HomePage.tsx`, route `/home`, prototype **D-01**) — landing page after login. Time-of-day greeting with the signed-in doctor's short name; a "Today · Virtual clinic · N patients · M in waiting room" card whose table is **synced with the worklist** (same `getWorklist()` → `GET /api/v1/physicians/me/worklist`, scoped by `X-Physician-Id`, auto-refresh 15s) showing Time/Patient/Type/Status; an "Open virtual consultations" button → `/worklist`; and a static right-rail **Inbox** summary (results/notes/renewals/messages — dashboard context only, not backed by a service). Uses `.home-grid` (2-col → 1-col under 900px, rule in `theme.css`). **No HIS sessions** (morning clinic / ward round / inpatients) — HIS is a separate system, intentionally omitted.
- **D-01 Today timeline + real consultation time** (updated this session to match the D-01 screenshot). The "Today" card is a **timeline**: `Morning clinic` (09:00–13:00) and `Ward round` (14:00–15:30) are **hardcoded** entries (`HIS_SESSIONS` in `HomePage.tsx`) — placeholders for internal HIS APIs we do not build — each shown Completed; the **Virtual clinic** entry is live. Header reads "N sessions · M patients". Right rail adds a hardcoded **Inpatients** card alongside Inbox. The virtual-clinic table's **Time column shows the patient's booked consultation time** (`scheduledAt`), NOT the waiting-room arrival. To carry it: `AppointmentCreated.scheduledAt` → worklist projection new column (`V107__worklist_scheduled_at.sql`, `WorklistItemEntity.scheduledAt`, set in `onAppointmentCreated` + `copyInto`) → `WorklistEntryResponse.scheduledAt` → frontend `WorklistEntry.scheduledAt`. V107 also backfills demo rows TM-DEMO01/05/06 with 16:20/16:40/17:00 IST slots. Live bookings carry their own scheduledAt (verified: a fresh booking at a 14:00 slot showed scheduledAt on the worklist). Rows projected before V107 have null scheduledAt until re-booked — expected.
- **Status is data-driven, shared by Home + Worklist** (`src/api/worklistStatus.ts` → `deriveWorklistStatus(entry)`). Single source of truth derived from the REAL worklist fields (never hard-coded per screen): CLOSED→**Completed**; OPEN_DOCTOR/STARTED→**In consultation**; arrived+IMPAIRED→**Device issue**; arrived(`enteredAt` set)+READY→**Ready** ("Arrived HH:MM · Waiting N min"); arrived+IN_PROGRESS→**In waiting room** ("Arrived HH:MM · checks in progress"); else→**Not arrived**. Returns `{key,label,detail,icon,chip,arrived,joinable}`. "Arrived" = `enteredAt` is set. The Worklist `StatusCell` and the Home "Today" table both render from this; the Worklist "Waiting" tab = `hasArrived && !completed && not live`. These fields are fed by patient-side events (`PatientEnteredWaitingRoom` → enteredAt+IN_PROGRESS, `DeviceTestCompleted` → READY/NOT_READY+OK/IMPAIRED, `StatusChanged` → consultationStatus) via the physician-service `WorklistProjectionListener`.
- **Worklist** (`WorklistPage.tsx`) — subtle table matching the prototype (tabs All/Waiting/Completed, muted status pills, red-flag as plain text, "Review & join"/"View details"/"View summary").
- **Summary** (`SummaryPage.tsx`) — completed-consultation recap (back link, header + status banner, two-column body, soft-grey info cards, payment breakdown).
- **Patient Context** (`PatientContextPage.tsx`) — "View details"; reads REAL submitted booking details from patient-booking-service via the `/booking-api` proxy (reason, doctor, slot, mode, fee).
- Others present: ConsultationWorkspacePage, DocumentationPage, ReviewPage, SendPage, ClosePage, WithdrawnPage, PreviousPatientsPage.
- **Sidebar (NAV in `App.tsx`)** now = Home → Worklist → Previous patients. The **"Physicians"** item (`/providers`) and its `ProvidersPage`/`ProviderDetailPage` routes + imports were **removed** — scope is Home + Virtual consults only. Login and `*`/`/` redirects now go to **`/home`**.
- Shared detail-page layout primitives: `PageBack`, `PageHeader`, `PageBody`, `Section`, `InfoCard`, `CheckItem`, `KeyValue` (in `ui/primitives.tsx`). Responsive shell: sidebar → off-canvas drawer < 900px.

Design note: `CURRENT_PHYSICIAN` (in `src/data/mockData.ts`) is still used for the static "HIS · {clinic}"
subline in the top bar; the signed-in identity everywhere else comes from the physician session.

---

## 6. D-01 home page — DONE this session

Built `src/pages/HomePage.tsx` (route `/home`) and wired it into `App.tsx` (import + route + NAV
"Home" first + login/default/`*` redirects → `/home`). The "Today · Virtual clinic" table is **synced
with the worklist** (reuses `usePhysicianApi().getWorklist()`, the same scoped `GET
/api/v1/physicians/me/worklist`), so no new backend endpoint was needed. Removed the **"Physicians"**
nav item and its Providers routes/imports (scope is Home + Virtual consults only; **HIS is a separate
system** — morning clinic / ward round / inpatients intentionally omitted). Inbox right-rail is a
static dashboard summary. `.home-grid` responsive rule added to `theme.css`. See Section 5 for details.

Verified: `App.tsx` / `HomePage.tsx` / `theme.css` transform HTTP 200; login returns `doc-khanna`;
worklist returns 7 entries for doc-khanna (so Home's Today card shows 7 for that doctor).

## 6b. D-03 patient review & join page — DONE this session (full Kafka integration)

The physician "review & join" detail page (`PatientReviewPage.tsx`, route `/worklist/:appointmentId/review`)
is populated **entirely from a new Kafka event** — the physician side holds no hard-coded clinical
data (user decision: option B; render only what the payload carries).

**New domain event `ConsultationContextPublished`** (shared `java-common-events`, mirrored IDENTICALLY
in BOTH repos). Flat, PHI-safe, scalar-only payload + one `clinicalContextJson` string (the PHI
contract test rejects field names like `name`/`patientName`; that's why the patient label field is
`patientDisplay` and the rich lists ride as JSON). Registering a new event requires the **catalogue
triple** in sync or the `java-common-events` contract test fails the build:
`EventTypes.java` (constant + `ALL`), `shared/contracts/events/event-catalogue.yaml`,
`telemedicine-platform/infra/kafka/topics.txt` (main + `.DLQ` + `.RETRY`), plus a JSON schema
`shared/contracts/events/schemas/ConsultationContextPublished.v1.json` whose `required` array equals
the record components in order. All five edited in each repo; both repos' contract tests pass.

**Producer (patient-booking-service):** `service/ConsultationContextPublisher.java` assembles the
snapshot from the booking row (reason, mode, fee), the `SymptomCatalog` (symptoms + red flag from
the reason text), the cover-quote fee math (fee/voucher/paid), and `PatientReferenceData`
(age, medical-history lines). Published from `ConsultationFlowService.markReady` (P-16), right after
the waiting-room events. `clinicalContextJson` = `{reasonText, symptoms[], quickCheck[],
medicalHistory[], previousEncounters[], attachments[], sourced}`. Fields the patient backend doesn't
model (severity, duration, structured history, previous encounters, media) are a labelled
`"sourced":"booking-snapshot"` — empty arrays where there's genuinely no data.

**Consumer/projection (physician-service):** new `patient_detail` read model — Flyway
`V106__patient_detail_projection.sql`, `domain/detail/PatientDetailEntity` + `PatientDetailRepository`,
a new `@DomainEventListener(CONSULTATION_CONTEXT_PUBLISHED)` handler `onConsultationContext` added to
`WorklistProjectionListener` (last-writer-wins). **Detail endpoint**
`GET /api/v1/physicians/me/worklist/{appointmentId}` (added to `WorklistController`) →
`service/PatientDetailService` joins `patient_detail` + the live worklist row (readiness/status/
arrival) and is **scoped to the signed-in physician** (404 if the appointment isn't on their
worklist — verified doc-nair gets 404 for doc-khanna's appt). DTO `api/dto/PatientDetailResponse`.

**Frontend:** `api/types.ts` `PatientDetail` + `ClinicalContext`; `physicianApi.getPatientDetail(id)`
hits the detail endpoint directly (same `/api` proxy as the worklist). `PatientReviewPage` renders
header/reason/symptoms/quick-check/history/previous-encounters/payment/readiness all from the payload.
**Join is readiness-gated** via the shared `deriveWorklistStatus`: button shown only when the patient
has arrived (`status.arrived`), enabled only when `status.joinable` (arrived + READY); **not-arrived
patients get no Join button** (the user's requirement). Join calls `admitPatient` then navigates to
`/consultation/:id`. Worklist "View details" and "Review & join" both now route to `/review`.
`deriveWorklistStatus`/`isCompleted`/`hasArrived` were widened to accept a `StatusSource` subset so
WorklistEntry and PatientDetail both feed the one status function.

Verified end-to-end: both repos `java-common-events:test` + service `compileJava` BUILD SUCCESSFUL;
V106 applied; physician-service subscribed to `ConsultationContextPublished`; drove a real patient
`POST :8120/api/consultations/{ref}/ready` → the physician detail endpoint returned the projected
reason/symptoms/age/payment/readiness; fresh appt reaches READY + enteredAt; cross-physician 404.
(Gotcha seen: reusing an appointment id across restarts can get the arrival event deduped by
`@IdempotentConsumer`, leaving enteredAt null — use a fresh ref when smoke-testing arrival. Keep
`nationalIdMasked` ASCII — a `•` bullet mojibakes in this stack.)

### NEXT STEP — more physician screens (D-NN)
The user will share further physician prototype screenshots one at a time. Keep Section 2 decisions
(English / Indian names / INR / Connected Care, match prototype LAYOUT not copy). Approach that has
worked: implement the page, reuse existing scoped endpoints where possible (add to physician-service
only if genuinely new data is needed), then verify (HTTP 200 on the module transform + smoke-test).

---

## 7. Known gotchas / lessons

- **Port/DB collisions** between the `_integration` and legacy copies (physician :8114/:3015/DB physician;
  patient :8120/:3001/DB patient_booking). Run only one of each.
- **White screen = runtime/transform error.** Check the vite terminal output AND the browser console.
  The recurring one here: a duplicate React import or a `CURRENT_PHYSICIAN is not defined` from a
  half-finished login edit in `App.tsx`.
- **Stale Vite cache** can keep serving an old transform error after you fix the file — stop the dev
  server, delete `telemedicine-frontend/node_modules/.vite`, restart.
- **Flyway version collisions** across repos sharing a DB — the DB already had V100-V104; integration
  adds V105. Keep migration SQL ASCII (en-dash `–`/middle-dot `·` → mojibake failures). If a renamed
  migration lingers, delete `services/<svc>/build/resources` so processResources recopies from src.
- **Node 20** required for Nx; **pnpm via corepack** only.
- Dev console output in this environment interleaves oddly — trust HTTP status codes and actual file state.
- Frontend `node_modules` pnpm junctions can dangle if a repo folder is renamed — clean reinstall fixes.

---

## 8. Quick smoke test after (re)starting

```
curl http://localhost:8114/actuator/health                       # UP
curl -X POST http://localhost:8114/api/auth/physician-sign-in -H "Content-Type: application/json" -d "{\"email\":\"arjun.khanna@connectedcare.in\",\"password\":\"doctor123\"}"   # physicianId doc-khanna
curl -H "X-Market: IN" -H "X-Physician-Id: doc-khanna" http://localhost:8114/api/v1/physicians/me/worklist   # 7 items
open http://localhost:3015                                        # login page → sign in → worklist
```
