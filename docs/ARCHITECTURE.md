# Forge — Architecture

The current architecture of Forge: its structure, boundaries, data flow, and the
decisions behind them. This is the high-altitude view — for a class-by-class
walkthrough see `docs/CODEBASE_GUIDE.md`, and to learn to build it yourself see
`docs/BUILDING_FORGE.md`.

> **Status.** This reflects the **0.1 DI Container** (merged) and the **0.2 Tool &
> Agent Foundations** (built; some pieces are in open PRs at time of writing). The
> merge status of each component is noted where relevant.

---

## 1. System overview

Forge is a **zero-runtime-dependency inversion-of-control container** for Java 21,
with a thin AI-native layer (tools, tool schemas, agents, an LLM seam) built on
top of it. It is a single module today, `forge-core`.

Two layers:

```
┌──────────────────────────────────────────────────────────────┐
│  AI layer      @Tool · ToolRegistry · ToolSchema               │
│  (0.2)         @Agent · AgentRegistry · LlmClient/EchoLlmClient │
├──────────────────────────────────────────────────────────────┤
│  Container     scanner → definition → registry → resolver      │
│  (0.1)         → context → lifecycle                            │
└──────────────────────────────────────────────────────────────┘
             built on the JDK only — no runtime dependencies
```

The container layer is complete and self-sufficient. The AI layer *uses* the
container (agents are beans, tools are bean methods) but the container has no
knowledge of the AI layer — the dependency points one way, upward.

---

## 2. Module & package structure

```
dev.forge.core
├── annotation   @Omnissiah, @Inject, @PostConstruct, @PreDestroy,
│                @Tool, @Agent, Stereotypes
├── exception    ForgeException (abstract base) + specific subtypes
├── scanner      ForgeScanner
├── definition   BeanDefinition
├── registry     BeanRegistry
├── resolver     DependencyResolver
├── lifecycle    LifecycleMetadata
├── context      ApplicationContext          ← public entry point
├── tool         Tool metadata, ToolRegistry, ToolSchema, ToolParameter
├── agent        AgentRegistry
└── llm          LlmClient, LlmRequest, LlmResponse, EchoLlmClient
```

