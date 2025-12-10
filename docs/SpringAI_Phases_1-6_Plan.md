# Spring AI Agent Integration Plan (Phases 1–6)

Scope: Integrate a Leader MCP Agent with Spring AI, Microsoft Outlook/Teams tools, NLP role-based orchestration, React UI hooks, and observability. This plan is tailored to the MyTeam-API-Agent monorepo (Spring Boot backend + React frontend) and covers Phases 1–6 only.

Repo context
- Backend: Gradle Spring Boot under `src/main/java` (packages will be created).
- Frontend: React under `myteam-ui/src`.
- Docs: `docs/` contains endpoint and auth references.
- Artifacts: `artifacts/` used for exports/evals.

Contents
- Phase 1: Foundations (MCP Leader, agent core, RBAC, contracts)
- Phase 2: Spring AI (provider config, prompts, function calling, memory)
- Phase 3: Microsoft Outlook/Teams (OAuth + tools)
- Phase 4: NLP Router & Workflows
- Phase 5: React UI hooks
- Phase 6: Observability
- Sprint sequencing (2 weeks)
- Dependencies
- Risks (Phases 1–6 only)
- Interfaces (future plug-in points for Phases 7–11)

---

## Phase 1: Foundations — MCP Leader, Agent Core, RBAC, Contracts

Goal: Establish a consistent agent entrypoint and contracts, a role-based policy mapper, and a minimal MCP Leader tool registry to coordinate workflows.

Deliverables
- AgentController: REST endpoint `/api/agent/execute`.
- AgentService (+impl): request → RBAC → MCP tools → response.
- DTOs: `AgentRequest`, `AgentResponse`, `ToolInvocation`.
- RBAC policy mapping aligned to `docs/endpoint_role_matrix.md` and `docs/endpoints.md`.
- MCP Leader interfaces and a mock implementation.
- Docs references to agent endpoints and new audit types.

Files to add/modify
- Backend:
  - `src/main/java/com/myteam/agent/core/AgentController.java`
  - `src/main/java/com/myteam/agent/core/AgentService.java`
  - `src/main/java/com/myteam/agent/core/AgentServiceImpl.java`
  - `src/main/java/com/myteam/agent/core/dto/AgentRequest.java`
  - `src/main/java/com/myteam/agent/core/dto/AgentResponse.java`
  - `src/main/java/com/myteam/agent/core/dto/ToolInvocation.java`
  - `src/main/java/com/myteam/agent/core/policy/RbacPolicyMapper.java`
  - `src/main/java/com/myteam/agent/core/policy/RbacRoles.java`
  - `src/main/java/com/myteam/agent/mcp/McpLeader.java`
  - `src/main/java/com/myteam/agent/mcp/McpLeaderImpl.java`
- Docs:
  - Update `docs/endpoints.md` to include `/api/agent/execute`.
  - Update `docs/authentication_and_audit.md` with agent event types.

Contract sketch
- AgentRequest: actor(userId/email), role, text, intent?, payload(map), sessionId, preferredChannel, correlationId.
- AgentResponse: status(ok/error), message, steps[], toolCalls[], auditId, data(any), errors[].
- ToolInvocation: toolName, input(map), output(map), durationMs, success.

Definition of Done
- RBAC-enforced agent path returns structured responses.
- MCP Leader registers at least two mock tools and returns deterministic outputs.
- Audit events recorded for execute attempts.

Env vars
- `APP_ENV` (dev/stage/prod)
- `SESSION_SECRET`

Acceptance Criteria & Metrics
- 100% agent entrypoints use DTOs + RBAC.
- Mocked flows complete in <50ms local p50.

---

## Phase 2: Spring AI — Provider, Prompts, Function Calling, Memory

Goal: Configure Spring AI with provider selection, prompt templates, function calling mapped to tools, and basic conversation memory.

