# ExamForge Backend — Phase 1 through Phase 9

Clean Architecture / Modular Monolith backend for an online competitive-exam
preparation platform. This delivery covers **Phase 1 (Foundation)**,
**Phase 2 (Authentication & RBAC)**, **Phase 3 (Exam, Subject, Topic)**,
**Phase 4 (Question Bank)**, **Phase 5 (Test Builder)**,
**Phase 6 (Test Attempt Engine)**, **Phase 7 (Results, Analytics &
Rankings)**, **Phase 8 (Bookmarks, Notes & Study Material)**, and
**Phase 9 (Test Series & Subscriptions)**.

## Phase 1 recap — Foundation

- Spring Boot 3.3 / Java 21 / Maven project skeleton
- PostgreSQL + Flyway migration pipeline
- Redis wired up for caching
- Standard `ApiResponse<T>` envelope (success/error) used by every endpoint
- `GlobalExceptionHandler` — validation, auth, not-found, duplicate,
  data-integrity, unexpected — never leaks stack traces
- `BaseEntity` (id/created_at/updated_at/created_by/updated_by/deleted_at)
- Swagger/OpenAPI, Actuator health check, Docker + Docker Compose

## Phase 2 — Authentication & RBAC

### What's new

- **`role` module** — `RoleName` enum (`USER`, `CONTENT_CREATOR`, `REVIEWER`,
  `ADMIN`, `SUPER_ADMIN`), `Role` entity, `RoleRepository`. Roles are a
  closed, code-defined set seeded via Flyway (`V5__seed_roles.sql`), not
  admin-creatable — consistent with the RBAC model in the spec.
- **`user` module** — `User` entity (extends `BaseEntity`) with a
  `@ManyToMany` to `Role` via the `user_roles` join table, `UserRepository`,
  `UserService` (registration logic: email/mobile uniqueness checks,
  BCrypt hashing, default `USER` role assignment).
- **`auth` module**:
  - `POST /api/v1/auth/register` — name, email, mobile, password,
    confirmPassword (validated by a reusable `@PasswordMatches` class-level
    constraint in `common.validation`). Creates the account; does **not**
    auto-login — call `/login` afterward. Duplicate email/mobile → 409.
  - `POST /api/v1/auth/login` — authenticates via Spring Security's
    `AuthenticationManager`/`DaoAuthenticationProvider`, issues an access
    token + refresh token pair.
  - `POST /api/v1/auth/refresh` — validates the refresh token, **rotates**
    it (the old one is immediately revoked and cannot be reused — enforced
    via Redis), and returns a new pair.
  - `POST /api/v1/auth/logout` — revokes the given refresh token in Redis.
    Idempotent: logging out with an already-invalid token is a no-op, not
    an error.
  - `GET /api/v1/auth/me` — returns the authenticated user, built entirely
    from the access token's claims (no DB hit).
- **JWT (`common.security` + `jjwt` 0.12.6)**:
  - `JwtTokenProvider` — HS256-signed tokens, each carrying `sub` (user id),
    `email`, `roles`, `type` (`ACCESS`/`REFRESH`), and a `jti`. Access and
    refresh tokens are cross-type-rejecting: an access token cannot be used
    where a refresh token is expected and vice versa.
  - `JwtAuthenticationFilter` — runs once per request; if a valid Bearer
    access token is present, populates the `SecurityContext` **directly
    from the token's claims**, with no database round trip. This is what
    keeps every authenticated request fast and stateless.
  - `RefreshTokenService` — the one piece of server-side state: Redis holds
    `refresh_token:{jti} -> userId` with a TTL matching the token's own
    expiry. This is what makes rotation and logout-revocation possible for
    otherwise-stateless JWTs.
  - `JwtAuthenticationEntryPoint` / `CustomAccessDeniedHandler` — make sure
    401s and 403s coming out of the *security filter chain itself* (before
    a request ever reaches a controller) still return the same
    `ApiResponse` JSON envelope as everything else, not Spring Security's
    default HTML/plain-text error page.
- **RBAC enforcement**:
  - `SecurityConfig` now defaults to `anyRequest().authenticated()`. Public
    routes (`/auth/register`, `/auth/login`, `/auth/refresh`, Swagger,
    actuator health, `/api/v1/system/**`) are explicitly whitelisted.
  - `/api/v1/admin/**` requires `ADMIN` or `SUPER_ADMIN` at the URL-pattern
    level, backed up by `@PreAuthorize` on the endpoint itself (defense in
    depth) — see `AdminPingController`, a minimal proof-of-life endpoint;
    real admin functionality arrives with the modules that need it.
- **Audit logging (lightweight, interim)** — `REGISTER`, `LOGIN`,
  `TOKEN_REFRESH`, and `LOGOUT` are written as structured log lines via a
  dedicated `com.examforge.audit` SLF4J logger in `AuthServiceImpl`. This is
  **not** yet persisted to the `audit_logs` table from the spec — that
  table and a real `AuditLogService` are more valuable once there are admin
  mutations worth auditing in detail (Phase 9), so full audit persistence
  is deferred there rather than built against an empty use case now.

### New database tables (`V2`–`V5`)

```
roles              (id, name, description, + BaseEntity columns)
permissions        (id, name, description, + BaseEntity columns)  -- schema only, not yet enforced in code
role_permissions   (role_id, permission_id)                        -- schema only, not yet enforced in code
users              (id, name, email, mobile, password_hash, active, email_verified, + BaseEntity columns)
user_roles         (user_id, role_id)
```

`permissions` / `role_permissions` are created now so a future move to
fine-grained permission checks (beyond role-level) doesn't need a breaking
migration, but Phase 2 only enforces **role**-based access.

### New environment variables

None beyond what Phase 1 already defined — `JWT_SECRET`,
`JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION` were already in
`.env.example` and `application.yml`, just unused until now.

**Important:** the default `JWT_SECRET` in `.env.example` is a placeholder.
`JwtTokenProvider` logs a warning on startup if the configured secret is
under 256 bits — replace it with a real random value before any shared or
production deployment.

### Design decisions worth knowing about

- **Register does not auto-login.** Simpler contract, matches the spec's
  separate register/login endpoints. If you'd rather auto-login on
  register, that's a small change to `AuthController.register`.
- **Access tokens are validated statelessly** (signature + expiry + type
  only) — they are *not* checked against Redis or the database on every
  request. This is the standard JWT tradeoff: a compromised access token
  is valid until it expires (kept short, 15 min by default) or the account
  is deactivated and re-issued tokens are refused at `/refresh`. Only
  refresh tokens are tracked server-side, since that's where
  rotation/revocation actually needs to happen.
- **User enumeration protection**: `CustomUserDetailsService` throwing
  `UsernameNotFoundException` for an unknown email is converted by
  `DaoAuthenticationProvider` (default `hideUserNotFoundExceptions=true`)
  into the same `BadCredentialsException` as a wrong password — both cases
  return an identical "Invalid credentials" response.

## Commands to run

```bash
cp .env.example .env
mvn clean install
mvn test        # needs Docker running - Testcontainers spins up Postgres AND Redis now
mvn spring-boot:run
```

Or via Docker Compose: `docker compose up --build`.

As with Phase 1: Maven and Maven Central were not reachable from the
sandbox this was built in, so the build could not be executed here. Please
run `mvn clean install` and `mvn test` locally before moving to Phase 3.

## Trying it out