**Merge status:** `scanner`, `definition`, `registry`, `resolver`, `lifecycle`,
`context`, and the tool *registry* are on `main`. Tool *schemas* (#24), `@Agent`
+ `Stereotypes` (#19), and the `llm` package (#18) are in open PRs.

---

## 3. Runtime data flow

The canonical path, from bootstrap to a wired application:

```
ApplicationContext.run(basePackage)
   │
   ├─▶ ForgeScanner.scan(basePackage)
   │       walks the package directory, loads classes without initializing them,
   │       returns Class<?> of every component (@Omnissiah, or a stereotype of it)
   │
   ├─▶ BeanDefinition.fromComponent(class)      (one per discovered class)
   │       immutable, validated metadata: name, type, injectable constructor,
   │       dependency types
   │
   ├─▶ BeanRegistry.registerDefinition(...)     (store by name)
   │
   ├─▶ DependencyResolver.resolve(name)         (for every definition, eagerly)
   │       post-order graph build: resolve dependencies first, then construct;
   │       cache the singleton; detect cycles; serialize under a reentrant lock
   │
   └─▶ LifecycleMetadata.invokePostConstruct(bean)
           in creation order (dependencies first)

getBean(type|name) ─▶ returns the cached singleton

close()  ─▶ invokePreDestroy in reverse creation order ─▶ clear singletons
```

The AI layer sits beside this: after `run()`, `ToolRegistry.fromContext(ctx)` and
`AgentRegistry.fromContext(ctx)` read the context's beans to build their catalogs;
`ToolSchema` turns tool metadata into an LLM-consumable description; `LlmClient`
is the seam to a model.

---

## 4. Component responsibilities & collaborators

| Component | Single responsibility | Depends on |
|-----------|----------------------|------------|
| `ForgeScanner` | Discover component classes on the classpath | annotation, exception |
| `BeanDefinition` | Immutable, validated per-bean metadata | annotation, exception |
| `BeanRegistry` | Store definitions; cache singletons; look up | definition, exception |
| `DependencyResolver` | Instantiate + inject; detect cycles; build once | registry, definition, exception |
| `LifecycleMetadata` | Find/validate/invoke lifecycle callbacks | annotation, exception |
| `ApplicationContext` | Orchestrate the above; own lifecycle; public API | all of the above |
| `ToolRegistry` / `ToolMetadata` | Discover & invoke `@Tool` methods | annotation, context, exception |
| `ToolSchema` / `ToolParameter` | Machine-readable tool descriptions | tool |
| `AgentRegistry` | Locate `@Agent` beans | annotation, context, exception |
| `LlmClient` + value types | Provider-neutral model access | exception |

Each row is describable in one sentence — the litmus test for single
responsibility.

---

## 5. Dependency rules

Package dependencies flow **downward and never cycle**:

```
context ─▶ scanner, definition, registry, resolver, lifecycle
resolver ─▶ registry, definition
registry ─▶ definition
tool, agent ─▶ context (+ annotation, exception)
everything ─▶ exception, annotation   (the two leaf packages)
llm ─▶ exception
```

- `annotation` and `exception` depend on nothing (leaves).
- The **AI layer depends on the container, never the reverse.** You could delete
  `tool`/`agent`/`llm` and the container still compiles and runs.
- No package imports a package that (transitively) imports it back. This acyclic
  structure is what lets any layer be understood, tested, and changed in
  isolation.

---

## 6. Key architectural decisions (and why)

Concise decision records. Each is a deliberate stance, not an accident.

- **AD1 — Metadata vs instances are separate.** The scanner and definitions work
  with `Class`/`Constructor` (method-area metadata); only the resolver produces
  heap instances. *Why:* clean separation of "what exists" from "what's built,"
  and it makes discovery testable without side effects.
- **AD2 — Constructor injection only.** *Why:* explicit dependencies, `final`
  fields, container-free testability. Field/setter injection is deferred and
  discouraged.
- **AD3 — Eager singletons at startup.** The context instantiates everything in
  `run()`. *Why:* misconfiguration fails loudly at startup, not lazily at first
  use.
- **AD4 — Fail fast, unchecked exceptions, one base.** All errors are unchecked
  `ForgeException` subtypes. *Why:* container misconfiguration is a programming
  error; forcing `try/catch` would be noise. One base enables uniform handling.
- **AD5 — Immutable, non-null, validated-at-construction.** Definitions, value
  types, and returned collections are immutable; collections are never null; a
  constructed object is always valid. *Why:* thread-safety for free, and code
  that doesn't re-validate.
- **AD6 — Serialize singleton creation under a reentrant lock.** *Why:* exactly-
  once construction with no race, and simple cycle detection; the cost
  (serialized construction) is irrelevant at startup.
- **AD7 — Stereotypes via meta-annotations.** `@Agent` carries `@Omnissiah`;
  discovery resolves one level of meta-annotation. *Why:* agents are ordinary
  beans plus a tag, with no duplication of container machinery. (Mirrors Spring's
  `@Service`.)
- **AD8 — Provider-neutral AI layer.** Tools and schemas emit plain data;
  `LlmClient` is an interface with an in-memory impl. *Why:* `forge-core` stays
  vendor-free and offline-testable; real adapters live in separate modules.
- **AD9 — Zero runtime dependencies.** *Why:* a framework others embed should add
  no dependency conflicts or attack surface.
- **AD10 — Filesystem-only scanning, own classloader.** *Why:* the MVP targets
  IDE/Maven execution where one classloader sees everything; JAR scanning and
  thread-context classloaders are deferred as bounded future work.

---

## 7. Cross-cutting concerns

- **Thread safety.** Immutable objects everywhere possible; `ConcurrentHashMap`
  for the registry maps; `CopyOnWriteArrayList` for the creation-order log;
  `ReentrantLock` for singleton creation; `volatile` for the context's closed
  flag. The scanner and definitions are stateless/immutable and safe by
  construction.
- **Error handling.** One `ForgeException` hierarchy; low-level checked
  exceptions are wrapped with cause preserved; `InvocationTargetException` is
  unwrapped so callers see the real cause; messages name the offending value.
- **Reflection safety.** Classes are loaded with `initialize=false` (no side
  effects while scanning); `setAccessible(true)` enables package-private
  members; `-parameters` retains real parameter names for tool schemas.
- **Determinism / testability.** No hidden global state; the classloader and LLM
  client are injectable; tests run fully offline.

---

## 8. Public API surface (what users touch)

- **Annotations:** `@Omnissiah`, `@Inject`, `@PostConstruct`, `@PreDestroy`,
  `@Tool`, `@Agent`.
- **Entry point:** `ApplicationContext.run(basePackage)`, `getBean`, `close`
  (`AutoCloseable`).
- **AI:** `ToolRegistry`, `ToolSchema`, `AgentRegistry`, `LlmClient` (+
  `LlmRequest`/`LlmResponse`).

Everything else is internal and may change. The small, mechanism-hiding surface
is what allows internal rework without breaking users.

---

## 9. Extension points (seams)

Where Forge is designed to grow, without rewrites:

- **Scanning source** — the scanner's filesystem walk can become one strategy
  beside a JAR-scanning strategy (the protocol guard is the current seam).
- **Bean scope** — `BeanDefinition` can carry a scope; the resolver branches on
  it (singleton today).
- **Injection point resolution** — by-type today; qualifiers/primary can extend
  `getDefinitionByType`.
- **LLM providers** — implement `LlmClient` in a separate module.
- **Stereotypes** — new component categories are just meta-annotations of
  `@Omnissiah`, like `@Agent`.

---

## 10. Constraints & scope boundaries (current)

Deliberate limits, documented so they're not mistaken for gaps:

- Filesystem classpath scanning only (no JARs, no thread-context classloader).
- Singleton scope only.
- Constructor injection only; no qualifiers/primary.
- Lifecycle callbacks are not inherited (declared methods only).
- Tools have schemas but no argument binding from model JSON, and no
  conversation/execution loop yet.
- Single module; no CLI; no configuration/environment system.

See `docs/ROADMAP.md` for how these are intended to evolve.