Deliverables
- Spring AI dependencies (BOM) and provider configuration.
- AiConfig: provider selection via env, client beans, health checks.
- PromptService: templated prompts (system, routing_intent, recognition_classify).
- FunctionCallingService: LLM tool call → `McpLeader` tool invocation.
- Memory: conversation store (session-scoped) + embedding index (in-memory).

Files to add/modify
- Backend:
  - `build.gradle` (add Spring AI deps)
  - `src/main/resources/application.yml` (spring.ai provider/model settings)
  - `src/main/java/com/myteam/agent/ai/AiConfig.java`
  - `src/main/java/com/myteam/agent/ai/PromptService.java`
  - `src/main/java/com/myteam/agent/ai/FunctionCallingService.java`
  - `src/main/java/com/myteam/agent/memory/ConversationMemoryStore.java`
  - `src/main/java/com/myteam/agent/memory/EmbeddingIndex.java`
  - `src/main/java/com/myteam/agent/memory/InMemoryEmbeddingIndex.java`
  - Integrate `AgentServiceImpl` with PromptService + FunctionCallingService.

Prompt template examples (high-level)
- System: role/permissions, endpoint matrix summary, safety, redaction policies.
- Routing_intent: return intent + entities in strict JSON schema.
- Recognition_classify: suggest type/category/level/points based on text.

Definition of Done
- `/api/agent/execute` performs an LLM call with templated prompts and function calling to a registered tool.
- Memory persists N turns per session; embedding index stub reachable.
- Provider switch via env verified.

Env vars
- `SPRING_AI_PROVIDER` (openai|azure|anthropic|groq)
- `SPRING_AI_MODEL`
- `SPRING_AI_BASE_URL`
- `OPENAI_API_KEY` or `AZURE_OPENAI_API_KEY` or `ANTHROPIC_API_KEY` or `GROQ_API_KEY`
- `AI_CACHE_ENABLED=true|false`

Acceptance Criteria & Metrics
- p50 LLM call <1.5s locally with caching.
- Function call precision ≥90% on curated prompts.

---

## Phase 3: Microsoft Outlook & Teams — OAuth + Tools

Goal: Implement Microsoft Graph OAuth and adapters for Outlook email/calendar and Teams messages; expose tools to the MCP Leader.

Deliverables
- OAuth 2.0 auth code flow, token exchange & refresh; per-user token storage (dev in-memory or file-backed).
- Graph adapters: OutlookService (send/read email, create/list events), TeamsService (post messages, list recent activity).
- MCP tools: `sendEmail`, `listInbox`, `createCalendarEvent`, `postTeamsMessage`.

Files to add/modify
- Backend:
  - `src/main/java/com/myteam/integrations/ms/MicrosoftOAuthController.java`
  - `src/main/java/com/myteam/integrations/ms/MicrosoftOAuthService.java`
  - `src/main/java/com/myteam/integrations/ms/GraphClient.java`
  - `src/main/java/com/myteam/integrations/ms/OutlookService.java`
  - `src/main/java/com/myteam/integrations/ms/TeamsService.java`
  - Register tools in `McpLeaderImpl`.
  - `src/main/resources/application.yml` entries for MS tenant/app.
- Docs:
  - Update `docs/endpoints.md` with `/api/integrations/ms/*` (connect, callback, status, disconnect).
  - Update `docs/authentication_and_audit.md` with OAUTH_CONNECT/REFRESH/REVOKE events.

Definition of Done
- OAuth connect works locally; tokens persisted; refresh reliable.
- `sendEmail`, `createCalendarEvent`, `postTeamsMessage` succeed against Graph in dev.

Env vars
- `MS_TENANT_ID`
- `MS_CLIENT_ID`
- `MS_CLIENT_SECRET`
- `MS_REDIRECT_URI` (e.g., `http://localhost:8080/api/integrations/ms/callback`)
- `MS_SCOPES` (minimum: `Mail.Read Mail.Send Calendars.ReadWrite Chat.ReadWrite`)

Acceptance Criteria & Metrics
- OAuth success ≥95%; tool latency p50 <1s (excluding network); refresh reliability ≥99%.

---

