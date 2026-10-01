# Forge — Learning Path

**A staged roadmap for any developer who wants to learn this codebase from zero —
from first clone to confidently extending it.**

This is a *curriculum*, not a reference. It tells you **what to read, in what
order, what to do at each step, and how to know you've got it.** It threads
together the project's other docs:

| Doc | What it is | Used in |
|-----|-----------|---------|
| `README.md` | Vision & overview | Stage 0 |
| `docs/ARCHITECTURE.md` | Current structure & decisions | Stage 0–1 |
| `docs/CODEBASE_GUIDE.md` | Every class explained + a worked trace | Stage 1 |
| `docs/BUILDING_FORGE.md` | Masterclass: build it yourself | Stage 2–3 |
| `docs/ROADMAP.md` | Future requirements & sequencing | Stage 4 |

Work top to bottom. Each stage lists a **goal**, **read**, **do**, and a
**checkpoint** — a concrete thing you can now explain or build. Time estimates
assume an intermediate Java developer; adjust to your pace.

---

## Who this is for, and what you'll be able to do

**For:** developers who know basic Java (classes, interfaces, generics, `try`/
`catch`) and want to understand how a dependency-injection framework actually
works under the hood — and then build and extend one.

**By the end you will be able to:**
- Explain how an IoC container discovers, wires, and manages objects.
- Read and navigate the whole Forge codebase confidently.
- Use reflection, classloaders, annotations, and concurrency primitives correctly.
- Rebuild each Forge subsystem from scratch.
- Add a new, tested feature and open a pull request that passes CI.

**You do *not* need** prior Spring/Quarkus experience, or to have written a
framework before. That's what this path is for.

---

## Prerequisites

### Knowledge (brush up if rusty)
- **Core Java:** classes, interfaces, generics (`List<T>`), exceptions, `enum`,
  `final`, static vs instance.
- **Helpful but taught here:** reflection, annotations, the classpath,
  concurrency. If these are new, Stage 2 covers them from first principles.