```bash
# Register
curl -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "name": "Priya Sharma", "email": "priya@example.com", "mobile": "9876543210",
  "password": "SecurePass123", "confirmPassword": "SecurePass123"
}'

# Login
curl -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{
  "email": "priya@example.com", "password": "SecurePass123"
}'
# -> { "data": { "accessToken": "...", "refreshToken": "...", ... } }

# Authenticated request
curl localhost:8080/api/v1/auth/me -H 'Authorization: Bearer <accessToken>'

# Refresh (rotates - the old refreshToken becomes unusable)
curl -X POST localhost:8080/api/v1/auth/refresh -H 'Content-Type: application/json' -d '{
  "refreshToken": "<refreshToken>"
}'

# Logout
curl -X POST localhost:8080/api/v1/auth/logout -H 'Content-Type: application/json' -d '{
  "refreshToken": "<refreshToken>"
}'
```

## Tests

- `ExamForgeApplicationTests` — full context + Flyway migrations against
  Testcontainers Postgres.
- `SystemControllerTest` — response envelope shape; now also distinguishes
  a 401 on an unauthenticated protected route from a 404 on an unmatched
  *public* route, since Phase 2 locks down `anyRequest().authenticated()`.
- `JwtTokenProviderTest` — pure unit test (no Spring context): access vs.
  refresh type enforcement, tampered-signature rejection, malformed-token
  rejection.
- `AuthControllerIntegrationTest` — full register → login → `/me` →
  refresh-rotation → old-token-rejected → logout → post-logout-refresh-
  rejected flow; duplicate-registration conflict; wrong-password rejection;
  mismatched-password validation; RBAC allow/deny on `/api/v1/admin/ping`.

All integration tests run against Testcontainers-managed **Postgres and
Redis** — no local services required beyond Docker.

## Phase 3 — Exam, Subject, Topic

### What's new

- **New tables (`V6`–`V9`)**: `exam_categories`, `exams`, `subjects`
  (FK → `exams`, unique `(exam_id, name)`), `topics` (FK → `subjects`,
  unique `(subject_id, name)`).
- **`exam` module**:
  - `ExamCategory` / `Exam` entities. Both carry
    `@SQLRestriction("deleted_at IS NULL")`, so every query — repository
    methods, Specifications, everything — automatically excludes
    soft-deleted rows without each query needing to remember to filter
    for it.
  - `Exam` fields match the spec: category (FK, optional), name,
    description, duration (minutes), total questions, maximum marks,
    negative marking, difficulty (`EASY`/`MEDIUM`/`HARD`), status
    (`DRAFT`/`PUBLISHED`/`ARCHIVED`).
  - `ExamSpecifications` — composable JPA `Specification<Exam>` predicates
    (category, difficulty, keyword, status) that chain together with
    `Specification.where(...).and(...)`, each a no-op when its filter
    value is absent. This is what backs both the public and admin search
    endpoints without duplicating filter logic.
  - Public: `GET /api/v1/exams` (paginated, filterable by `categoryId`,
    `difficulty`, `keyword`, sortable), `GET /api/v1/exams/{id}` — **both
    always restricted to `PUBLISHED`** exams; a draft or archived exam
    404s on the public endpoint even if you know its id.
  - Admin: `GET /api/v1/admin/exams` (same filters plus `status`, all
    statuses visible), `GET /api/v1/admin/exams/{id}` (any status),
    `POST` / `PUT` / `DELETE /api/v1/admin/exams/{id}`.
  - `GET /api/v1/exam-categories` (public list) and
    `POST/PUT/DELETE /api/v1/admin/exam-categories` (admin CRUD).