## Phase 4: NLP Router & Workflow Orchestration

Goal: Classify intents and orchestrate multi-step workflows conditioned by role and context, with guardrails and retries.

Deliverables
- IntentRouter: uses PromptService (LLM) + fallback rules.
- RouterPolicy: role-aware intent permissions and tool preference mapping.
- WorkflowEngine: step graph execution; compensations for side-effects.
- Workflows: RecognitionAssistWorkflow, NotificationTriageWorkflow (minimum).
- Config: `agent-policies.yml` for role→intent→tool mapping.

Files to add/modify
- Backend:
  - `src/main/java/com/myteam/agent/router/IntentRouter.java`
  - `src/main/java/com/myteam/agent/router/RouterPolicy.java`
  - `src/main/resources/agent-policies.yml`
  - `src/main/java/com/myteam/agent/workflow/WorkflowEngine.java`
  - `src/main/java/com/myteam/agent/workflow/WorkflowStep.java`
  - `src/main/java/com/myteam/agent/workflow/workflows/RecognitionAssistWorkflow.java`
  - `src/main/java/com/myteam/agent/workflow/workflows/NotificationTriageWorkflow.java`
  - Integrate in `AgentServiceImpl`.

Definition of Done
- At least two workflows complete end-to-end with RBAC and tool calls.
- Denials produce structured RBAC errors; policies configurable at runtime (dev).

Env vars
- `ROUTER_THRESHOLD` (e.g., 0.6)
- `WORKFLOW_MAX_RETRIES=2`

Acceptance Criteria & Metrics
- Routing accuracy ≥90% (curated set), workflow completion ≥95% with retries.

---

## Phase 5: React UI Hooks — Agent Console, Recognition Assistant, Notifications

Goal: Provide UI surfaces to exercise the agent: console for chat/execute, recognition assistance, notifications and MS connection status.

Deliverables
- AgentConsole page: send execute requests, stream responses (optional), inspect structured results.
- RecognitionAssistant page: NLP aid for message/fields, quick actions.
- Notifications page: Outlook/Teams activity and connect/disconnect status.
- Services: `agentApi.ts` and `msApi.ts` clients.

Files to add/modify
- Frontend:
  - `myteam-ui/src/pages/AgentConsole.tsx`
  - `myteam-ui/src/pages/RecognitionAssistant.tsx`
  - `myteam-ui/src/pages/Notifications.tsx`
  - `myteam-ui/src/services/agentApi.ts`
  - `myteam-ui/src/services/msApi.ts`
  - Update `myteam-ui/src/App.tsx` routing.

Definition of Done
- All three pages routable; basic interactions succeed against dev backend.
- Clear errors and user feedback; role context visible.

Env vars
- `VITE_API_BASE_URL` (if needed for proxy/base URL)

Acceptance Criteria & Metrics
- TTI <2s; interaction p50 <500ms (excluding network); no UI console errors.

---

## Phase 6: Observability — Structured Logs, Audit, Prompt Capture, Evaluation

Goal: Add JSON structured logging with correlation IDs, full audit coverage, prompt/response capture with redaction, and evaluation hooks.

Deliverables
- logback-spring.xml with JSON encoder and MDC correlation IDs.
- LoggingConfig (MDC) wiring for request lifecycle.
- AuditService + AuditController; AuditEvent schema.
- PromptCaptureService with redaction rules and storage policy.
- EvalRunner with tagged sample sets and exports to `artifacts/`.

Files to add/modify
- Backend:
  - `src/main/resources/logback-spring.xml`
  - `src/main/java/com/myteam/observability/LoggingConfig.java`
  - `src/main/java/com/myteam/audit/AuditService.java`
  - `src/main/java/com/myteam/audit/AuditEvent.java`
  - `src/main/java/com/myteam/audit/AuditController.java`
  - `src/main/java/com/myteam/observability/PromptCaptureService.java`
  - `src/main/java/com/myteam/eval/EvalRunner.java`