### Tools
- **JDK 21+** — `java -version` shows 21 or newer. (Temurin is a good choice.)
- **Maven 3.9+** — `mvn -version`. (Or use your IDE's bundled Maven.)
- **Git**, and a GitHub account to contribute.
- **An IDE** — IntelliJ IDEA (Community is fine) or VS Code with the Java
  extensions. An IDE's "go to definition" and debugger make this path far easier.

---

## Setup & first run (~15 min)

```bash
git clone https://github.com/SushrutVaidya/forge.git
cd forge
mvn clean test
```

You should see **`BUILD SUCCESS`** and all tests passing. That green bar is your
proof the toolchain works — on macOS, Linux, or Windows (the code is
OS-independent).

**Open it in your IDE:** open the `forge/pom.xml` *as a project* (not the folder)
so the IDE recognizes it as a Maven project and enables code analysis.

> **Checkpoint 0:** `mvn clean test` is green, and the project opens in your IDE
> with no red errors.

---

## Stage 0 — Orient (~45 min)

**Goal:** understand *what* Forge is and its shape, before any code.

**Read:**
1. `README.md` — the vision and a usage example.
2. `docs/ARCHITECTURE.md` §1–3 — the two layers, the package map, and the
   runtime data flow diagram.

**Do:**
- Run `mvn test` again, but read the test names in the output — they read like a
  specification of what the framework does.
- In your IDE, open `ApplicationContext` and skim its `run()` method. Don't study
  it yet — just see the shape: scan → register → resolve → lifecycle.

> **Checkpoint 0:** you can explain, in two sentences, what "inversion of control"
> means and name the five stages a bean goes through (`scan → define → register →
> resolve → lifecycle`).

---

## Stage 1 — Understand the container (~3–4 hrs)

**Goal:** understand *how* the existing code works, class by class.

**Read:** `docs/CODEBASE_GUIDE.md` end to end, with the code open beside it. Read
each section, then open the class it describes and match the explanation to the
code. Follow this order (it mirrors the data flow):

1. `annotation/` — the markers (`@Omnissiah`, `@Inject`, `@PostConstruct`, …)
2. `exception/` — the `ForgeException` hierarchy
3. `scanner/ForgeScanner` — discovery
4. `definition/BeanDefinition` — bean metadata
5. `registry/BeanRegistry` — the store
6. `resolver/DependencyResolver` — instantiation & injection
7. `lifecycle/LifecycleMetadata` — callbacks
8. `context/ApplicationContext` — the façade
9. `tool/`, `agent/`, `llm/` — the AI layer

**Do:**
- Read the guide's **"full worked trace"** section, then **reproduce it in the
  debugger**: set a breakpoint in `DependencyResolver.resolve`, run
  `ApplicationContextTest`, and step through building a bean and its dependencies.
  Watch the recursion and the singleton cache.
- Open `ApplicationContextTest` and read every test — it's a readable spec of the
  container's behaviour.

> **Checkpoint 1:** you can trace, out loud, what happens from
> `ApplicationContext.run("...")` to a fully-wired bean — including where
> dependencies get created and why `@PostConstruct` runs when it does.

---

## Stage 2 — Master the Java foundations (~4–6 hrs)

**Goal:** truly understand the language mechanisms Forge is built on — the
transferable skills that make you a stronger Java developer anywhere.

**Read:** `docs/BUILDING_FORGE.md` **Part 1** (Foundations) and **Part 2** (DI
theory). Take each subsection slowly:
- JVM runtime model (method area vs heap)
- Classloaders & `getResource`
- Reflection (incl. `initialize=false`, `InvocationTargetException` unwrapping)
- Annotations (retention, target, meta-annotations)
- Generics & type tokens
- Immutability & the Java Memory Model (ConcurrentHashMap, ReentrantLock, volatile)
- Exceptions as design; API design

**Do (small experiments — the best way to learn these):**
- Write a 10-line `main` that uses reflection to list the constructors of
  `BeanDefinition` and print their parameter types.
- In `ForgeScanner.loadClass`, temporarily change `Class.forName(name, false, …)`
  to `true`, add a fixture class with a `static { System.out.println("init!"); }`
  block, run the scanner test, and watch the side effect fire. Then revert it and
  understand why `false` matters. (Don't commit this.)

> **Checkpoint 2:** you can explain why marker annotations must be `RUNTIME`, why
> the scanner loads classes without initializing them, and why the resolver uses
> a `ReentrantLock`.

---

## Stage 3 — Build it yourself (~1–2 weeks, at your pace)

**Goal:** cement understanding by *reconstructing* Forge, subsystem by subsystem.
This is where learning becomes ability.

**Read & do:** `docs/BUILDING_FORGE.md` **Part 3** — the 11 lessons. For each
lesson:
1. Read the problem and the design decision (and its alternatives).
2. In a **separate practice project**, implement it yourself from the skeleton —
   don't copy Forge's source; use the lesson's guidance.
3. Write the tests the lesson lists (happy path *and* failure modes).
4. Only then compare your version to Forge's, and note the differences.

Build in order: marker annotation → scanner → definition → registry → resolver →
lifecycle → context → tools → schemas → stereotypes/agents → LLM seam.

> **Checkpoint 3:** you have your own working mini-container that can scan a
> package, wire beans by constructor injection, detect a cycle, and run
> `@PostConstruct` — built by you, with tests.

---

## Stage 4 — Extend Forge (contribute) (~ongoing)

**Goal:** add real, tested value to the project and go through the full
contribution workflow.

**Read:** `docs/ROADMAP.md` — the functional/non-functional requirements
(`FR-*`/`NFR-*`) and the sequencing plan. Pick a small, self-contained item.
Good first features (no architectural decisions needed):
- **Argument binding (FR-9):** coerce a model's JSON tool arguments into Java
  parameter types.
- **Bean scopes (FR-11):** add a `scope` to `BeanDefinition` and a prototype
  branch in the resolver.
- **Qualifiers (FR-12):** disambiguate by-type lookups by name.

**Do — the Forge workflow:**
1. Open a **GitHub issue** with a behaviour spec (a Given/When/Then table).
2. Branch off `main` (`feat/your-feature`). Branch off `main` for independent
   work — only *stack* branches when one truly depends on another, and then merge
   bottom-up.
3. **Write tests first** — the happy path *and* every failure mode.
4. Implement until green; keep it immutable, non-null, fail-fast.
5. `mvn clean verify` — this runs the tests **and** the JaCoCo coverage gate
   (90% instruction / 85% branch). Stay above it.
6. Open a **PR** that closes the issue. CI runs automatically.

> **Checkpoint 4:** you've merged a PR that adds a tested feature and passes CI
> and the coverage gate.

---

## How to read the code (quick map)

Each class does one thing — read them in dependency order:

```
annotation/*     markers only, no logic
exception/*      one ForgeException hierarchy
scanner          finds component classes
definition       immutable bean metadata (type, name, ctor, deps)
registry         stores definitions + caches singletons
resolver         builds & injects; detects cycles
lifecycle        @PostConstruct / @PreDestroy
context          ties it together; the public API
tool / agent /   the AI layer, built on the container
llm
```

If you only have 20 minutes: read `ApplicationContext`, then
`DependencyResolver`. Those two tell most of the story.

---

## Exploring & debugging tips

- **Run one test:** `mvn -o test -Dtest=DependencyResolverTest` (or right-click →
  Run in your IDE).
- **Use the debugger** on a test — step into `resolve` and watch the recursion.
- **Break a test on purpose** (change an assertion) to see how failures read, then
  revert. Breaking things is a fast way to learn what guarantees exist.
- **Read the test output as docs** — `@DisplayName`s describe the behaviour.
- **Coverage report:** after `mvn verify`, open
  `forge-core/target/site/jacoco/index.html` to see exactly which lines your
  tests exercise.

---

## Exercise ladder

From easiest to hardest (several are detailed in `BUILDING_FORGE.md` Part 6):

1. Add a new `@Omnissiah` bean to a test fixture and assert the scanner finds it.
2. Add a `@PostConstruct` to a fixture and prove, with a test, that it runs after
   injection.
3. Write a test for a **new** cycle shape (e.g. a 4-bean cycle) and confirm the
   message names the full path.
4. Implement the resolver's cycle detection yourself in your practice project.
5. Add **prototype scope** (returns a new instance each time) with tests.
6. Build the **agent reason-act loop** against `EchoLlmClient` (no network).
7. Add **`@Qualifier`** to disambiguate two beans of the same type.

---

## Coding standards (so your PR fits in)

- One responsibility per class; name infrastructure descriptively.
- Validate inputs at the boundary; throw a specific `ForgeException` subtype whose
  message names the offending value.
- Return immutable, non-null collections.
- Favour immutability; make shared state thread-safe.
- Every feature: tests for the happy path **and** each failure mode.
- Keep `forge-core` dependency-free; provider/integration code goes in separate
  modules.

---

## Self-assessment — "you've learned Forge when you can…"

- [ ] Explain inversion of control and constructor injection to someone else.
- [ ] Trace `run()` end to end from memory.
- [ ] Say why marker annotations are `RUNTIME` and why scanning doesn't initialize
      classes.
- [ ] Describe how cycles are detected and why singleton creation is locked.
- [ ] Rebuild the scanner + resolver yourself, with tests.
- [ ] Add a tested feature and get it through CI and the coverage gate.

Tick all six and you don't just *use* Forge — you understand and can extend it.

---

## Further reading

- **Spring Framework** docs — `@Component`/`@Service` stereotypes,
  `BeanFactory`/`ApplicationContext` (the industrial version of what you built).
- **Quarkus / Micronaut** — build-time DI; contrast with runtime reflection.
- **JSR-330 (`jakarta.inject`)** — the standard `@Inject`/`@Named` vocabulary.
- **The Java Language Specification** — classes, initialization, annotations.
- `docs/BUILDING_FORGE.md` Part 7 compares these in context.