- **`subject` module**: subjects nest under an exam.
  `GET /api/v1/exams/{examId}/subjects` (public — 404s if the parent exam
  isn't published) and `GET/POST/PUT/DELETE /api/v1/admin/subjects`
  (admin, any exam status, `examId` passed as a query param for listing
  or in the request body for create/update).
- **`topic` module**: topics nest under a subject, exact same pattern one
  level down — `GET /api/v1/subjects/{subjectId}/topics` (public, 404s
  unless the *grandparent* exam is published) and
  `GET/POST/PUT/DELETE /api/v1/admin/topics`.
- **Referential-integrity guards on delete** (new `ResourceInUseException`,
  409 Conflict): you can't delete an exam category while exams still
  reference it, can't delete an exam while subjects still exist under it,
  can't delete a subject while topics still exist under it. Deletes are
  always soft (`deleted_at` is set, row stays), never hard deletes — the
  `@SQLRestriction` on each entity is what then makes them disappear from
  every subsequent query.
- **Pagination envelope**: new `common.response.PageResponse<T>` record
  wraps every paginated list response consistently
  (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`,
  `last`) instead of leaking Spring Data's own verbose `Page` JSON shape.

### Design decisions worth knowing about

- **Drafts are genuinely invisible publicly, not just hidden from
  listings.** Both the list endpoint *and* the single-resource `GET` by
  id enforce `status == PUBLISHED` server-side — an unpublished exam's id
  leaking (e.g. in a browser history or a support ticket) doesn't let
  anyone read its content early.
- **Subjects/topics inherit their exam's publish gate.** A subject list
  under a still-draft exam 404s exactly like the exam itself would,
  rather than accidentally exposing the structure of unpublished content.
  Once the exam is published, its existing subjects/topics become visible
  immediately — no separate "publish" step needed for children.
- **Soft delete is blocked, not cascaded, when children exist.** Rather
  than silently soft-deleting an entire exam → subjects → topics tree in
  one call (easy to do by accident), each level requires you to delete
  bottom-up. This mirrors the `ON DELETE CASCADE` foreign keys (which
  exist for defensive integrity at the DB layer) while keeping the
  *application-level* soft-delete behavior deliberate and reversible one
  step at a time.
- **`CONTENT_CREATOR`/`REVIEWER` roles are still unused** by any endpoint.
  They exist in the `roles` table since Phase 2 but the workflow that
  needs them (`DRAFT → IN_REVIEW → APPROVED → PUBLISHED` for individual
  *questions*, not exams) is Phase 4's job.

### Trying it out

```bash
# Create a category (admin token required)
curl -X POST localhost:8080/api/v1/admin/exam-categories \
  -H "Authorization: Bearer <adminAccessToken>" -H 'Content-Type: application/json' \
  -d '{"name": "Banking", "description": "Bank PO/Clerk exams"}'

# Create an exam (defaults to DRAFT - invisible publicly until published)
curl -X POST localhost:8080/api/v1/admin/exams \
  -H "Authorization: Bearer <adminAccessToken>" -H 'Content-Type: application/json' \
  -d '{"categoryId":"<categoryId>","name":"SSC CGL","durationMinutes":60,"totalQuestions":100,"maximumMarks":200,"negativeMarking":0.25,"difficulty":"MEDIUM"}'

# Publish it
curl -X PUT localhost:8080/api/v1/admin/exams/<examId> \
  -H "Authorization: Bearer <adminAccessToken>" -H 'Content-Type: application/json' \
  -d '{"categoryId":"<categoryId>","name":"SSC CGL","durationMinutes":60,"totalQuestions":100,"maximumMarks":200,"negativeMarking":0.25,"difficulty":"MEDIUM","status":"PUBLISHED"}'

# Now it's visible to everyone, no auth needed
curl "localhost:8080/api/v1/exams?difficulty=MEDIUM&page=0&size=20"
curl localhost:8080/api/v1/exams/<examId>
```

### Tests

- `ExamCatalogIntegrationTest` — the full lifecycle: category creation,
  duplicate-name rejection, exam creation as DRAFT, confirms it's absent
  from the public list and 404s on public single-get, admin can still
  fetch it, publishing flips visibility, public list/filter finds it,
  category deletion blocked while referenced, subject creation +
  duplicate rejection, public nested subject listing, exam deletion
  blocked while subject exists, topic creation, public nested topic
  listing, subject deletion blocked while topic exists, then the full
  bottom-up unwind (topic → subject → exam → category, all succeeding),
  and confirms the exam is gone afterward. Plus separate tests for
  payload validation and public category-list readability.

Runs the same way as before - `mvn test` (Testcontainers Postgres + Redis,
no local services needed).

## Phase 4 — Question Bank

### What's new

- **New tables (`V10`–`V13`)**: `questions` (FK → `topics`, `ON DELETE
  RESTRICT`), `question_options` (FK → `questions`, cascade), `question_tags`
  (simple `(question_id, tag)` pairs - no separate tags master table),
  `question_translations` (FK → `questions`, unique per `(question_id,
  language)`).
- **`question` module**:
  - `Question` entity: `topic` (FK), `questionType`
    (`MCQ`/`MULTIPLE_CHOICE`/`TRUE_FALSE`/`NUMERIC`), `questionText`,
    `explanation`, `difficulty` (reuses `ExamDifficulty` from the `exam`
    module rather than duplicating the enum), `marks`, `negativeMarks`,
    `language`, `status`, `rejectionReason`, `correctNumericAnswer`
    (only meaningful for `NUMERIC`), a `List<QuestionOption>` (cascaded,
    orphan-removal - replacing a question's options on update deletes the
    old rows), and a `Set<String> tags`.
  - `QuestionOption`: text, `correct` (boolean), display order. Deliberately
    *not* a `BaseEntity` - it's a tightly-owned child of `Question` with no
    independent lifecycle, so it skips the audit/soft-delete columns that
    don't apply to it.
  - `QuestionAnswerValidator` — enforces the answer-key shape per question
    type as a business rule (not bean validation, since it depends on
    `questionType`): `MCQ` needs ≥2 options with exactly one correct,
    `MULTIPLE_CHOICE` needs ≥2 options with ≥1 correct, `TRUE_FALSE` needs
    exactly 2 options with exactly one correct, `NUMERIC` needs
    `correctNumericAnswer` and no options at all.
  - `QuestionSpecifications` — same composable-filter pattern as
    `ExamSpecifications`, filtering by topic, subject (via
    `topic.subject.id`), status, difficulty, type, and keyword.
- **The content workflow** (this is what finally puts the
  `CONTENT_CREATOR` and `REVIEWER` roles seeded back in Phase 2 to use):
  ```
  DRAFT ──submit──▶ IN_REVIEW ──approve──▶ APPROVED ──publish──▶ PUBLISHED
    ▲                   │
    └──────reject────────┘
  ```
  Plus `ARCHIVED` as a terminal state reachable from `PUBLISHED`,
  `APPROVED`, or `REJECTED`. Each transition is its own endpoint under
  `/api/v1/questions/{id}/...` (`submit-for-review`, `approve`, `reject`,
  `publish`, `archive`), independently role-gated:
  - `submit-for-review`: `CONTENT_CREATOR` (own questions only),
    `ADMIN`/`SUPER_ADMIN` (any).
  - `approve` / `reject`: `REVIEWER`, `ADMIN`, `SUPER_ADMIN` — **and a
    `REVIEWER` cannot approve or reject their own submission**, even if
    the same account also holds `CONTENT_CREATOR` (this ownership check
    is enforced in the service layer, independent of the role gate, and
    is specifically what `reviewerCannotApproveTheirOwnSubmission...`
    in the test suite exercises).
  - `publish`: `ADMIN`/`SUPER_ADMIN` only - the final go-live gate is
    reserved for the highest-trust role, per the design decision flagged
    back in the Phase 3 notes.
  - `archive`: `ADMIN`/`SUPER_ADMIN` only.
  - Editing (`PUT`) and deleting (`DELETE`) are only permitted while a
    question is `DRAFT` or `REJECTED` (edit) / `DRAFT` (delete) - once
    content enters review, it's immutable until rejected back or it goes
    all the way to `PUBLISHED`/`ARCHIVED`.
  - **Even `ADMIN`/`SUPER_ADMIN` cannot skip states** (e.g. `DRAFT` straight
    to `PUBLISHED`). Admin privilege means "can act on anyone's content and
    isn't blocked by ownership checks," not "can bypass the workflow" -
    this keeps the guarantee that published content always went through
    review, full stop.
- **`GET/POST/PUT/DELETE /api/v1/questions/{questionId}/translations`** -
  minimal CRUD for alternate-language variants of a question.
- **New shared utility**: `common.security.SecurityUtils` - reads the
  current `UserPrincipal` off the `SecurityContext` (id, email, role
  check) for the ownership logic above. Reusable anywhere else in the
  codebase that needs "who is making this request" without re-threading
  `Authentication` through every method signature.
- **Referential integrity carried down one more level**: `TopicServiceImpl.delete()`
  now also blocks (409) while questions exist under that topic, extending
  the same bottom-up delete chain from Phase 3 (topic → subject → exam →
  category) one level further.

### Design decisions worth knowing about

- **This is not a public catalog.** Every `/api/v1/questions/**` endpoint
  requires `CONTENT_CREATOR`, `REVIEWER`, `ADMIN`, or `SUPER_ADMIN` -
  regular users never call these directly. They'll encounter question
  *content* only through the Test/Attempt endpoints Phase 5/6 introduces.
- **`QuestionResponse` is the full, answer-including view, on purpose** -
  every current caller is legitimately authorized to see the answer key
  (that's the whole point of authoring and review). It carries an explicit
  warning in its Javadoc: **the Test/Attempt module must not reuse this
  DTO**. It needs its own answer-free response shape, enforced by which
  endpoint/role is calling, not a client-supplied "hide answers" flag
  (a flag the client controls is not a security boundary). This is
  flagged again in the Phase 5 plan below so it isn't missed.
- **Difficulty reuses `ExamDifficulty`** rather than introducing a second,
  identical `EASY`/`MEDIUM`/`HARD` enum - one concept, one definition,
  referenced from both `exam` and `question`.
- **Tags have no master table.** The spec lists `question_tags` as a
  table without describing tag management (no `/api/v1/tags` CRUD, no
  tag normalization/autocomplete). Modeling it as a plain
  `@ElementCollection<String>` matches what's actually specified without
  inventing a tag-administration feature nobody asked for; a proper `tags`
  master table with slug normalization is a reasonable future addition
  if free-text tags turn out to be too messy in practice.

### Trying it out

```bash
# Content creator drafts an MCQ (topicId from Phase 3's catalog)
curl -X POST localhost:8080/api/v1/questions \
  -H "Authorization: Bearer <creatorToken>" -H 'Content-Type: application/json' -d '{
    "topicId": "<topicId>", "questionType": "MCQ", "questionText": "What is 2 + 2?",
    "difficulty": "EASY", "marks": 1, "negativeMarks": 0.25,
    "options": [
      {"optionText": "3", "correct": false},
      {"optionText": "4", "correct": true},
      {"optionText": "5", "correct": false}
    ]
  }'

# Submit for review
curl -X POST localhost:8080/api/v1/questions/<id>/submit-for-review -H "Authorization: Bearer <creatorToken>"

# A different user (REVIEWER) approves it
curl -X POST localhost:8080/api/v1/questions/<id>/approve -H "Authorization: Bearer <reviewerToken>"

# Admin publishes
curl -X POST localhost:8080/api/v1/questions/<id>/publish -H "Authorization: Bearer <adminToken>"
```

### Tests

- `QuestionWorkflowIntegrationTest` — the full lifecycle (draft → blocked
  cross-user edit → submit → blocked delete once out of DRAFT → blocked
  self-approve via role gate → reject with reason → edit again while
  REJECTED → resubmit → approve → blocked reviewer-publish → admin
  publish → topic-delete now blocked → archive), a dedicated test for the
  same-person ownership block on `approve` using a dual-role account (the
  one case the role gate alone can't catch), and answer-key validation
  (MCQ with zero correct options, NUMERIC without a numeric answer, valid
  NUMERIC succeeding).

Runs the same way as before - `mvn test`.

## Phase 5 — Test Builder

### What's new

- **New tables (`V14`–`V16`)**: `tests` (FK → `exams`, `ON DELETE
  RESTRICT`), `test_sections` (FK → `tests`, unique `(test_id, name)`),
  `test_questions` (FK → `tests` cascade, FK → `questions` restrict, FK →
  `test_sections` nullable/`SET NULL`, unique `(test_id, question_id)`).
- **A naming note worth knowing about**: the entity is called `TestPaper`,
  not `Test`. A class named `Test` in the same package tree as JUnit
  tests collides with `org.junit.jupiter.api.Test` the moment both are
  imported unqualified in the same file — so the Java class is
  `TestPaper` while the table stays `tests` and the API path stays
  `/api/v1/tests`, exactly per the spec's naming. Purely a Java-naming
  workaround, invisible from the API surface.
- **`test` module**:
  - `TestPaper`: exam FK, name, description, duration, total marks,
    negative marking, status (`DRAFT`/`PUBLISHED`/`ARCHIVED`),
    `free`/`price`.
  - `TestSection`: name, optional per-section duration (a section without
    its own timer just uses the whole test's duration), target question
    count, target marks, display order.
  - `TestQuestion`: the join between a test and a question, carrying its
    own `marks`/`negativeMarks` (defaulting to the question's own values
    but overridable per-test) and `displayOrder`. Like `QuestionOption`
    from Phase 4, it's *not* a `BaseEntity` — a pure association row with
    no independent soft-delete lifecycle; detaching a question from a
    test is a hard delete of this row, not a soft one.
- **Manual assembly**: `POST /api/v1/admin/tests/{id}/questions` attaches
  one question at a time (`PUT`/`DELETE .../{testQuestionId}` to
  update the override/order or detach). Only `PUBLISHED` questions can be
  attached — attaching a `DRAFT`/`IN_REVIEW` question is rejected.
- **Automatic assembly**: `POST /api/v1/admin/tests/{id}/questions/auto-generate`
  takes a list of buckets (`{topicId or subjectId, difficulty, count}`),
  randomly selects that many `PUBLISHED` questions per bucket (reusing
  `QuestionSpecifications` from Phase 4 directly — no duplicated filter
  logic), and attaches them. **Strictly all-or-nothing**: if any single
  bucket can't be fully satisfied, the whole call fails with 409 and
  *nothing* is added — no silent partial fill, no half-built test left in
  an ambiguous state. Buckets within one call also can't double-select
  the same question, and already-attached questions are excluded from the
  candidate pool.
- **Public test catalog**: `GET /api/v1/tests` / `GET /api/v1/tests/{id}`
  — same `PUBLISHED`-only pattern as exams, and additionally requires the
  **parent exam** to also be `PUBLISHED` (same inheritance pattern
  subjects/topics used in Phase 3).
- **Referential integrity, same chain as before**: deleting a test is
  blocked while sections or questions are still attached; deleting an
  exam is now also blocked while tests reference it (extending the
  category → exam → subject → topic → question chain one more branch).

### Design decisions worth knowing about — and the answer-leak question, resolved

- **The public test endpoints expose zero question content — not even
  redacted.** This is the resolution to the risk flagged repeatedly since
  Phase 4: rather than building a "safe" question DTO now and hoping every
  future caller remembers to use it, Phase 5 sidesteps the risk entirely
  by not serving question content through the test catalog at all.
  `TestPaperResponse` only ever contains metadata (name, duration, total
  marks, pricing) — a student browsing `/api/v1/tests` cannot see a single
  question, correct or not, until they actually start an attempt.
  **This is a deliberate deferral, not a resolution of the underlying
  need** — the Attempt module (Phase 6) still has to build a genuine
  answer-free question DTO for serving content *during* an active
  attempt, and that DTO still must never be `question.dto.QuestionResponse`
  or `test.dto.TestQuestionResponse` (both of which legitimately include
  answers, since only ADMIN/SUPER_ADMIN ever call the endpoints that
  return them). Flagged again in the Phase 6 plan below.
- **`totalMarks` and `negativeMarking` on `TestPaper` are declared
  values, not computed ones** — same pattern as `Exam.maximumMarks` from
  Phase 3. The admin states the intended total when creating the test;
  the system doesn't currently cross-check it against the sum of attached
  `test_questions.marks`. A "does this test's declared total match its
  actual content" validation endpoint would be a reasonable future
  addition once there's a concrete "finalize test" step in the admin
  workflow, but isn't invented here without that context.
- **Manual attach and auto-generate both hard-require `PUBLISHED`
  questions** — this is where the Phase 4 content workflow actually pays
  off: nothing reaches a test unless it went through the full
  draft → review → approve → publish pipeline.

### Trying it out

```bash
# Create a test under a published exam
curl -X POST localhost:8080/api/v1/admin/tests \
  -H "Authorization: Bearer <adminToken>" -H 'Content-Type: application/json' -d '{
    "examId": "<examId>", "name": "SSC CGL Mock 1", "durationMinutes": 60,
    "totalMarks": 100, "negativeMarking": 0.25, "free": true
  }'

# Auto-generate: 20 easy + 20 medium + 10 hard from a subject
curl -X POST localhost:8080/api/v1/admin/tests/<testId>/questions/auto-generate \
  -H "Authorization: Bearer <adminToken>" -H 'Content-Type: application/json' -d '{
    "buckets": [
      {"subjectId": "<subjectId>", "difficulty": "EASY", "count": 20},
      {"subjectId": "<subjectId>", "difficulty": "MEDIUM", "count": 20},
      {"subjectId": "<subjectId>", "difficulty": "HARD", "count": 10}
    ]
  }'

# Publish, then it's visible publicly (metadata only)
curl -X PUT localhost:8080/api/v1/admin/tests/<testId> -H "Authorization: Bearer <adminToken>" \
  -H 'Content-Type: application/json' -d '{ "...": "same fields as create, plus", "status": "PUBLISHED" }'
curl "localhost:8080/api/v1/tests?examId=<examId>"
```

### Tests

- `TestBuilderIntegrationTest` — manual assembly lifecycle (draft hidden
  publicly → attach → duplicate-attach rejected → delete blocked while
  attached → publish → now visible, and the public payload is asserted to
  contain no `questionText` or `correct` field anywhere); auto-generate
  all-or-nothing behavior (over-requesting a scarce difficulty bucket
  fails with nothing added, verified by re-checking the test's question
  list is still empty; a satisfiable request succeeds; re-running the
  same buckets afterward correctly fails since those questions are now
  already attached); and an RBAC check that a plain user can't touch
  `/api/v1/admin/tests` at all.

Runs the same way as before - `mvn test`.

## Phase 6 — Test Attempt Engine

### A security property worth calling out explicitly

Phase 5's `SecurityConfig` whitelists `/api/v1/tests/*` as a public route
(single path segment) so `GET /api/v1/tests/{id}` doesn't need auth. Phase
6 then added `POST /api/v1/tests/{testId}/attempts` one segment deeper.
Before wiring the new controller in, I verified precisely how Spring's Ant-style
path matching treats a single `*` versus `**`: `*` matches within one path
segment only and does not cross a `/`, so `/api/v1/tests/*` matches
`/api/v1/tests/{id}` but does **not** match
`/api/v1/tests/{id}/attempts` - that route correctly falls through to the
default `anyRequest().authenticated()` rule and requires a valid Bearer
token. `unauthenticated_cannotStartAttempt` in the test suite is the
regression test for this. Calling it out explicitly rather than silently
relying on it, since a wildcard whitelist pattern is exactly the kind of
thing that's easy to get subtly wrong as new nested routes get added
under an already-public prefix - worth re-checking any time a future
phase adds routes under `/api/v1/exams/**` or `/api/v1/subjects/**` too.

### What's new

- **New tables (`V17`–`V19`)**: `test_attempts` (FK → `users`, `tests`,
  both `RESTRICT`; a **partial unique index** on `(user_id, test_id)
  WHERE status = 'IN_PROGRESS'` as a DB-level backstop against ever
  having two simultaneous in-progress attempts for the same user+test),
  `attempt_answers` (FK → `test_attempts` cascade, `questions` restrict,
  unique `(attempt_id, question_id)` - one answer row per question),
  `attempt_answer_selected_options` (join table - see the generalization
  note below).
- **`attempt` module**:
  - `TestAttempt`: user, test, `startedAt`, `expiresAt` (backend-computed,
    never client-supplied), `submittedAt`, `status`
    (`IN_PROGRESS`/`SUBMITTED`/`AUTO_SUBMITTED`/`ABANDONED` - `ABANDONED`
    is defined per the spec but not reachable by any endpoint yet; reserved
    for a future explicit "abandon" action), `score`, `correctCount`,
    `incorrectCount`, `unansweredCount`, `accuracy`. Deliberately **not**
    a `BaseEntity` - an attempt is a permanent academic record, never
    soft-deleted, and `created_by`/`updated_by` would be redundant with
    the `user_id` column it already has.
  - `AttemptAnswer`: attempt, question, `selectedOptionIds`, `numericAnswer`,
    `markedForReview`, `answeredAt`, `timeSpentSeconds`.
  - **A deliberate generalization from the spec**: the spec's
    `attempt_answers` table describes a singular `selectedOption`, which
    fits MCQ/TRUE_FALSE but not `MULTIPLE_CHOICE` (which the question bank
    has supported since Phase 4 and needs multiple correct selections).
    Generalized to `Set<UUID> selectedOptionIds` via a join table -
    MCQ/TRUE_FALSE answers simply end up with exactly one row in it.
- **THE answer-free question DTO, finally built for real** —
  `QuestionAttemptResponse` / `QuestionAttemptOptionResponse` in the
  `attempt.dto` package. No `correct` field on any option, no
  `explanation`, no `correctNumericAnswer`. This is the shape
  `GET /api/v1/attempts/{attemptId}/questions` serves, and it is
  completely separate from `question.dto.QuestionResponse` and
  `test.dto.TestQuestionResponse` - neither of which this module reuses,
  exactly as flagged since Phase 4. `AttemptIntegrationTest` specifically
  asserts the questions payload never contains the strings `"correct"`,
  `explanation`, `correctNumericAnswer`, or the actual explanation text -
  the check that's been deferred twice now finally has teeth.
- **Backend-authoritative expiry, checked lazily on every access** —
  there's no background scheduler in this phase; instead, every operation
  on an attempt (`getQuestions`, `saveAnswer`, `submit`, `getResult`)
  starts by calling a private `checkAndAutoSubmitIfExpired()` that
  compares `Instant.now()` against the stored `expiresAt` and, if past
  due, immediately scores and finalizes the attempt as `AUTO_SUBMITTED`
  before doing anything else. A client-side timer is purely cosmetic; the
  backend is what actually ends the attempt, and it does so the moment
  anyone touches it after the deadline, not on some later polling cycle.
  A true background sweep for *never-touched-again* abandoned attempts
  (one nobody ever calls back into) is a reasonable future addition and
  is called out again in the Phase 7 plan below.
- **Start attempt is idempotent** — calling
  `POST /api/v1/tests/{testId}/attempts` while an `IN_PROGRESS` attempt
  already exists for that user+test returns the *same* attempt (resumes)
  rather than erroring or creating a duplicate. If that existing attempt
  turns out to be expired at the moment of the check, it's auto-submitted
  and a genuinely new attempt is started in its place.
- **Server-side scoring, never a client-supplied score**:
  `POST /api/v1/attempts/{attemptId}/submit` computes everything from the
  stored `attempt_answers` against the question bank's actual answer key.
  Grading policy per type: MCQ/TRUE_FALSE require the selected set to
  equal the single correct option exactly; `MULTIPLE_CHOICE` requires an
  exact match against the full correct-option set (no partial credit -
  a defensible default, not the only possible one); `NUMERIC` compares via
  `BigDecimal.compareTo()` (value-equal regardless of trailing-zero scale
  differences, not `.equals()` which is scale-sensitive). An unanswered
  question contributes zero either way; an answered-but-wrong question
  applies the negative marking.
- **Result and per-question review are the same endpoint** —
  `GET /api/v1/attempts/{attemptId}/result` returns both the summary
  (score, max score, percentage, accuracy, counts) and a `questions` array
  with full per-question review (selected vs. correct answer, explanation,
  marks awarded), consolidating the spec's separately-described "Result"
  and "Question Review" sections into one response rather than a second
  endpoint that would just repeat most of the same joins. This DTO
  (`QuestionReviewResponse`) legitimately includes the answer key, since
  by definition it's only ever returned once the attempt is no longer
  `IN_PROGRESS`.
- **Ownership is enforced by hiding existence, not by 403** - if attempt
  ID `X` belongs to someone else, every endpoint returns 404, the same
  pattern used for draft exams/questions elsewhere in the codebase, so a
  guessed or leaked attempt ID doesn't even confirm it exists.

### Trying it out

```bash
# Start (or resume) an attempt
curl -X POST localhost:8080/api/v1/tests/<testId>/attempts -H "Authorization: Bearer <userToken>"

# Get the answer-free question list
curl localhost:8080/api/v1/attempts/<attemptId>/questions -H "Authorization: Bearer <userToken>"

# Save an answer
curl -X POST localhost:8080/api/v1/attempts/<attemptId>/answers \
  -H "Authorization: Bearer <userToken>" -H 'Content-Type: application/json' \
  -d '{"questionId": "<questionId>", "selectedOptionIds": ["<optionId>"]}'

# Submit - score computed server-side
curl -X POST localhost:8080/api/v1/attempts/<attemptId>/submit -H "Authorization: Bearer <userToken>"

# Full result + per-question review, now that it's submitted
curl localhost:8080/api/v1/attempts/<attemptId>/result -H "Authorization: Bearer <userToken>"
```

### Tests

- `AttemptIntegrationTest`:
  - `fullLifecycle_correctAndIncorrectAnswers_scoredServerSide` — start →
    idempotent resume on a second start call → fetch questions with the
    leak-free assertion (no `"correct"`, no `explanation`, no
    `correctNumericAnswer`, no explanation text anywhere in the payload)
    → answer one MCQ question correctly and one NUMERIC question
    incorrectly → result blocked with 403 pre-submission → submit →
    score verified as exactly `+1.00 (correct MCQ) + 0.00 (incorrect
    NUMERIC with no negative marking configured) = 1.00`, with
    `correctCount=1`, `incorrectCount=1`, `unansweredCount=0` → resubmission
    rejected with 403 → result now available and *does* legitimately
    contain the answer key and explanation text.
  - `expiredAttempt_isAutoSubmittedByBackend_notFrontend` — backdates
    `expiresAt` directly via the repository (simulating time passing
    without waiting in real time) and confirms the very next request -
    not a background job - is what flips the attempt to `AUTO_SUBMITTED`
    and computes its score.
  - `attemptIsOwnerOnly_otherUsersGet404NotForbidden` — a second user
    hitting someone else's attempt id gets 404.
  - `unauthenticated_cannotStartAttempt` — the regression test for the
    path-matching property described above.

Runs the same way as before - `mvn test`.

## Phase 7 — Results, Analytics & Rankings

### What's new

- **`result` module** — no new tables; a thin read layer over what
  `attempt.service.AttemptServiceImpl` already computes.
  `GET /api/v1/users/me/attempts` (a user's own attempt history across
  every test, paginated) and `GET /api/v1/tests/{testId}/attempts/me`
  (their attempts on one specific test - useful for a retake-history
  view). Both scoped to the caller via the same `SecurityUtils` pattern
  used everywhere else; no admin bypass, same as the attempt endpoints
  themselves. The spec's `GET /api/v1/attempts/{attemptId}/result` was
  already built in Phase 6 (it's the canonical single-result endpoint
  per the spec's own Result Module section) - this phase adds the
  *history/listing* layer around it, not a duplicate of it.
- **`analytics` module** — admin-only aggregate dashboards under
  `/api/v1/admin/analytics/**`:
  - `GET /tests/{testId}` — total/completed/in-progress attempt counts,
    average score, average accuracy, completion rate for one test.
  - `GET /popular-tests` / `GET /popular-exams` — top N by attempt count.
  - `GET /user-registrations?days=N` — daily registration counts.
  All backed by real SQL aggregation (`COUNT`, `AVG`, `GROUP BY`) added
  directly to `TestAttemptRepository` and `UserRepository` — no
  in-memory aggregation over fetched rows — and every method is
  `@Cacheable` (Redis, ~10 min TTL via the cache defaults from Phase 1's
  `RedisConfig`), since dashboard data doesn't need millisecond
  freshness and this is exactly the query pattern that shouldn't hit
  Postgres on every page load.
- **`ranking` module** — `GET /api/v1/tests/{testId}/ranking`, public
  (same `PUBLISHED`-test-under-`PUBLISHED`-exam visibility gate as the
  rest of the public catalog). One entry per user - their **best**
  completed attempt, not every attempt - via a Postgres `DISTINCT ON`
  native query (`findBestAttemptPerUser` in `TestAttemptRepository`),
  ordered by score then accuracy, with rank computed from page offset.
  The response DTO (`RankingResponse`) is deliberately narrow: rank,
  user id, user **name**, score, accuracy - no email, no mobile, per the
  spec's explicit "do not expose unnecessary personal information" for
  this endpoint.
- **Background sweep for genuinely-abandoned attempts** — a new
  `AttemptSweepJob` (`@Scheduled`, interval configurable via
  `ATTEMPT_SWEEP_INTERVAL_MS`, default 60s) periodically finds every
  `IN_PROGRESS` attempt past its `expiresAt` and finalizes it as
  `AUTO_SUBMITTED`, backed by a new `AttemptService.autoSubmitAllExpired()`
  method. This is the piece flagged since Phase 6: the lazy per-request
  check in `checkAndAutoSubmitIfExpired()` only fires when someone
  touches their own attempt again after time's up — an attempt nobody
  ever revisits (browser closed, never reopened) would otherwise sit
  `IN_PROGRESS` in the database indefinitely. `@EnableScheduling` added
  to `ExamForgeApplication`.

### Design decisions worth knowing about

- **Ranking shows one entry per user, not one per attempt.** A user who
  retakes a test only ever appears once, at their best score — otherwise
  a leaderboard would let someone pad it with repeated low-scoring
  attempts, and the "your best result" framing is what students actually
  want from a leaderboard anyway.
- **Analytics endpoints only ever return aggregates** — counts, averages,
  totals. There's no endpoint anywhere in this phase (or any prior one)
  that lets an admin pull up an arbitrary user's individual score outside
  of that user's own `/results` or a leaderboard they chose to appear on;
  that stays intentionally out of scope until there's a concrete
  moderation/support need for it, rather than building a "view anyone's
  attempt" admin backdoor speculatively.
- **A caching caveat worth knowing if you're testing this by hand**: the
  analytics endpoints cache by their input parameters (`testId`, `limit`,
  `days`). Calling the same endpoint with the same parameters twice
  within the TTL window returns the cached result even if the underlying
  data changed in between (e.g., a new attempt was submitted) — expected
  and fine for a dashboard, but worth knowing if a number looks "stale"
  during manual testing. It'll refresh within the TTL window, or restart
  Redis/flush the cache to force it immediately.
- **`total_marks` still isn't cross-validated against attached questions**
  (noted back in Phase 5) — that remains true here too; nothing in this
  phase changes it.

### Trying it out

```bash
# A user's own attempt history
curl localhost:8080/api/v1/users/me/attempts -H "Authorization: Bearer <userToken>"

# Per-test analytics (admin)
curl localhost:8080/api/v1/admin/analytics/tests/<testId> -H "Authorization: Bearer <adminToken>"

# Popular tests (admin)
curl "localhost:8080/api/v1/admin/analytics/popular-tests?limit=5" -H "Authorization: Bearer <adminToken>"

# Public leaderboard - no auth needed
curl localhost:8080/api/v1/tests/<testId>/ranking
```

### Tests

- `ResultIntegrationTest` — a user's cross-test history and per-test
  history both correctly scoped to the caller (another user's attempts
  never leak in); unauthenticated access rejected.
- `AnalyticsIntegrationTest` — per-test analytics numbers verified exactly
  against a known fixture (3 attempts: 2 completed, 1 in-progress →
  `completionRate = 66.67`); every analytics endpoint confirmed
  admin-only (403 for a plain user, 401 unauthenticated); popular-tests/
  popular-exams/registration-trend reachability. (Exact-value assertions
  are deliberately limited to the single-test endpoint, which caches by a
  fixture-unique `testId` — the `limit`/`days`-keyed endpoints share a
  cache key space with every other test method and test class in the
  suite via the shared Testcontainers Redis instance, so asserting a
  precise list there would be flaky by construction, not a real bug.)
- `RankingIntegrationTest` — score-descending order, exactly one entry
  per user even after a retake (verified by having the top scorer
  deliberately score lower on a second attempt and confirming the
  leaderboard still reflects their *first, better* result), no
  email/mobile substring anywhere in the response body, public
  reachability without a token, 404 for a nonexistent/unpublished test.
- `AttemptIntegrationTest` gained `sweepJob_autoSubmitsAbandonedAttempts_thatNobodyEverRevisits`
  — backdates `expiresAt` and calls `autoSubmitAllExpired()` directly
  (rather than waiting on the real `@Scheduled` trigger, which would make
  the test slow and timing-dependent) to verify the sweep finalizes an
  attempt nobody has touched since, and that running it again afterward
  is a correct no-op.

Runs the same way as before - `mvn test`.

## Phase 8 — Bookmarks, Notes & Study Material

### What's new

- **New tables (`V20`–`V22`)**: `bookmarks` (user, question, unique per
  pair, no soft delete - pure add/remove), `user_notes` (user, question,
  note text, unique per pair - one note per user per question), and
  `study_materials` (title, type, optional exam/subject scoping, a
  `storage_key` rather than the file itself, `BaseEntity`-shaped since
  admins manage this content like the rest of the catalog).
- **`bookmark` module**: `GET/POST /api/v1/bookmarks`,
  `DELETE /api/v1/bookmarks/{questionId}`. `BookmarkResponse` is
  deliberately answer-free — the same rule `QuestionAttemptResponse`
  follows since Phase 6 — because a user can bookmark a question at any
  point, including mid-attempt, well before they'd legitimately see its
  answer key.
- **`note` module**: `GET/POST/PUT/DELETE /api/v1/questions/{id}/notes`.
  One note per user per question; `POST`/`PUT` both upsert to the same
  underlying row (verified by asserting the row `id` is unchanged across
  an update, not just that the text changed).
- **`study` module + `common.storage.FileStorageService`**: the file
  storage abstraction the spec calls for, so large files never land in
  Postgres. `LocalFileStorageService` is the one implementation shipped
  — disk-backed, with path-traversal protection on every storage-key
  resolution (`resolve().normalize()` then verifying the result still
  starts with the configured base directory, rejecting anything like
  `../../etc/passwd`). `POST /api/v1/admin/study-materials` (multipart:
  title, description, type, optional examId/subjectId, file),
  `GET /api/v1/study-materials` (public list/filter),
  `GET /api/v1/study-materials/{id}` (public metadata),
  `GET /api/v1/study-materials/{id}/file` (public download, streamed via
  `FileStorageService.load()`), `DELETE /api/v1/admin/study-materials/{id}`
  (soft-deletes the metadata row **and** calls `FileStorageService.delete()`
  on the underlying file — verified by a test that re-fetches after
  deletion and gets a 404).

### An honest note about how this phase actually went

Partway through building this phase, the sandbox environment I'd been
working in was reset — the entire scratch workspace (`/home/claude`) was
wiped, taking every file from Phases 1–8 with it. This is a real
operational risk of the environment, not something either of us did
wrong. What survived was the **Phase 7 zip already delivered to you** in
the outputs directory (206 files, version `0.7.0-phase7`) — so I restored
the project from that rather than reconstructing eight phases from
memory, verified the restore against the exact file count and version
I'd last confirmed, and then rebuilt the bookmark/note/study work that
had been lost. The `study` module specifically had to be redone from
scratch, since only its `MaterialType` enum had been created before the
reset. I'm flagging this so the practice is visible rather than
invisibly patched over: **if you ever notice a delivered zip's contents
don't match what a session claims to have built, that mismatch is worth
raising immediately** — this time the recovery path worked cleanly
because a verified deliverable existed to restore from, but that won't
always be true, and asking is the reliable check.

### Design decisions worth knowing about

- **Bookmarks and notes both use "hide existence via 404" for
  ownership**, same pattern as attempts since Phase 6 — deleting a note
  or bookmark that isn't yours (or doesn't exist) returns 404, not 403,
  so a guessed id doesn't confirm anything exists.
- **`LocalFileStorageService` is explicitly a single-instance
  implementation** — files live on whatever disk the app happens to be
  running on. That's fine for local development and a single-server
  deployment, but wrong for a horizontally-scaled production deployment
  (each instance would only see its own uploads). The `FileStorageService`
  interface is what makes swapping in an S3-compatible implementation
  later a matter of adding a second `@Service` bean, not a rewrite of any
  caller — but that S3 implementation itself hasn't been built, since
  doing so honestly would mean adding a real AWS/S3 SDK dependency and
  something testable against it, not a decorative interface implementation
  that looks done but isn't. The `STORAGE_ENDPOINT`/`STORAGE_ACCESS_KEY`/
  `STORAGE_SECRET_KEY`/`STORAGE_BUCKET` env vars stay reserved for it.
- **Study material's public catalog only ever serves metadata + the file
  itself** — no admin-only fields (like who uploaded it) leak into the
  public response shape.

### Trying it out

```bash
# Bookmark a question
curl -X POST localhost:8080/api/v1/bookmarks -H "Authorization: Bearer <userToken>" \
  -H 'Content-Type: application/json' -d '{"questionId": "<questionId>"}'

# Save a note on a question
curl -X POST localhost:8080/api/v1/questions/<questionId>/notes -H "Authorization: Bearer <userToken>" \
  -H 'Content-Type: application/json' -d '{"noteText": "Remember: percentage tricks apply here"}'

# Admin uploads a study material file
curl -X POST localhost:8080/api/v1/admin/study-materials -H "Authorization: Bearer <adminToken>" \
  -F "title=Percentage Formula Sheet" -F "type=NOTES" -F "file=@formulas.pdf"

# Anyone downloads it
curl localhost:8080/api/v1/study-materials/<materialId>/file -o formulas.pdf
```

### Tests

- `BookmarkIntegrationTest` — add/list/remove lifecycle, duplicate-add
  rejected, list correctly scoped per user (another user's bookmark list
  stays empty), removing someone else's or a nonexistent bookmark returns
  404, unauthenticated access rejected.
- `NoteIntegrationTest` — full lifecycle including the upsert check (the
  row `id` from create matches the row `id` returned by update — proving
  it's the same row, not a second one); a second user querying the same
  question sees 404, not the first user's note; unauthenticated access
  rejected.
- `StudyMaterialIntegrationTest` — multipart upload → public list/get/
  download, with the downloaded bytes asserted to exactly match what was
  uploaded; non-admin upload rejected with 403; delete removes both the
  metadata (re-fetch returns 404) and the underlying file.

Runs the same way as before - `mvn test`.

## Phase 9 — Test Series & Subscriptions

### What's new

- **New tables (`V23`–`V26`)**: `plans` (name, type, price, duration,
  active flag), `subscriptions` (user, plan, status, start/end date - a
  **partial unique index** on `(user_id) WHERE status = 'ACTIVE'`, same
  DB-level backstop pattern as the one-in-progress-attempt constraint from
  Phase 6), `test_series` (`BaseEntity`-shaped, reusing `TestStatus` from
  the `test` module rather than a duplicate enum), `test_series_tests`
  (join, not a `BaseEntity` - same "pure association row" pattern as
  `TestQuestion` and `AttemptAnswer`).
- **`plan` module**: `GET /api/v1/plans` (public, active plans only),
  full admin CRUD under `/api/v1/admin/plans`. A plan's price is required
  unless its type is `FREE`.
- **`subscription` module**:
  - `POST /api/v1/subscriptions` (self-service) — subscribing to a
    `FREE` plan activates immediately; subscribing to any paid plan
    creates the subscription in `PENDING`, deliberately **not**
    auto-activated (see the honesty note below).
  - `GET /api/v1/subscriptions/me` (history), `GET /api/v1/subscriptions/me/active`
    (current active one, 404 if none).
  - `POST /api/v1/subscriptions/{id}/cancel` — only the subscription's
    own owner can cancel it, and only while it's `ACTIVE`.
  - `POST /api/v1/admin/subscriptions/grant` (admin-only) — activates a
    subscription for a given user immediately, regardless of plan type,
    bypassing payment entirely. This is the support/promo escape hatch,
    and also what makes paid content usable at all in this phase, since
    there's no real payment flow yet.
  - Expiry is checked **at read time** (`Subscription.isCurrentlyActive()`
    compares `endDate` against `Instant.now()` on every access) rather
    than requiring a background job to flip the stored status - same
    "lazy check, no drift" philosophy as the attempt engine's expiry
    check, just simpler here since there's no side-effecting state
    transition that needs to happen for the gate itself to be correct.
- **`testseries` module**: `GET /api/v1/test-series` /
  `GET /api/v1/test-series/{id}` (public, same `PUBLISHED`-series-under-
  `PUBLISHED`-exam gate used everywhere else), `GET /api/v1/test-series/{id}/tests`
  (the bundled tests' metadata - **not** an access grant; see below).
  Admin CRUD plus `POST`/`DELETE /api/v1/admin/test-series/{id}/tests`
  to attach/detach tests, blocked from deleting a series while tests are
  still attached (same delete-integrity chain pattern from every prior
  phase).
- **The access gate, wired into the one place that actually matters**:
  `AttemptServiceImpl.startAttempt()` now checks
  `subscriptionRepository.hasActiveSubscription(userId)` before allowing
  an attempt to start on any non-free test, rejecting with a clear 403 if
  the user has none. Free tests are completely unaffected - the check is
  skipped entirely for them.

### An honest scope boundary, stated plainly

**Test series do not currently grant access to anything.** A series
bundles tests for browsing/marketing purposes (`GET .../tests` shows
what's in it), but purchasing or subscribing doesn't unlock the
individual tests inside a paid series - each test's own `free`/`price`
and the user's own subscription status is what `AttemptServiceImpl`
actually checks. Building real bundle-purchase-unlocks-N-tests logic
would mean inventing a purchase-tracking table ahead of the Payment
module that's supposed to own that concept, which would very likely need
to be reshaped once Payments actually exists. Rather than build that
speculatively and probably wrong, `testseries` in this phase is
deliberately just a content-organization feature. The same honesty
applies to subscriptions: **no payment processing exists**, so a paid
plan cannot be self-activated by a user - it either stays `PENDING`
forever in this phase, or an admin grants it directly. Faking automatic
activation without a real payment behind it would be worse than being
upfront that it isn't built yet.

### Design decisions worth knowing about

- **`plan` and `subscription` reference each other's repositories** —
  `PlanServiceImpl` checks `SubscriptionRepository.existsByPlanId()`
  before allowing a plan delete, and `SubscriptionServiceImpl` obviously
  needs `PlanRepository`. This is a real, acknowledged tight coupling
  between the two modules, on par with how `role` and `user` already
  relate — arguably these are one bounded concept (subscription
  management) split into two packages mainly for the same
  one-file-per-concern clarity used everywhere else in this codebase,
  not a sign of an accidental circular dependency that needs untangling.
- **Admin-granting a subscription supersedes, not blocks, an existing
  active one** — if a user already has an active subscription and an
  admin grants them a different plan, the old one is transitioned to
  `CANCELLED` (with `endDate` set) rather than the grant call failing.
  An admin override is meant to be authoritative.
- **Cancelling sets `endDate` to now** rather than leaving it null, so
  the subscription's own history record accurately reflects when access
  actually ended, not just that it eventually got cancelled at some
  unspecified point.

### Trying it out

```bash
# Admin creates a plan
curl -X POST localhost:8080/api/v1/admin/plans -H "Authorization: Bearer <adminToken>" \
  -H 'Content-Type: application/json' -d '{"name":"Premium Yearly","planType":"YEARLY","price":1999,"durationDays":365}'

# User self-subscribes (works immediately only if the plan is FREE)
curl -X POST localhost:8080/api/v1/subscriptions -H "Authorization: Bearer <userToken>" \
  -H 'Content-Type: application/json' -d '{"planId": "<planId>"}'

# Admin grants a paid plan directly (support/promo, bypasses payment)
curl -X POST localhost:8080/api/v1/admin/subscriptions/grant -H "Authorization: Bearer <adminToken>" \
  -H 'Content-Type: application/json' -d '{"userId": "<userId>", "planId": "<planId>"}'

# Now the user can start an attempt on a paid test
curl -X POST localhost:8080/api/v1/tests/<paidTestId>/attempts -H "Authorization: Bearer <userToken>"
```

### Tests

- `SubscriptionIntegrationTest` — the core new behavior: subscribing to a
  `FREE` plan activates immediately; subscribing to a paid plan lands
  `PENDING` (and `GET .../active` correctly 404s, since nothing is
  active yet); a second active subscription is blocked until the first
  is cancelled, and cancelling correctly re-opens the door; **the actual
  access gate** — a paid test blocks `startAttempt()` with 403 for an
  unsubscribed user, and an admin grant immediately unblocks it; a free
  test never requires a subscription at all; the admin grant endpoint is
  itself RBAC-checked.
- `TestSeriesIntegrationTest` — draft series hidden publicly, attach/
  duplicate-attach-rejected/detach, delete blocked while a test is
  attached then succeeds after detaching, publish makes the series and
  its test list publicly visible, non-admin write access blocked.
- `PlanIntegrationTest` — admin-created plan appears in the public active
  list, a paid plan without a price is rejected while a free plan isn't,
  admin plan endpoints require authentication and the `ADMIN` role.

Runs the same way as before - `mvn test`.

## Phase 10 plan — Payments & Coupons

Phase 10 will add, without restructuring anything above:

1. **Migrations**: `payments`, `coupons`.
2. **`payment` module**: `Payment` entity (states per the spec:
   `CREATED`/`PENDING`/`SUCCESS`/`FAILED`/`REFUNDED`/`CANCELLED`),
   integration prepared for Razorpay/Stripe (the `RAZORPAY_KEY_ID`/
   `RAZORPAY_KEY_SECRET` env vars have sat unused in `.env.example`
   since Phase 1), webhook endpoint architecture for payment-provider
   callbacks, and **this is what finally lets `SubscriptionServiceImpl.subscribe()`
   auto-activate a paid plan for real** instead of landing in `PENDING`
   — closing the honest gap this phase left open. Payment status must be
   verified server-side against the provider, never trusted from a
   client-supplied "it succeeded" flag, same never-trust-the-client
   principle the attempt engine's scoring already follows.
3. **`coupon` module**: code, discount percentage, max discount, min
   order amount, expiry, usage limit/count, active flag - validated
   server-side against a payment amount before it's applied, never
   client-computed.
4. **Purchase-tracking, built for real this time**: once payments exist,
   individual test/test-series purchases (not just plan subscriptions)
   become representable, which is exactly the piece Phase 9 explicitly
   deferred rather than guessing at prematurely.
5. Unit + integration tests including a webhook-simulation test (a
   provider callback correctly transitions a `Payment` and activates the
   associated `Subscription`) and coupon validation edge cases (expired,
   usage-limit-exceeded, below-minimum-amount).

Phase 10 will not start until you confirm Phase 9 compiles, migrates,
and passes its tests in your environment.

