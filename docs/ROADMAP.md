# Forge — Roadmap & Future Requirements

Where Forge is going, what each future capability requires, how the work is
sequenced, and the decisions still to be made. This is a living document — update
it as milestones close and decisions are settled.

> For what exists today, see `docs/ARCHITECTURE.md`. Requirements below are
> written so each could become a GitHub issue with a behaviour spec.

---

## 1. Vision

Forge aims to be an **AI-native application framework**: the same way Spring made
enterprise Java productive, Forge should make LLM-native applications productive —
agents, tools, prompts, and model access managed by a container as naturally as
ordinary beans.

The long-term pipeline (each layer builds on the previous):

```
CLI ─▶ Scanner ─▶ Bean Registry ─▶ Dependency Injection ─▶ Application Context
    ─▶ Agent Registry ─▶ Tool Registry ─▶ Prompt Engine ─▶ LLM Provider Layer
    ─▶ AI Runtime ─▶ Developer Applications
```

Everything through **Application Context** exists. The AI layers are partially
built (tools, tool schemas, agent stereotype, LLM seam) with the *runtime* — the
part that makes it an agent framework — still ahead.

**Guiding constraint:** depth over breadth. Every capability ships tested,
documented, and fail-fast, or it doesn't ship.

---

## 2. Milestones

### ✅ 0.1 — DI Container (complete)
Scanner, bean definitions, registry, constructor injection with cycle detection,
application context, lifecycle. A complete IoC container.

### ◐ 0.2 — Tool & Agent Foundations (built; merging)
`@Tool` + `ToolRegistry`, tool schemas for LLM tool-use, `@Agent` stereotype +
`AgentRegistry` + meta-annotation support, a provider-neutral `LlmClient` +
in-memory `EchoLlmClient`. Plus CI and the documentation set.

### ○ 0.3 — Agent Runtime (next major)
The piece that makes Forge an *agent* framework: an execution loop that binds an
agent's `LlmClient`, tools, and (optionally) memory into a working
reason-act cycle. **This is the defining, opinionated milestone** — design it
against a real application.

### ○ 0.4 — Real LLM adapters
A concrete provider module (Anthropic Claude first) implementing `LlmClient`,
plus the tool-use wire protocol.

### ○ 0.5 — Prompt Engine
First-class, testable prompt management (templates, composition, versioning).

### ○ 0.6 — Forge CLI
Project scaffolding and a dev runner (`forge new`, `forge run`).

### ○ Later
Multi-agent orchestration, durable/replayable runs, observability, configuration
& environment, web integration.

---

## 3. Functional requirements

Grouped by capability. Each `FR` is a candidate issue.

### Agent runtime (0.3)
- **FR-1 Agent execution loop.** Given an `@Agent` bean, run: send prompt → if the
  model requests a tool, invoke it via `ToolRegistry` and feed the result back →
  repeat until a final answer. Must be testable offline against `EchoLlmClient`
  (deterministic canned tool-call replies).
- **FR-2 Agent runtime object.** An `AgentRuntime` (or similar) per `@Agent` bean
  binding its `LlmClient`, its tool set, and its memory; obtainable from the
  context.
- **FR-3 Agent memory / conversation state.** Pluggable conversation history with
  a defined lifecycle relative to the bean (per-call? per-conversation?
  container-scoped?). *Requires a scope decision — see Open Decisions.*
- **FR-4 Tool result feedback.** Serialize a tool's return value back into the
  model conversation in the provider's expected shape.

### LLM integration (0.4)
- **FR-5 Anthropic adapter.** `forge-llm-anthropic` module implementing
  `LlmClient` over the Messages API, mapping `LlmRequest`/`LlmResponse`, wrapping
  transport/HTTP errors in `LlmException`. Kept out of `forge-core`.
- **FR-6 Tool-use protocol.** Feed `ToolSchema`s as provider tool definitions;
  parse tool-call requests from responses; support the multi-turn tool loop
  (works with FR-1).
- **FR-7 Streaming (optional).** Token streaming behind an extended `LlmClient`
  method, added only when a consumer needs it.
- **FR-8 Multi-turn requests.** Extend `LlmRequest` to carry message history
  (currently single-turn) when the agent loop requires it.

### Tools (extends 0.2)
- **FR-9 Argument binding.** Coerce a model's JSON tool arguments into the Java
  parameter types and invoke — the inverse of the schema (which is output-only
  today).
- **FR-10 Richer schemas.** Optional parameters, enums, nested objects,
  collections in `ToolSchema` (currently all-required scalars + `object`
  fallback).

### Container (extends 0.1)
- **FR-11 Bean scopes.** Prototype (and later request/conversation) scope via a
  `scope` on `BeanDefinition` and a resolver branch.
- **FR-12 Qualifiers.** `@Qualifier`/`@Primary` (or name-based) to resolve
  by-type ambiguity at injection points.
- **FR-13 JAR scanning.** Handle the `jar:` protocol as a second scanning
  strategy (the current protocol guard is the seam).
- **FR-14 Configuration/environment.** External configuration (properties/env)
  injectable into beans.

