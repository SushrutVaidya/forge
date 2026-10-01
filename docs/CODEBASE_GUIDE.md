# Forge — Codebase Guide

**Understand every line of Forge from scratch, without prior context.**

This document is a complete, self-contained walkthrough of the Forge codebase as
of the 0.1 container plus the tool registry. It assumes you know Java syntax but
explains every framework-level and reflection-level concept from first
principles. Read it top to bottom the first time; use the table of contents as a
reference afterwards.

---

## Table of contents

1. [What Forge is (the mental model)](#1-what-forge-is)
2. [The end-to-end flow in one picture](#2-the-end-to-end-flow)
3. [Java concepts you need first](#3-java-concepts-you-need-first)
4. [Package map](#4-package-map)
5. [Walkthrough: annotations](#5-walkthrough-annotations)
6. [Walkthrough: the exception hierarchy](#6-walkthrough-the-exception-hierarchy)
7. [Walkthrough: ForgeScanner](#7-walkthrough-forgescanner)
8. [Walkthrough: BeanDefinition](#8-walkthrough-beandefinition)
9. [Walkthrough: BeanRegistry](#9-walkthrough-beanregistry)
10. [Walkthrough: DependencyResolver](#10-walkthrough-dependencyresolver)
11. [Walkthrough: LifecycleMetadata](#11-walkthrough-lifecyclemetadata)
12. [Walkthrough: ApplicationContext](#12-walkthrough-applicationcontext)
13. [Walkthrough: the tool layer](#13-walkthrough-the-tool-layer)
14. [A full worked trace of `run()`](#14-a-full-worked-trace)
15. [Design principles and why](#15-design-principles-and-why)
16. [How the tests are built](#16-how-the-tests-are-built)
17. [Build, run, extend](#17-build-run-extend)
18. [Known limitations (deliberate scope)](#18-known-limitations)
19. [Glossary](#19-glossary)

---

## 1. What Forge is

Forge is an **inversion-of-control (IoC) container** — the same category of tool
as the core of Spring. The idea:

> Instead of your code creating its own collaborators with `new`, you *declare*
> your components, and the container creates them and hands each one its
> dependencies. Control of object creation is *inverted* from your code to the
> framework.

Concretely, you write:

```java
@Omnissiah
public class Service {
    private final Repository repository;
    public Service(Repository repository) {   // "I need a Repository"
        this.repository = repository;
    }
}
```

and Forge, at startup, finds `Service`, sees it needs a `Repository`, creates
the `Repository` first, then creates the `Service` passing that repository in.
You never call `new Service(...)` yourself. That is dependency injection.

**Why this is useful:** your classes state their dependencies as constructor
parameters (explicit, testable, `final`), and a single place — the container —
knows how to assemble the whole object graph. Change a wiring and you change one
declaration, not many call sites.

Forge's longer-term goal is to make **AI agents and tools** first-class citizens
of this same container, which is why the tool registry already exists.

---

## 2. The end-to-end flow

```
ApplicationContext.run("com.example.app")
        │
        ▼
ForgeScanner ......... finds every class annotated with @Omnissiah in the package
        │             (returns Class<?> objects — metadata, not instances)
        ▼
BeanDefinition ....... for each class, build immutable metadata:
        │             name, type, which constructor to use, what it depends on
        ▼
BeanRegistry ......... store all definitions by name; also caches the one
        │             instance of each bean once created
        ▼
DependencyResolver ... create each bean: resolve its dependencies first
        │             (recursively), then call its constructor. Detect cycles.
        ▼
ApplicationContext ... eagerly build all beans, run @PostConstruct callbacks;
        │             expose getBean(...); run @PreDestroy on close()
        ▼
ToolRegistry ......... (optional) collect every @Tool method on the beans into
                       a catalog an LLM can invoke by name
```

Each stage has exactly one job. The stages are wired together only by the
`ApplicationContext`; everything else is independent and unit-testable.

---

## 3. Java concepts you need first

These are the language mechanisms Forge is built on. If a walkthrough later
refers to one of these, come back here.

### 3.1 Packages, the classpath, and `.class` files

A Java **package** like `com.example.app` looks like a folder hierarchy, but at
runtime it is **just part of a class's name**. `com.example.app.Service` is a
single flat identifier. There is no runtime object representing "the package"
that you can open and list.

When you compile, each class becomes a `.class` file (bytecode). Those files live
in **classpath roots** — for Maven that is `target/classes` (your code) and
`target/test-classes` (your tests). The **classpath** is the set of these roots.

A path inside the classpath always uses `/` separators (even on Windows), because
it addresses entries inside classpath roots and JARs, not your operating
system's filesystem.

### 3.2 ClassLoaders and `getResource`

A **ClassLoader** is the object that finds and loads classes and resources from
the classpath. The one operation Forge uses:

```java
URL resource = classLoader.getResource("com/example/app");
```

Given a `/`-separated path, it returns a `URL` locating that resource, or `null`
if nothing on the classpath matches. It returns a `URL` (not a `File`) because
the bytes might live in a directory, inside a JAR, or over a network — a `URL`
can name all of those; a `File` cannot.

**There is no JDK method that lists the classes in a package.** That is why every
scanning framework (Spring, Quarkus, Forge) implements discovery by hand: get the
package's directory URL, then walk the filesystem under it.

### 3.3 Reflection

Reflection is the ability to inspect and use classes *by name, at runtime*,
rather than by referencing them in source. The pieces Forge uses:

- `Class<?>` — the runtime handle for a type. `String.class` is a compile-time
  literal; `Class.forName("java.lang.String")` gets the same handle from a
  string at runtime.
- `Class.forName(name, initialize, classLoader)` — load a class by name. The
  `initialize` flag, when `false`, loads the class **without running its static
  initializers** — important so that merely *scanning* a class never executes its
  code.
- `clazz.getDeclaredConstructors()` / `getDeclaredMethods()` — the constructors
  and methods declared on the class.
- `clazz.isAnnotationPresent(Ann.class)` / `clazz.getAnnotation(Ann.class)` —
  test for / read an annotation.
- `constructor.newInstance(args)` — call a constructor reflectively, producing an
  instance. `method.invoke(target, args)` — call a method reflectively.
- `member.setAccessible(true)` — allow reflective access to a non-public
  constructor/method.

When a reflectively-invoked constructor or method throws, the exception is
wrapped in an **`InvocationTargetException`**; the original is available via
`getTargetException()`. Forge unwraps this everywhere so callers see the *real*
error, not the reflection wrapper.

### 3.4 Annotations, retention, and target

An annotation is declared with `@interface`. Two meta-annotations control it:

- `@Retention(RetentionPolicy.RUNTIME)` — keep the annotation available for
  reflection at runtime. The default is `CLASS` (written to the `.class` file but
  **not** loaded into memory), which would make `getAnnotation` return `null`.
  Forge's marker annotations *must* be `RUNTIME` or discovery silently breaks.
- `@Target(ElementType.TYPE)` — restrict where the annotation may be placed
  (here, only on classes/interfaces). Misuse becomes a compile error instead of
  being silently ignored.

An annotation member named `value()` is special: `@Omnissiah("x")` is shorthand
for `@Omnissiah(value = "x")`.

### 3.5 Generics and wildcards

- `Class<?>` — "a `Class` of some unknown type." The `?` is a wildcard; you use
  it when you handle classes generically and don't know (or care) which specific
  type.
- `List<Class<?>>` — a list of such class handles.
- `<T> T getBean(Class<T> type)` — a generic method: the caller's requested type
  `T` flows through, so `getBean(Service.class)` returns a `Service` with no cast
  on the caller's side. Internally `type.cast(object)` performs the checked cast.

### 3.6 Immutability and defensive copies

An **immutable** object cannot change after construction. Forge favours it because
immutable objects are automatically thread-safe and cannot be corrupted by a
caller. `List.copyOf(collection)` returns an unmodifiable copy; calling `add` on
it throws `UnsupportedOperationException`. Forge returns such copies from every
collection getter so callers can never mutate internal state.

### 3.7 Thread-safety primitives

- `ConcurrentHashMap` — a map safe for concurrent reads and writes; `putIfAbsent`
  atomically inserts only if the key is absent (used to detect duplicates).
- `CopyOnWriteArrayList` — a list safe under concurrent iteration/append; used for
  the small, append-mostly creation-order log.
- `ReentrantLock` — a mutual-exclusion lock the *same thread* can acquire
  repeatedly (re-enter). The resolver uses this so a thread building a bean can
  recurse into building its dependencies while holding the lock.
- `volatile` — makes a field's writes immediately visible to other threads; used
  for the context's `closed` flag.

### 3.8 Checked vs unchecked exceptions

- **Checked** (extends `Exception`): the compiler forces you to catch or declare
  them. Example: `IOException`, `URISyntaxException`, `ClassNotFoundException`.
- **Unchecked** (extends `RuntimeException`): no compiler ceremony. Forge's own
  exceptions are all unchecked, because a misconfiguration (a missing bean, a
  cycle) is a programming error you cannot meaningfully recover from at runtime —
  the right response is to fail fast at startup. Forge wraps checked JDK
  exceptions into its own unchecked `ForgeException` types, preserving the
  original as the *cause*.

### 3.9 try-with-resources and AutoCloseable

```java
try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
    ...
}   // entries.close() is called automatically here, even on exception
```

Any object implementing `AutoCloseable` can be declared in the `try (...)` header
and is closed automatically when the block exits. `DirectoryStream` holds an
operating-system file handle; not closing it leaks handles. `ApplicationContext`
is itself `AutoCloseable`, which is how `@PreDestroy` is guaranteed to run.

---

## 4. Package map

```
dev.forge.core
├── annotation   @Omnissiah, @Inject, @PostConstruct, @PreDestroy, @Tool
├── exception    ForgeException (base) + specific subtypes
├── scanner      ForgeScanner            — discovery
├── definition   BeanDefinition          — bean metadata
├── registry     BeanRegistry            — definition store + singleton cache
├── resolver     DependencyResolver      — instantiation + injection
├── lifecycle    LifecycleMetadata       — @PostConstruct/@PreDestroy handling
├── context      ApplicationContext      — public façade + orchestration
└── tool         Tool metadata + ToolRegistry — AI tool execution
```

Dependencies flow downward: `context` uses everything; `annotation` and
`exception` depend on nothing. There are no cycles between packages.

---

## 5. Walkthrough: annotations

All annotations live in `dev.forge.core.annotation`. They are pure markers —
they carry no logic; other classes read them reflectively.

### `@Omnissiah`
Marks a class as a managed component. `@Retention(RUNTIME)` (so the scanner can
read it), `@Target(TYPE)` (classes only), `@Documented`. Its `value()` is the
**optional explicit bean name**; blank means "derive the name from the class."

### `@Inject`
Marks *which* constructor the container should use, and is only needed when a
class declares **more than one** constructor. `@Target(CONSTRUCTOR)`.

### `@PostConstruct` / `@PreDestroy`
Mark no-argument methods to run after a bean is fully wired
(`@PostConstruct`) and when the context closes (`@PreDestroy`).
`@Target(METHOD)`.

### `@Tool`
Marks a method of a bean as an executable tool. Attributes: `name()` (defaults to
the method name) and `description()` (for an LLM). `@Target(METHOD)`.

**Key point:** none of these do anything by themselves. Their power comes
entirely from `RUNTIME` retention, which lets other classes discover them via
reflection.

---

## 6. Walkthrough: the exception hierarchy

`dev.forge.core.exception`. Every framework failure is an **unchecked** exception
under one abstract base:

```
RuntimeException
└── ForgeException  (abstract)
    ├── ForgeScannerException        scanning/discovery failed
    ├── BeanDefinitionException      a class can't become a valid bean
    ├── NoSuchBeanException          asked for a bean that doesn't exist
    ├── NoUniqueBeanException        asked by type, multiple matched
    ├── CircularDependencyException  the dependency graph has a cycle
    ├── BeanInstantiationException   a constructor failed
    ├── LifecycleException           a @PostConstruct/@PreDestroy is invalid/failed
    ├── ToolDefinitionException      the tool catalog is invalid (duplicate name)
    ├── ToolExecutionException       a tool threw or got bad arguments
    └── NoSuchToolException          asked for a tool that doesn't exist
```

**Why a shared base?** So a caller can `catch (ForgeException e)` to handle *any*
Forge failure uniformly, while still being able to catch a specific subtype when
it cares. The base is `abstract` because you never throw a generic "Forge
exception" — you always throw a specific reason.

**Why all unchecked?** These are configuration/wiring errors surfaced at startup.
No caller can sensibly recover from "you have a dependency cycle" at runtime, so
forcing `try/catch` everywhere would be pure noise. Fail fast, fail loud.

---

## 7. Walkthrough: ForgeScanner

`dev.forge.core.scanner.ForgeScanner`. **Job:** given a base package, return every
`@Omnissiah`-annotated `Class<?>` under it. Returns metadata, never instances.

### Construction
Two constructors: the public no-arg one uses `ForgeScanner.class.getClassLoader()`
(the loader that loaded Forge); a package-private one accepts an explicit
`ClassLoader` so tests can control resource resolution. The classloader is stored
as a `final` field, so an instance is immutable and thread-safe.

### `scan(String basePackage)` step by step
1. **Validate the argument.** `null` and blank both throw `ForgeScannerException`
   with distinct messages. Blank is checked separately because an empty string,
   if allowed through, would map to the *classpath root* and cause the whole
   classpath to be scanned.
2. **Package → resource path.** `basePackage.replace('.', '/')` turns
   `com.example` into `com/example` (see §3.1).
3. **Locate the resource.** `classLoader.getResource(path)`. If `null`, the
   package isn't on the classpath → throw (a typo shouldn't silently yield zero
   components).
4. **Protocol guard.** `resource.getProtocol()` must be `"file"`. If it's `"jar"`
   or anything else, throw a clear "filesystem only" error rather than failing
   confusingly downstream. (JAR scanning is deliberately out of scope.)
5. **URL → Path.** `Path.of(resource.toURI())`. Using `toURI()` (not
   `getFile()`) matters: a URL percent-encodes its components, so a path with a
   space would come back as `.../My%20App/...` from `getFile()` and point at a
   directory that doesn't exist. `toURI()` decodes correctly. `toURI()` throws the
   checked `URISyntaxException`, which is wrapped in `ForgeScannerException`.
6. **Walk and collect.** Build a mutable `ArrayList`, call the recursive
   `scanDirectory`, then return `List.copyOf(...)` — mutable while building,
   immutable once returned.

### `scanDirectory(directory, packageName, components)` — the recursion
Opens the directory with `Files.newDirectoryStream(directory)` inside
try-with-resources (so the OS file handle is always released). For each entry:

- **If it's a directory**, recurse, appending the directory name to the package:
  `packageName + "." + fileName`. This is how the fully-qualified class name is
  reconstructed — the package name is *carried down* the recursion rather than
  computed from the filesystem path (which would need OS-specific separator
  handling).
- **If it's a candidate class file** (ends in `.class`, and is not
  `package-info.class` or `module-info.class`, whose names are not valid class
  names), build the FQCN (`packageName + "." + fileName` minus `.class`), load it
  with `Class.forName(name, false, classLoader)` (**`false` = do not initialize**,
  see §3.3), and if it `isAnnotationPresent(Omnissiah.class)`, add it.

`IOException` from reading the directory, and `ClassNotFoundException` /
`LinkageError` from loading, are wrapped in `ForgeScannerException`. In the MVP a
load failure means the FQCN was computed wrong — a bug to surface, not hide.

---

## 8. Walkthrough: BeanDefinition

`dev.forge.core.definition.BeanDefinition`. **Job:** immutable, validated metadata
about one bean — its type, name, the constructor to inject through, and that
constructor's parameter types.

### Why immutable + validated-at-construction
The constructor is `private`; you create a definition only through the static
factories below, and they validate everything. Consequence: **an invalid
`BeanDefinition` cannot exist.** Any definition you hold is guaranteed
instantiable with an unambiguous constructor. This is a recurring Forge pattern —
push validation into construction so the rest of the code can trust the object.

### Factories
- `fromComponent(Class<?>)` — for scanned components. Requires `@Omnissiah`
  (throws `BeanDefinitionException` if absent), derives the name (below), and
  delegates to `of`.
- `of(Class<?> type, String name)` — annotation-agnostic core. Validates the name
  is non-blank and the type is instantiable, resolves the constructor, and builds
  the definition.

### `requireInstantiable`
Rejects interfaces, abstract classes, enums, annotations, arrays, primitives, and
non-static inner classes — anything the container could not `new`.

### `resolveInjectableConstructor`
- Exactly one constructor → use it.
- Several constructors → exactly one must be `@Inject`; zero or more-than-one is a
  `BeanDefinitionException`. (This is why `@Inject` exists.)

### Bean name derivation (`defaultName`)
If `@Omnissiah("x")` gives a name, use it. Otherwise decapitalize the simple class
name: `UserService` → `userService`. Acronyms are preserved (`URLParser` stays
`URLParser`) by the JavaBeans rule "if the first two characters are uppercase,
leave it unchanged." This is implemented by hand to avoid depending on
`java.beans.Introspector` (which lives in the `java.desktop` module).

### `dependencyTypes()`
`List.of(constructor.getParameterTypes())` — the constructor's parameter types in
order, immutable. This is the list the resolver walks to wire dependencies.

`equals`/`hashCode` are by (type, name), so two definitions of the same bean
compare equal.

---

## 9. Walkthrough: BeanRegistry

`dev.forge.core.registry.BeanRegistry`. **Job:** hold all definitions (by name) and
cache each bean's single instance. It stores; it does **not** instantiate.

### State (all thread-safe)
- `definitionsByName` — `ConcurrentHashMap<String, BeanDefinition>`.
- `singletonsByName` — `ConcurrentHashMap<String, Object>` (the instance cache).
- `singletonCreationOrder` — `CopyOnWriteArrayList<String>`, the order beans were
  created, which drives lifecycle ordering (see §11–12).

### Definitions
- `registerDefinition` — `putIfAbsent`; a duplicate **name** throws
  `BeanDefinitionException` (two beans can't share a name).
- `getDefinition(name)` — throws `NoSuchBeanException` if absent (never returns
  null).
- `getDefinitionByType(type)` — filters definitions whose bean type is assignable
  to `type` (so you can look up by an implemented interface). Zero matches →
  `NoSuchBeanException`; more than one → `NoUniqueBeanException` (the MVP has no
  qualifiers to disambiguate). Exactly one → return it.
- `getAllDefinitions` / `getBeanDefinitionNames` — immutable snapshots.

### Singleton cache
- `registerSingleton(name, instance)` — caches once (duplicate throws) and appends
  to the creation-order log.
- `getSingleton(name)` — returns the instance or `null` on a miss. This is the one
  place Forge returns `null`, deliberately: a cache miss is a normal, expected
  outcome the resolver checks for, exactly like `Map.get`.
- `clearSingletons` — drops the cache and the order log (used on context close);
  definitions are kept.

---

## 10. Walkthrough: DependencyResolver

`dev.forge.core.resolver.DependencyResolver`. **Job:** turn definitions into live,
wired singletons. This is where "inversion of control" actually happens.

### State
- `registry` — where definitions and the cache live.
- `creationLock` — a `ReentrantLock` serializing singleton creation.
- `beansInCreation` — a `LinkedHashSet<String>` of beans currently being built on
  the resolution path (insertion-ordered, so a cycle can be printed in order).
  Only ever touched while holding the lock.

### `resolve(String beanName)`
1. Fast path: if the singleton is already cached, return it (no locking).
2. Otherwise acquire `creationLock` and call `createSingleton`. The lock is
   reentrant, so the recursion below (which resolves dependencies) re-enters
   freely on the same thread, while *other* threads wait for the whole graph to
   finish. This guarantees each singleton is built **exactly once** with no race,
   at the cost of serializing construction — fine for a startup-time operation.

### `createSingleton(String beanName)`
1. Re-check the cache (another thread may have finished it; our own recursion may
   revisit a bean).
2. Get the definition (`NoSuchBeanException` if unknown).
3. **Cycle check:** `beansInCreation.add(name)` returns `false` if the name is
   already there → we've looped back to a bean still under construction → throw
   `CircularDependencyException` with the path, e.g. `a -> b -> a`.
4. Resolve constructor arguments: for each dependency *type*, find its definition
   by type and recursively `createSingleton` it. (Dependencies are therefore built
   before their dependents — "post-order".)
5. Instantiate (below), cache it via `registerSingleton`, and — in a `finally` —
   remove the name from `beansInCreation` so the path set stays accurate.

### `instantiate`
`constructor.setAccessible(true)` then `newInstance(args)`. An
`InvocationTargetException` (the constructor threw) is unwrapped so the caller
sees the real cause; any reflective failure becomes `BeanInstantiationException`.

**The key insight:** because dependencies are created before dependents, the
registry's creation-order log ends up in dependency-first order — which is exactly
what the lifecycle needs.

---

## 11. Walkthrough: LifecycleMetadata

`dev.forge.core.lifecycle.LifecycleMetadata`. **Job:** find, validate, and invoke a
bean type's `@PostConstruct` and `@PreDestroy` methods.

- `forType(Class<?>)` scans the class's declared methods for each annotation.
  **Validation:** at most one method per phase (else `LifecycleException`), and it
  must take **no parameters** (else `LifecycleException`). If neither annotation is
  present it returns a shared `NONE` instance (a no-op), so the common case
  allocates nothing.
- `invokePostConstruct(bean)` / `invokePreDestroy(bean)` — `setAccessible(true)`
  then `invoke`, unwrapping `InvocationTargetException` into `LifecycleException`
  so a failing callback surfaces its real cause. A `null` method is a safe no-op.

Callback discovery inspects the bean class's *own* declared methods; inherited
callbacks are out of scope for now.

---

## 12. Walkthrough: ApplicationContext

`dev.forge.core.context.ApplicationContext`. **Job:** the public entry point that
ties everything together and owns the bean lifecycle. Implements `AutoCloseable`.

### `run(String basePackage)` (static factory)
1. Create a `BeanRegistry`.
2. `new ForgeScanner().scan(basePackage)` → for each class,
   `registry.registerDefinition(BeanDefinition.fromComponent(class))`.
3. Create a `DependencyResolver` over the registry, and the context.
4. `instantiateSingletons()` — **eagerly** resolve every definition, so any wiring
   error surfaces now, at startup, not lazily at first use.
5. `initializeSingletons()` — iterate the registry's *creation order*
   (dependencies first), compute each bean's `LifecycleMetadata` (cached in a
   `LinkedHashMap` for reuse at close), and call `@PostConstruct`.

### Lookup
- `getBean(Class<T>)` — find the definition by type, resolve it, and `type.cast`
  the result so the caller gets a typed reference with no cast.
- `getBean(String)` — resolve by name.
- Both first call `ensureOpen()`, which throws `IllegalStateException` if the
  context is closed.
- `getBeanDefinitionNames()` — immutable list of names.

### `close()`
Idempotent (guarded by the `volatile boolean closed`). Iterates the creation order
**in reverse** (dependents before dependencies) and calls each bean's
`@PreDestroy`, then clears the singleton cache. Because the context is
`AutoCloseable`, `try (var ctx = ApplicationContext.run(...)) { ... }` guarantees
this runs.

**Lifecycle ordering, summarized:** creation order = dependency-first (from the
resolver's post-order). `@PostConstruct` runs in creation order; `@PreDestroy`
runs in reverse. So a bean is initialized after its dependencies and destroyed
before them.

---

## 13. Walkthrough: the tool layer

`dev.forge.core.tool`. **Job:** expose `@Tool` methods on beans as an invocable
catalog — the primitive an LLM uses to take actions. Provider-agnostic; no LLM
dependency.

### `ToolMetadata`
Immutable descriptor of one tool: name (from `@Tool.name()` or the method name),
description, the owning bean instance, the `Method`, and the parameter types.
`invoke(Object... args)` calls the method reflectively, wrapping a thrown
exception (unwrapped cause) and an argument mismatch into `ToolExecutionException`.

### `ToolRegistry`
Immutable catalog keyed by tool name.
- `fromBeans(Collection<Object>)` — scan each bean's declared methods for `@Tool`,
  build a `ToolMetadata` per method. A duplicate tool name across the catalog
  throws `ToolDefinitionException`.
- `fromContext(ApplicationContext)` — the ergonomic entry point: pull every bean
  from the context (`getBeanDefinitionNames()` → `getBean(name)`) and delegate to
  `fromBeans`. So a bean's tools are discovered automatically once it's a
  component.
- `getTool(name)` (throws `NoSuchToolException` if absent), `getTools()`,
  `getToolNames()`, `containsTool(name)`, and `invoke(name, args...)`.

Not yet built (see the roadmap): JSON-schema generation for parameters and
argument binding from JSON, which an LLM client will need to actually *choose* and
*call* a tool.

---

## 14. A full worked trace

Say you run `ApplicationContext.run("shop")` and `shop` contains:

```java
@Omnissiah class Repository { @PostConstruct void open(){} @PreDestroy void close(){} }
@Omnissiah class Service    { Service(Repository r){...}  @PostConstruct void warm(){} }
@Omnissiah class Controller { Controller(Service s){...} }
```

1. **Scan.** `ForgeScanner.scan("shop")` walks `target/classes/shop`, loads the
   three classes, keeps all three (all `@Omnissiah`), returns them.
2. **Define.** For each: `BeanDefinition.fromComponent`. Names become
   `repository`, `service`, `controller`. `service`'s `dependencyTypes()` is
   `[Repository]`; `controller`'s is `[Service]`; `repository`'s is `[]`.
3. **Register.** All three definitions go into the registry.
4. **Instantiate (eager).** The context resolves each. Suppose it starts with
   `controller`:
   - `createSingleton("controller")` → needs `Service` → `createSingleton("service")`
     → needs `Repository` → `createSingleton("repository")` → no deps → `new
     Repository()`, cache it (creation order: `[repository]`).
   - back in `service`: `new Service(repository)`, cache (order: `[repository,
     service]`).
   - back in `controller`: `new Controller(service)`, cache (order: `[repository,
     service, controller]`).
   Resolving the other definitions now just hits the cache.
5. **Initialize.** In creation order: `repository.open()`, then `service.warm()`.
   (`controller` has no `@PostConstruct`.)
6. **Use.** `context.getBean(Controller.class)` returns the cached controller,
   whose `service` and transitively `repository` are the same singletons.
7. **Close.** In reverse order: `controller` (no `@PreDestroy`), `service` (none),
   `repository.close()`. Cache cleared.

---

## 15. Design principles and why

- **Zero runtime dependencies.** `forge-core` depends only on the JDK. Fewer
  dependencies = fewer conflicts and a smaller attack surface for a framework
  others embed.
- **Fail fast, fail loud.** Bad configuration throws at startup with a message
  naming the problem. The alternative — silently doing nothing — is the most
  expensive bug to diagnose.
- **Immutable where possible; thread-safe always.** Definitions are immutable;
  shared mutable state uses concurrent collections and a lock.
- **Never return null collections.** Every collection getter returns an immutable,
  non-null snapshot. The single deliberate `null` is the singleton *cache* miss.
- **Validate at construction.** A `BeanDefinition` or `LifecycleMetadata` you hold
  is already valid, so downstream code needn't re-check.
- **One responsibility per class.** Scanner discovers; registry stores; resolver
  builds; context orchestrates. Each is testable in isolation, and the AI layer
  bolts on without touching them.
- **Reproducible builds.** Dependency and plugin versions are pinned centrally in
  the parent POM.

---

## 16. How the tests are built

Tests live in `forge-core/src/test/java`, mirroring the main package structure,
and use **JUnit 5** (Jupiter). Patterns you'll see:

- **`@Nested` classes** group related cases (e.g. `Validation`, `Discovery`) so
  the report reads like a spec.
- **`@DisplayName`** gives each test a human-readable sentence.
- **Fixtures** — small purpose-built classes the tests operate on — live in
  `...fixtures` / `...app` sub-packages. Some are nested `public static` classes in
  a holder (compact); some are top-level classes in a package the scanner can find
  on the *test* classpath.
- **Failure modes are tested explicitly** with `assertThrows`, including that a
  wrapped exception preserves its cause.
- **Concurrency** is tested for the resolver: 16 threads resolve the same bean and
  must observe the identical instance.

Everything runs **offline**: dependencies are cached in `~/.m2`, so
`mvn -o clean test` works without network access.

---

## 17. Build, run, extend

### Build & test
```bash
cd forge
mvn clean test        # or: mvn -o clean test   (offline)
```

### Use it in code
```java
try (ApplicationContext ctx = ApplicationContext.run("com.example.app")) {
    MyService s = ctx.getBean(MyService.class);
    s.run();
}
```

### Recipe: add a new feature the Forge way
1. Open a GitHub issue with a behaviour spec (a Given/When/Then table).
2. Create a feature branch off `main` (**not** stacked on another feature branch —
   stacked PRs must be merged bottom-up in order or they don't reach `main`).
3. Put new public API in its own package; keep it dependency-light.
4. Validate inputs at the boundary; throw a specific `ForgeException` subtype with
   a message that names the offending value.
5. Return immutable, non-null collections.
6. Write JUnit 5 tests for the happy path **and every failure mode**.
7. `mvn clean test` green → open a PR that closes the issue.

---

## 18. Known limitations (deliberate scope)

These are conscious MVP boundaries, not oversights:

- **Filesystem scanning only.** No scanning inside JARs; no thread-context
  classloader. Target is IDE/Maven execution where one classloader sees
  everything.
- **Singleton scope only.** No prototype/request/session scopes.
- **Constructor injection only.** No field or setter injection (by design —
  constructor injection keeps dependencies explicit and testable).
- **By-type lookup has no qualifiers.** Two beans of the same type is an ambiguity
  error; there's no `@Qualifier`/`@Primary` yet.
- **Lifecycle callbacks aren't inherited.** Only methods declared on the bean's own
  class are considered.
- **Tools have no JSON schema yet.** They can be discovered and invoked in code,
  but an LLM can't yet be handed a machine-readable description to choose from.

---

## 19. Glossary

- **Bean** — an object whose creation and lifecycle the container manages.
- **Component** — a class marked `@Omnissiah`, i.e. a candidate bean.
- **Definition** (`BeanDefinition`) — immutable metadata describing how to build a
  bean.
- **Singleton** — the single shared instance of a bean within a context.
- **Container / context** (`ApplicationContext`) — the runtime that scans,
  registers, builds, and manages beans.
- **Injection** — the container supplying a bean's dependencies (here, via its
  constructor).
- **Inversion of control (IoC)** — the framework, not your code, controls object
  creation and wiring.
- **FQCN** — fully-qualified class name, e.g. `com.example.app.Service`.
- **Reflection** — inspecting/using types by name at runtime.
- **Retention** — how long an annotation survives (`SOURCE`/`CLASS`/`RUNTIME`).
- **Tool** — a bean method marked `@Tool`, exposed for invocation by name.
```

---

*This guide covers the 0.1 DI container and the tool registry. For the current
issue list and roadmap (LLM client, agents, prompt engine), see the GitHub
milestones.*
