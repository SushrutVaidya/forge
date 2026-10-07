# Forge

> **An AI-native Java framework — a lightweight inversion-of-control container for building intelligent applications.**

![CI](https://github.com/SushrutVaidya/forge/actions/workflows/ci.yml/badge.svg)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![License: MIT](https://img.shields.io/badge/License-MIT-green)

Forge discovers your components, wires them together through constructor injection,
and manages their lifecycle — then layers AI concerns (tools, agents, LLM access)
on the same container. It has **zero runtime dependencies** and is built from first
principles in Java 21.

Think *"Spring, but designed for AI-native applications."*

---

## Quick start

Annotate a class to make it a managed component; declare dependencies in the
constructor:

```java
import dev.forge.core.annotation.*;

@Omnissiah
public class Repository {
    @PostConstruct void open()  { /* acquire resources */ }
    @PreDestroy   void close() { /* release resources */ }
}

@Omnissiah
public class Service {
    private final Repository repository;
    public Service(Repository repository) {   // Forge injects this
        this.repository = repository;
    }
}
```

Bootstrap the container in one line and fetch a fully-wired bean:

```java
import dev.forge.core.context.ApplicationContext;

try (ApplicationContext context = ApplicationContext.run("com.example.app")) {
    Service service = context.getBean(Service.class);
    // ... use the application ...
} // @PreDestroy callbacks fire here
```

`run` scans the package, registers a bean per `@Omnissiah` type, eagerly builds and
wires every singleton (failing fast on misconfiguration), then runs
`@PostConstruct` in dependency order. Closing the context runs `@PreDestroy` in
reverse.

---

## What's built

**The container (complete):**
- **Component scanning** — recursive classpath discovery of `@Omnissiah` types.
- **Constructor dependency injection** — explicit, `final`-friendly, testable;
  `@Inject` disambiguates when a class has several constructors.
- **Singletons** — created once, shared, thread-safe.
- **Circular-dependency detection** — fails fast and names the cycle.
- **Lifecycle** — `@PostConstruct` / `@PreDestroy`, ordered by the dependency graph.
- **One fail-fast exception hierarchy** — misconfiguration surfaces at startup.

**The AI layer (foundations):**
- **`@Tool`** — expose a bean method as an executable tool, discovered into a
  `ToolRegistry` and invocable by name.
- **Tool schemas** — each tool has a machine-readable, LLM-ready description.
- **`@Agent`** — a stereotype making agents first-class managed beans, located via
  an `AgentRegistry`.
- **`LlmClient`** — a provider-neutral seam to a language model, with an in-memory
  `EchoLlmClient` for offline tests.

Quality: **136 tests**, a JaCoCo **coverage gate** (90% instruction / 85% branch),
and **CI** on every push and pull request.

---

## Architecture

```
ForgeScanner        discovers @Omnissiah / @Agent classes on the classpath
   │
BeanDefinition      immutable, self-validating metadata per bean
   │
BeanRegistry        thread-safe store of definitions + singleton cache
   │
DependencyResolver  constructor injection, cycle detection, exactly-once singletons
   │
ApplicationContext  public façade: run(), getBean(), close(); owns lifecycle
   │
Tools · Agents · LLM   the AI layer, built on the container
```

The AI layer depends on the container, never the reverse.

---

## Build & run

Requires **JDK 21+** and **Maven 3.9+**.

```bash
# build and run the full test suite (verifies the framework)
mvn clean verify

# see it work: run the demo application
mvn -q -pl forge-example -am compile
java -cp "forge-core/target/classes:forge-example/target/classes" dev.forge.example.DemoApplication
#   (Windows: use ';' instead of ':' in -cp; or just Run DemoApplication in your IDE)
```

The demo prints the whole pipeline in action — scanning, injection, lifecycle,
tool invocation, an LLM-ready tool schema, and agent lookup.

> **Note:** Forge is a *library*, not an executable — you run an application that
> *uses* it (see `forge-example`). Run from exploded classes (IDE / Maven); fat-jar
> (`java -jar`) scanning is a deliberately-deferred limitation.

---

## Documentation

| Doc | Purpose |
|-----|---------|
| [`docs/LEARNING_PATH.md`](docs/LEARNING_PATH.md) | **Start here** — a staged path to learn the codebase from zero |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Current structure, data flow, and decisions |
| [`docs/CODEBASE_GUIDE.md`](docs/CODEBASE_GUIDE.md) | Every class explained, with a worked trace |
| [`docs/BUILDING_FORGE.md`](docs/BUILDING_FORGE.md) | A masterclass: build Forge yourself |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Future requirements and sequencing |

---

## Design principles

- **Zero runtime dependencies** — provider/integration code lives in separate modules.
- **Immutable where possible; thread-safe always.**
- **Fail fast** — bad configuration throws at startup with a message naming the cause.
- **Never return null collections** — immutable, non-null results.
- **One responsibility per class.**

---

## Roadmap

- [x] Component scanner
- [x] Bean registry
- [x] Dependency injection (constructor) + cycle detection
- [x] Application context + lifecycle
- [x] Tool registry + tool schemas
- [x] `@Agent` stereotype + agent registry
- [x] Provider-neutral `LlmClient` abstraction
- [x] CI + coverage gate + runnable example
- [ ] Agent runtime (the reason → call-tool → respond loop)
- [ ] Anthropic/Claude `LlmClient` adapter (separate module)
- [ ] Prompt engine
- [ ] Forge CLI

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for detailed requirements.

---

## Contributing

Every change: open an issue with a behaviour spec → branch off `main` → write tests
(happy path **and** failure modes) → `mvn clean verify` green → open a PR. CI and the
coverage gate run automatically.

---

## License

MIT.