### Prompt engine (0.5)
- **FR-15 Prompt templates.** Named, parameterized, composable prompt templates
  with variable substitution, testable in isolation.
- **FR-16 `@Prompt` support.** Declare prompts on beans/agents, discovered like
  tools.

### Tooling (0.6+)
- **FR-17 CLI.** `forge new` (scaffold) and `forge run` (dev runner).
- **FR-18 Maven wrapper** so contributors build with no local Maven.
- **FR-19 Repository layout** so `pom.xml`, `.git`, and IDE config share one root.

### Observability (later)
- **FR-20 Run tracing.** Structured, inspectable records of an agent run (prompts,
  tool calls, results, timings).
- **FR-21 Durable/replayable runs.** Persist a run so it can be resumed after a
  crash or replayed against a different model.

---

## 4. Non-functional requirements

- **NFR-1 Zero runtime dependencies in `forge-core`.** Provider/integration code
  lives in separate modules.
- **NFR-2 Thread safety.** All shared components safe under concurrent use;
  immutable by default.
- **NFR-3 Fail fast.** Misconfiguration surfaces at startup with a message naming
  the cause.
- **NFR-4 Startup performance.** Scanning and wiring stay linear in the number of
  beans; consider bytecode scanning (ASM) instead of class loading if startup
  becomes a concern at scale.
- **NFR-5 Native-image friendliness (aspirational).** Reflection use should be
  describable to GraalVM; a build-time processing path is a possible future
  direction (cf. Quarkus/Micronaut).
- **NFR-6 Backward compatibility / SemVer.** Once public, API changes follow
  semantic versioning; prefer additive changes; deprecate before removing.
- **NFR-7 Test coverage of failure modes.** Every feature tests the happy path
  *and* each way it can fail (nulls, cycles, ambiguity, wrapped causes,
  concurrency).
- **NFR-8 CI on the target JDK.** Every push/PR builds and tests on JDK 21.
- **NFR-9 Documentation.** Each capability ships with docs; the guide/masterclass
  stay current.
- **NFR-10 Security.** No arbitrary code execution during scanning
  (`initialize=false` upheld); provider credentials handled only in adapter
  modules, never in core.

---

## 5. Sequencing & dependencies

Build order respects the dependency graph:

```
0.2 (merge) ─▶ FR-9 argument binding ─┐
                                       ├─▶ FR-1 agent loop ─▶ FR-2 runtime ─▶ FR-3 memory
0.2 LlmClient ─▶ FR-8 multi-turn ─────┘         │
                                                 └─▶ FR-5/6 Anthropic adapter + tool protocol
                                                            (swap EchoLlmClient for real calls)
```

- **FR-1 (agent loop)** is the linchpin; build it against `EchoLlmClient` *before*
  the real adapter, so control flow is tested without a network.
- **FR-9 (argument binding)** and **FR-8 (multi-turn)** are prerequisites for a
  useful loop.
- **FR-5/6 (real adapter)** can proceed in parallel once the loop's shape is
  known; it just implements the seam.
- Container extensions (FR-11/12/13) are independent and can be picked up
  anytime.
- Infra (FR-18/19) is independent; do the repo-layout flatten (FR-19) on a clean
  `main` with no open PRs.

---

## 6. Open design decisions

These must be settled *before* the milestone that depends on them — deliberately,
not by default:

- **OD-1 What is an agent?** A thin bean holding an `LlmClient` + tools, or a
  richer entity with managed memory and a runtime? Drives FR-2/FR-3 and the whole
  0.3 identity. *Decide against a real use case.*
- **OD-2 Memory/conversation lifecycle & scope.** Per call, per conversation, or
  container-managed? Interacts with FR-11 (scopes).
- **OD-3 `LlmClient` shape evolution.** When and how to add multi-turn, tool-use,
  and streaming without breaking the minimal interface. Prefer additive, possibly
  a richer request/response type behind the same method.
- **OD-4 Build-time vs runtime processing.** Stay reflection-at-runtime (Spring-
  like) or add an annotation-processing/build-time path (Quarkus-like) for
  startup/native-image? Large architectural fork; only if NFR-4/NFR-5 demand it.
- **OD-5 Multi-agent model.** Do agents call agents? Orchestration primitives?
  Deferred until single-agent runtime is solid.

---

## 7. Non-goals (for now)

Stating these prevents scope creep:

- Not a general-purpose web framework (no controllers/routing in core).
- Not an ORM or data-access layer.
- Not tied to any single LLM vendor (core stays neutral).
- Not chasing feature parity with Spring — Forge is focused on the AI-native path.
- No premature abstractions: a capability is built when a real need arrives, with
  a spec and tests, not speculatively.

---

## 8. How to advance the roadmap

For any item above:
1. Open a GitHub issue with a **behaviour spec** (Given/When/Then table) and
   acceptance criteria — reuse the `FR-`/`NFR-` text as the starting point.
2. Branch off `main` (independent; stack only for true dependencies, and then
   merge bottom-up).
3. TDD it — failure modes included.
4. Green build + CI → PR that closes the issue → review → merge.
5. Update this roadmap and `docs/ARCHITECTURE.md`.