- Docs:
  - Update `docs/authentication_and_audit.md` with agent audit types and retention.
  - Update `docs/curl_examples.sh` to include audit/eval retrieval.

Definition of Done
- Structured logs present with correlationId, actor, intent, tools, latency, provider/model.
- Audit endpoints return records; prompt capture toggled by env; redaction enabled.
- Eval runner produces summary metrics and writes to `artifacts/json` or `artifacts/csv`.

Env vars
- `LOG_FORMAT=json`
- `PROMPT_CAPTURE_ENABLED=true|false`
- `AUDIT_RETENTION_DAYS=30`
- `EVAL_MODE=mock|live`

Acceptance Criteria & Metrics
- ≥95% agent flows emit required logs; 100% audit coverage on agent endpoints.
- Eval metrics include accuracy, latency, error rate.

---

## Sprint Sequencing (2 weeks, 10 working days)

Week 1
- Day 1–2: Phase 1 Foundations (agent core, RBAC, contracts, MCP Leader)
- Day 3: Phase 2 deps (build.gradle, application.yml, AiConfig)
- Day 4: Phase 2 prompts + function calling + memory
- Day 5: Phase 3 OAuth scaffolding (controller/service, envs, docs)

Week 2
- Day 6: Phase 3 adapters + MCP tools; end-to-end OAuth connect
- Day 7: Phase 4 router + workflows (recognition, notifications)
- Day 8: Phase 5 pages + agentApi
- Day 9: Phase 5 notifications view + msApi + connect CTA
- Day 10: Phase 6 logging, audit, prompt capture, eval runner

---

## Dependencies
- Phase 2 depends on Phase 1 contracts and routing.
- Phase 3 depends on Phases 1–2 (tools registry via MCP; provider + prompts for intent).
- Phase 4 depends on Phases 1–3 (router uses AI; workflows use tools).
- Phase 5 depends on Phases 1–4 backend endpoints.
- Phase 6 can begin after Phase 1 but delivers most value after Phases 2–4.

---

## Risks (Phases 1–6 only)
- Provider instability & rate limits: add retry/backoff; cache; circuit breaker; provider switch via env.
- OAuth complexity & tenant policies: validate app registration early; least-privilege scopes; user-delegated tokens; clear error UX.
- RBAC misconfigurations: deny-by-default; RouterPolicy tests; audit denials.
- Prompt capture & PII: redaction rules; opt-out via env; storage retention limits.
- Workflow compensations: idempotent steps; bounded retries; compensation logs.
- UI/backend drift: stable DTOs; versioned endpoints; shared types where possible.
- Memory persistence: start in-memory; abstract interface for PGVector/Pinecone later.

---

## Interfaces for Future Plug-ins (Phases 7–11)
- Security hardening: TokenStorage interface for KMS-backed secrets; rate limit filters; sandbox tool execution.
- DevOps: log shipping to ELK/OTel; CI hooks for eval; Docker profiles for vector store.
- Testing: E2E harness using EvalRunner; mocks for GraphClient; NLP intent fixtures.

---

## Quickstart (Dev)

Backend (after scaffolding)
- Configure env vars:
  - SPRING_AI_PROVIDER, SPRING_AI_MODEL, OPENAI_API_KEY (or GROQ_API_KEY/AZURE_OPENAI_API_KEY)
  - MS_TENANT_ID, MS_CLIENT_ID, MS_CLIENT_SECRET, MS_REDIRECT_URI, MS_SCOPES
  - APP_ENV=dev, LOG_FORMAT=json, PROMPT_CAPTURE_ENABLED=true
- Run Spring Boot dev server.

Frontend
- Set `VITE_API_BASE_URL` if needed; then run dev server.

Artifacts
- Eval exports appear under `artifacts/json` or `artifacts/csv`; graphs under `artifacts/graphs`.

Notes
- Keep role names consistent with `employee`, `teamlead`, `manager`, `admin`.
- Recognition flows tie into `RecognitionManagement.tsx` and backend `/recognitions` endpoints.

