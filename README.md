# Forge

**An AI-native Java framework, built from first principles.**

Forge is a lightweight inversion-of-control container for Java 21 with **zero
runtime dependencies**. It discovers your components, wires them together
through constructor injection, and manages their lifecycle — the foundation on
which its AI-native features (agents, tools, LLM runtime) are being built.

---

## Quick start

Annotate a class with `@Omnissiah` to make it a managed component:

```java
import dev.forge.core.annotation.Omnissiah;
import dev.forge.core.annotation.PostConstruct;
import dev.forge.core.annotation.PreDestroy;

@Omnissiah
public class Repository {
    @PostConstruct
    void open() { /* acquire resources */ }

    @PreDestroy
    void close() { /* release resources */ }
}

@Omnissiah
public class Service {
    private final Repository repository;

    // Dependencies are injected through the constructor.
    public Service(Repository repository) {
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
} // @PreDestroy callbacks fire automatically on close
```

`run` scans the package, registers one bean per `@Omnissiah` type, eagerly
instantiates and wires every singleton, then invokes `@PostConstruct` in
dependency-first order. Closing the context invokes `@PreDestroy` in reverse.

---

## Features

- **Component scanning** — recursive classpath discovery of `@Omnissiah` types.
- **Constructor dependency injection** — explicit, `final`-friendly, testable
  without the container. `@Inject` disambiguates when a class has several
  constructors.
- **Singleton beans** — created once, shared, thread-safe.
- **Circular-dependency detection** — fails fast at startup and names the cycle.
- **Lifecycle callbacks** — `@PostConstruct` and `@PreDestroy`, ordered by the
  dependency graph.
- **Fail-fast, meaningful errors** — a single `ForgeException` hierarchy;
  misconfiguration surfaces at startup, not at first use.

## Design principles

- **Zero runtime dependencies.** The framework depends on nothing but the JDK.
- **Immutable, thread-safe internals.** Definitions are immutable; the registry
  and resolver are safe under concurrent use.
- **Never return null collections.** Every collection result is immutable and
  non-null.
- **Reproducible builds.** Dependency and plugin versions are pinned centrally.

## Architecture

```
ForgeScanner        discovers @Omnissiah classes on the classpath
   │
BeanDefinition      immutable, self-validating metadata per bean
   │
BeanRegistry        thread-safe store of definitions + singleton cache
   │
DependencyResolver  constructor injection, cycle detection, exactly-once singletons
   │
ApplicationContext  public façade: run(), getBean(), close(); owns lifecycle
```

## Build

Requires JDK 21+ and Maven.

```bash
cd forge
mvn clean test
```

## Status

Milestone **Forge 0.1 — DI Container** delivers the complete container
described above. The AI-native runtime (agent registry, prompt engine, tool
registry, LLM adapters) builds on this foundation and is tracked in the issue
list.
