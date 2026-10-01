# Building Forge From Scratch — A Masterclass

**Read this, and you can write Forge yourself — and understand *why* every line is
the way it is.**

Where `docs/CODEBASE_GUIDE.md` *explains the code that exists*, this document
*teaches you to build it*. It is a progressive course: each lesson states a
problem, teaches the Java mastery you need, walks the design decision (with the
alternatives and their trade-offs), shows you how to build it, how to test it,
and the traps to avoid — then leaves you exercises. Work through it in order.

By the end you will understand, deeply enough to reconstruct and extend
independently: reflection, classloading, the Java memory model, annotation
processing, generics, immutable API design, dependency-injection theory, and the
engineering discipline (tests, PRs, CI) that keeps a framework alive.

---

## Table of contents

- [Part 0 — Mindset](#part-0--mindset)
- [Part 1 — The foundations you must own](#part-1--the-foundations-you-must-own)
  - [1.1 The JVM runtime model](#11-the-jvm-runtime-model)
  - [1.2 Classloaders](#12-classloaders)
  - [1.3 Reflection](#13-reflection)
  - [1.4 Annotations](#14-annotations)
  - [1.5 Generics and type tokens](#15-generics-and-type-tokens)
  - [1.6 Immutability and the Java Memory Model](#16-immutability-and-the-java-memory-model)
  - [1.7 Exceptions as design](#17-exceptions-as-design)
  - [1.8 API design principles](#18-api-design-principles)
- [Part 2 — Dependency injection, from first principles](#part-2--dependency-injection-from-first-principles)
- [Part 3 — Build it, lesson by lesson](#part-3--build-it-lesson-by-lesson)
  - [Lesson 1 — The marker annotation](#lesson-1--the-marker-annotation)
  - [Lesson 2 — The scanner](#lesson-2--the-scanner)
  - [Lesson 3 — The bean definition](#lesson-3--the-bean-definition)
  - [Lesson 4 — The registry](#lesson-4--the-registry)
  - [Lesson 5 — The resolver](#lesson-5--the-resolver)
  - [Lesson 6 — Lifecycle](#lesson-6--lifecycle)
  - [Lesson 7 — The application context](#lesson-7--the-application-context)
  - [Lesson 8 — Tools](#lesson-8--tools)
  - [Lesson 9 — Tool schemas](#lesson-9--tool-schemas)
  - [Lesson 10 — Stereotypes and agents](#lesson-10--stereotypes-and-agents)
  - [Lesson 11 — The LLM seam](#lesson-11--the-llm-seam)
- [Part 4 — Building the next things yourself](#part-4--building-the-next-things-yourself)
- [Part 5 — The engineering discipline](#part-5--the-engineering-discipline)
- [Part 6 — Exercises](#part-6--exercises)
- [Part 7 — How the giants do it](#part-7--how-the-giants-do-it)

---

## Part 0 — Mindset

Three habits separate people who *use* frameworks from people who *build* them:

1. **Ask "why does this mechanism exist?" before "how do I call it?"** A method
   like `getResource` returning `null` isn't arbitrary — it reflects a design
   stance (lookups are allowed to miss). When you understand the stance, you
   never misuse the API.
2. **Design the seam before the implementation.** A *seam* is a small, stable
   surface (an interface, a method signature) that hides a decision you might
   change later. Cheap to add now, priceless when requirements shift. An
   *abstraction* (a whole hierarchy) is expensive — build it only when a second
   case actually arrives.
3. **Make illegal states unrepresentable.** If an object can only be created
   through a factory that validates it, then *every* instance you hold is valid,
   and the rest of your code stops re-checking. Push validation to the boundary.

Keep these in view; every lesson applies them.

---

## Part 1 — The foundations you must own

You cannot build a container without genuinely understanding these. Don't skim.

### 1.1 The JVM runtime model

When the JVM runs, memory is divided into regions. The three that matter here:

- **Method area** (part of "metaspace" in modern JVMs): holds each loaded
  *class's* structure — its bytecode, field/method metadata, the runtime
  constant pool, and the `Class` object. One per class, shared by all instances.
- **Heap**: holds *objects* (instances). `new Service()` allocates here.
- **Stack**: one per thread; holds stack frames for method calls (local
  variables, operands).

The key mental split for a framework author: **a class's metadata lives in the
method area and is what you inspect with reflection; instances live on the heap
and are what dependency injection creates.** Forge's scanner and definitions work
with metadata (`Class`, `Constructor`); the resolver produces heap instances.
"Return metadata, not instances" is this distinction made into an API rule.

A class is only *loaded* (its metadata built) when first needed, and only
*initialized* (its `static` blocks run) at first *active use*. These are two
separate events — remember this for reflection.

### 1.2 Classloaders

A **classloader** turns a class *name* into a loaded `Class`, by locating its
bytecode on the classpath. Facts you must internalize:

- **Packages are names, not containers.** `com.example.Service` is one flat
  identifier. There is no runtime "package object" to enumerate. This is *why*
  scanning requires walking the filesystem — the JDK offers no "list classes in
  package" call, because there is nothing to list.
- **`getResource(path)`** answers "do you have bytes at this `/`-separated
  path?" with a `URL` or `null`. It returns a `URL`, not a `File`, because bytes
  might be in a directory, a JAR, a nested JAR, or over a network. The `URL`'s
  *protocol* (`file`, `jar`, `http`) tells you which.
- **Delegation.** Classloaders form a parent-first chain: a loader asks its
  parent before trying itself. This matters when a framework in one loader must
  load user classes in a child loader (servlet containers, plugins). For a plain
  `java`/Maven run, one loader sees everything — which is why Forge's MVP uses
  its own loader and defers the thread-context-classloader complexity.

**Exercise your understanding:** why can two classes with the *same name* loaded
by *different* classloaders be considered different types by the JVM? (Because a
class's identity is `name + defining classloader`.) This is the root of most
"ClassCastException: X cannot be cast to X" mysteries.

### 1.3 Reflection

Reflection is using types *by name, at runtime*. The API surface you need:

```java
Class<?> c = Class.forName("com.example.Service", false, loader);
```

`forName`'s three arguments are the whole lesson:
- the binary name (`Outer$Inner` for nested types, `$` not `.`);
- **`initialize`** — pass `false` to *load without running static initializers*.
  This is critical: if you scan a class merely to read an annotation, you must
  not accidentally run its `static { ... }` (which might open a database). A
  scanner that initializes classes has *side effects on the code it inspects* —
  a serious bug. Always `false` for inspection.
- the classloader to load from.

Then:
```java
Constructor<?>[] ctors = c.getDeclaredConstructors();  // incl. non-public
Object instance = ctor.newInstance(arg1, arg2);        // invoke a constructor
Method m = c.getDeclaredMethod("save");
m.invoke(instance);                                    // invoke a method
member.setAccessible(true);                            // bypass access checks
c.isAnnotationPresent(Ann.class);                      // annotation test
Ann a = c.getAnnotation(Ann.class);                    // read annotation
```

**The one trap that will bite you:** when a constructor or method you invoke
*throws*, reflection wraps that exception in an
`InvocationTargetException`. The real exception is `e.getTargetException()` (or
`e.getCause()`). If you propagate the wrapper, your users see a meaningless
`InvocationTargetException` instead of *their* `IllegalStateException`. **Always
unwrap it.** Forge does this in three places (resolver, lifecycle, tools) — and a
test caught the one time it was forgotten.

**Parameter names:** by default the compiler discards them, so
`Parameter.getName()` returns `arg0`, `arg1`. To keep the real names (needed for
tool schemas), compile with `-parameters` (in Maven:
`<maven.compiler.parameters>true</maven.compiler.parameters>`).

### 1.4 Annotations

An annotation is an interface declared with `@interface`. Its members are
*methods* (that's why you read `annotation.value()`, not a field). The instance
you get from `getAnnotation` is a **JVM-synthesized proxy** implementing that
interface.

Two meta-annotations decide everything:

- **`@Retention`** — how long the annotation survives:
  - `SOURCE`: discarded after compile (e.g. `@Override`).
  - `CLASS`: in the `.class` file but **not loaded into memory** — this is the
    *default*, and it is a trap: `getAnnotation` returns `null` for it.
  - `RUNTIME`: readable by reflection. **Your marker annotations must be
    `RUNTIME`** or the whole discovery mechanism silently finds nothing.
- **`@Target`** — where it may be placed (`TYPE`, `METHOD`, `CONSTRUCTOR`, …).
  Omitting it lets the annotation go anywhere, so misuse (e.g. on a method that
  your framework only scans on types) is *silently ignored* instead of being a
  compile error. Always constrain it.

**Meta-annotations / stereotypes.** An annotation can itself be annotated. If
`@Agent` carries `@Omnissiah`, then `@Agent` is a *stereotype* meaning "also a
component." But beware: `agentClass.getAnnotation(Omnissiah.class)` returns
**null** — meta-annotations are not inherited onto the annotated class. To resolve
them you must look one level up:

```java
static Omnissiah find(Class<?> type) {
    Omnissiah direct = type.getAnnotation(Omnissiah.class);
    if (direct != null) return direct;
    for (Annotation a : type.getAnnotations())
        if (a.annotationType().isAnnotationPresent(Omnissiah.class))
            return a.annotationType().getAnnotation(Omnissiah.class);
    return null;
}
```

That's exactly how Spring's `@Service`/`@Repository` "are" `@Component`s.

### 1.5 Generics and type tokens

Generics are compile-time only — at runtime, `List<String>` and `List<Integer>`
are the same class (*type erasure*). What survives is the `Class` object, and you
exploit that:

- **`Class<?>`** — "some unknown type." Use the wildcard `?` when handling types
  generically.
- **`Class<T>` as a type token.** A method `<T> T getBean(Class<T> type)` lets
  the caller's type flow through: `getBean(Service.class)` returns a `Service`
  with no cast at the call site. Internally you perform the checked cast with
  `type.cast(object)` (which throws a clear `ClassCastException` if wrong, rather
  than an unchecked-cast warning). This "pass the `Class` to recover the type"
  pattern is everywhere in container APIs.

### 1.6 Immutability and the Java Memory Model

**Immutable** = no observable state changes after construction (all fields
`final`, no mutating methods, no leaking of internal mutable collections).
Benefits: an immutable object is *automatically thread-safe* and can't be
corrupted by a caller. Forge makes `BeanDefinition`, `ToolMetadata`, value types,
etc. immutable, and returns `List.copyOf(...)` from every collection getter so
callers can't mutate internals (calling `add` throws
`UnsupportedOperationException`).

When state *must* be shared and mutated across threads, you need the **Java
Memory Model** guarantees. Without synchronization, one thread's writes may never
become visible to another. Tools you'll use:

- **`ConcurrentHashMap`** — safe concurrent map; `putIfAbsent` is an *atomic*
  "insert if absent," perfect for detecting duplicate registrations.
- **`CopyOnWriteArrayList`** — safe under concurrent iteration + append; good for
  small, append-mostly logs (like creation order).
- **`ReentrantLock`** — mutual exclusion the *same* thread can re-acquire. The
  resolver uses one so a thread building bean A can recurse into building A's
  dependencies while still holding the lock, while *other* threads wait.
- **`volatile`** — a write to a `volatile` field is immediately visible to other
  threads; used for a simple `closed` flag.

**Why the resolver serializes creation with a lock:** it guarantees each
singleton is constructed *exactly once* with no race, and it makes cycle
detection simple (the "in-creation" set is only touched by one thread at a time).
The cost — serialized construction — is irrelevant for a one-time startup.

### 1.7 Exceptions as design

- **Checked** (`extends Exception`): compiler forces handling. Use for
  *recoverable*, expected conditions the caller can act on.
- **Unchecked** (`extends RuntimeException`): no ceremony. Use for *programming
  errors* the caller can't sensibly recover from.

A misconfigured container (missing bean, dependency cycle, bad annotation) is a
programming error surfaced at startup — nothing to recover, everything to fix. So
Forge's exceptions are **all unchecked**, under one **abstract base**
(`ForgeException`) so a caller can `catch (ForgeException)` uniformly while still
catching a specific subtype when it matters. Two rules you'll follow:

1. **Wrap, preserving the cause.** When you catch a low-level checked exception
   (`IOException`, `URISyntaxException`, `ClassNotFoundException`), rethrow a
   semantic framework exception with the original as `cause` — never swallow it.
2. **Messages name the offending value.** `"Base package not found on classpath:
   com.typo"` beats `"not found"`. The message *is* the reason you threw instead
   of returning empty.

### 1.8 API design principles

The public surface is a promise you must keep. Principles Forge follows:

- **Small, stable public API; hide the mechanism.** `scan(String)` leaks nothing
  about `URL`s or classloaders, so you can rewrite its internals freely.
- **Never return `null` collections.** Return empty, immutable collections. The
  *only* place Forge returns `null` is a cache *miss* (`getSingleton`), which is
  a deliberate `Map.get`-style signal used internally.
- **Fail fast at construction.** Static factories (`BeanDefinition.of`,
  `LifecycleMetadata.forType`) validate, so invalid objects can't exist.
- **Name things for the reader.** Infrastructure is descriptive (`BeanRegistry`,
  `DependencyResolver`); the *theme* (`@Omnissiah`, `@Agent`) lives on the
  user-facing surface only. Theme for users; clarity for engineers.

---

## Part 2 — Dependency injection, from first principles

**The problem.** Objects need collaborators. If each object `new`s its own, you
get: hard-coded wiring scattered everywhere, no way to substitute a test double,
and duplicated construction logic. Changing how a `Repository` is built means
editing every `new Repository()`.

**The idea.** *Invert control*: let a container create objects and supply their
collaborators. Objects *declare* what they need; the container *provides* it.

**Why constructor injection** (and not field/setter injection):
- Dependencies are explicit in the signature — you can't forget one.
- Fields can be `final` — the object is immutable and fully-formed once built.
- The class is testable *without* the container: `new Service(fakeRepo)`.
- No reflection into private fields, no half-initialized objects.

Field injection (`@Inject private Repo repo;`) reads shorter but hides
dependencies, forbids `final`, and needs the container to construct a valid
object. Forge chooses constructor injection deliberately.

**The object graph.** Beans form a directed graph: an edge A→B means "A needs B."
To build A you must build B first. This is a **post-order traversal** of the
graph. If the graph has a **cycle** (A→B→A), constructor injection is impossible
(you can't build A without B and B without A) — so you detect it and fail loudly.

**Singletons.** Most beans should exist once and be shared. So the container
caches each built instance and returns the same one thereafter. That cache plus
the post-order build *is* the core of the container.

Everything in Part 3 is the mechanical realization of these four ideas:
declare → discover → order → build-once.

---

## Part 3 — Build it, lesson by lesson

Each lesson is buildable on its own. Set up a Maven module (`pom.xml` with
`<maven.compiler.release>21</maven.compiler.release>`, a test-scoped
`junit-jupiter` dependency), and write the tests alongside — *see* it work.

### Lesson 1 — The marker annotation

**Goal:** a `@Component`-style marker (`@Omnissiah`) the framework can find.

**Build:**
```java
@Documented
@Retention(RetentionPolicy.RUNTIME)   // MUST be RUNTIME (see 1.4)
@Target(ElementType.TYPE)             // classes/interfaces only
public @interface Omnissiah {
    String value() default "";        // optional explicit bean name
}
```

**Reason it through:** `RUNTIME` because the scanner reads it reflectively;
`TYPE` so putting it on a method is a compile error; `value()` named `value` so
`@Omnissiah("x")` shorthand works. Nothing else — an unused member you invent now
is a promise you'll regret (YAGNI). `value()` earns its place because the registry
will use it for naming.

**Test:** reflectively assert the annotation *itself* has `RUNTIME` retention and
`TYPE` target — this guards the two meta-annotations that the whole framework
silently depends on.

### Lesson 2 — The scanner

**Goal:** given a base package, return every `@Omnissiah` class under it.

**Concepts:** classloaders (1.2), reflection without initialization (1.3),
`java.nio.file` walking, `URL`→`Path` conversion.

**Design decisions, and why:**
- Return `List<Class<?>>` — *metadata, not instances* (Part 1.1).
- Validate the argument first: reject `null` (it would NPE in `replace`) and
  *blank* (an empty string maps to the classpath *root* and would scan
  everything). Do this *before* using the value.
- `getResource` returns a `URL`; convert with **`toURI()` then `Path.of`**, never
  `getFile()` — `getFile()` returns the percent-encoded form, so a path with a
  space becomes `.../My%20App/...` and silently doesn't exist.
- Guard the protocol: only `file`. A `jar` URL means someone's running from a
  packaged jar — out of scope, so throw a *clear* error rather than crash
  confusingly. (Deferring a feature is fine; deferring the *error message* is
  not.)
- Load with `Class.forName(name, false, loader)` — **`false` = don't initialize**
  (no side effects while scanning).
- Skip `package-info.class` / `module-info.class` (their names aren't valid class
  names).
- Reconstruct the fully-qualified name by **carrying the package name down the
  recursion** (`pkg + "." + dirName`), not by manipulating the filesystem path
  (which has OS-specific separators).
- Use **`Files.newDirectoryStream` in try-with-resources** — it holds an OS file
  handle; leaking one per directory eventually throws "too many open files."

**Skeleton to complete yourself:**
```java
public List<Class<?>> scan(String basePackage) {
    // 1. validate null / blank
    // 2. path = basePackage.replace('.', '/')
    // 3. URL url = classLoader.getResource(path); if null -> throw
    // 4. if (!"file".equals(url.getProtocol())) throw
    // 5. Path dir = Path.of(url.toURI());  // wrap URISyntaxException
    // 6. List<Class<?>> out = new ArrayList<>(); walk(dir, basePackage, out);
    // 7. return List.copyOf(out);
}
private void walk(Path dir, String pkg, List<Class<?>> out) {
    // try (var entries = Files.newDirectoryStream(dir)) {
    //   for each entry:
    //     isDirectory -> walk(entry, pkg + "." + name, out)
    //     else if name.endsWith(".class") && not package/module-info:
    //         Class<?> c = Class.forName(pkg + "." + stripExt(name), false, loader);
    //         if (c.isAnnotationPresent(Omnissiah.class)) out.add(c);
    // } catch IOException -> wrap
}
```

**Make it testable:** take the `ClassLoader` as a constructor parameter (default
to your own). Now a test can point it at a fixtures package on the test
classpath. Test: null/blank throw; missing package throws with the name; a
fixtures package returns exactly its annotated classes (including nested ones);
an all-plain package returns empty; the returned list is immutable.

**Pitfall:** if you forget `initialize=false`, tests that scan a fixture with a
`static {}` block will mysteriously run that block. If you forget to skip
`package-info`, a package with package-level annotations will throw
`ClassNotFoundException` on `com.example.package-info`.

### Lesson 3 — The bean definition

**Goal:** immutable, validated metadata per bean — type, name, the constructor to
inject through, and its parameter types.

**Key principle (1.0):** make illegal states unrepresentable. Private constructor,
static factories that validate. Once built, a definition is *guaranteed* valid.

**Decisions:**
- **Constructor selection:** exactly one constructor → use it. Several → require
  exactly one `@Inject`. Zero or many `@Inject` → throw. (This is *why* `@Inject`
  exists: to disambiguate.)
- **Instantiability:** reject interfaces, abstract classes, enums, annotations,
  arrays, primitives, non-static inner classes — anything you can't `new`.
- **Naming:** explicit `@Omnissiah("x")`, else decapitalize the simple name
  (`UserService`→`userService`), preserving acronyms (`URLParser` stays
  `URLParser`) via the rule "if the first two chars are uppercase, leave it." Do
  it by hand — don't drag in `java.beans.Introspector` (it's in the `java.desktop`
  module).
- Store `dependencyTypes = List.of(ctor.getParameterTypes())` — the wiring list.

**Test:** naming (default, explicit, acronym); dependency capture; `@Inject`
selection; every rejection (non-component, interface, abstract, multiple ctors,
double `@Inject`); dependency list immutable.

### Lesson 4 — The registry

**Goal:** store definitions by name; cache singletons; look up by name and type.

**Decisions:**
- `ConcurrentHashMap` for both maps (thread-safe). `putIfAbsent` to reject
  duplicate names atomically.
- `getDefinition(name)` throws `NoSuchBeanException` if absent — never returns
  null.
- `getDefinitionByType(type)` filters definitions whose bean type is *assignable*
  to `type` (so lookup by an implemented interface works). Zero → `NoSuchBean`;
  more than one → `NoUniqueBean` (no qualifiers in the MVP to disambiguate).
- Singleton cache: `registerSingleton`, `getSingleton` (returns `null` on miss —
  the deliberate exception), and record **creation order** (a
  `CopyOnWriteArrayList`) — you'll need it for lifecycle ordering.
- All bulk getters return `List.copyOf(...)`.

**Test:** register/get; duplicate name throws; unknown name throws; by-type unique
/ absent / ambiguous; singleton cache round-trip and miss; immutability.

### Lesson 5 — The resolver

**Goal:** turn definitions into wired singletons; detect cycles; build each once,
safely under concurrency. This is the heart.

**Concepts:** post-order graph traversal (Part 2), `ReentrantLock`, reflection
instantiation + `InvocationTargetException` unwrapping (1.3).

**Algorithm:**
```
resolve(name):
    if cached -> return it
    lock:
        return createSingleton(name)

createSingleton(name):
    if cached -> return it                       // re-check under lock
    def = registry.getDefinition(name)
    if !inCreation.add(name):                     // already building it?
        throw CircularDependency(path + " -> " + name)   // cycle!
    try:
        args = for each dependency TYPE:
                   depDef = registry.getDefinitionByType(type)
                   createSingleton(depDef.name)   // recurse: deps first
        instance = ctor.newInstance(args)         // unwrap InvocationTargetException
        registry.registerSingleton(name, instance)
        return instance
    finally:
        inCreation.remove(name)
```

**Why each piece:**
- The lock is *reentrant* so the recursion (building dependencies) re-enters
  freely on the same thread; other threads block until the whole graph is done →
  each singleton built exactly once, no race.
- `inCreation` (a `LinkedHashSet`, insertion-ordered) is the cycle detector: if
  you're asked to build something already on the current path, that's a cycle,
  and the set gives you the path to print (`a -> b -> a`).
- Dependencies are resolved *before* the constructor runs → creation order comes
  out dependency-first (needed by lifecycle).
- Unwrap `InvocationTargetException` so a failing constructor surfaces its real
  cause.

**Test:** simple bean; one dependency; transitive chain; shared singleton (two
dependents get the *same* instance); idempotent resolve; direct and indirect
cycles throw and name the path; missing dependency; throwing constructor
(cause preserved); and a concurrency test — N threads resolve the same bean and
must all receive the identical instance.

### Lesson 6 — Lifecycle

**Goal:** run `@PostConstruct` after a bean is wired, `@PreDestroy` on shutdown,
in the right order.

**Build `@PostConstruct` / `@PreDestroy`** (method-targeted, `RUNTIME`). Then a
`LifecycleMetadata.forType(class)` that finds and *validates* them: at most one
per phase, no parameters (else throw). Invoking unwraps
`InvocationTargetException`. No callbacks → a shared no-op instance (allocate
nothing in the common case).

**Ordering (the subtle part):** `@PostConstruct` runs in **creation order**
(dependencies first — a bean initializes after the things it depends on).
`@PreDestroy` runs in **reverse** (dependents die before their dependencies).
That's why the registry records creation order.

**Test:** callback invoked; validation failures (multiple, parameterized); a
throwing callback preserves its cause; no-callbacks is a safe no-op.

### Lesson 7 — The application context

**Goal:** the public façade — one call to bootstrap everything, plus lifecycle
ownership.

**Build `ApplicationContext.run(basePackage)`:**
1. new registry;
2. scan → for each class, `registry.registerDefinition(BeanDefinition.fromComponent(c))`;
3. new resolver;
4. **eagerly** resolve every definition (so misconfiguration fails at startup, not
   at first use — the server-framework default);
5. run `@PostConstruct` in creation order.

`getBean(Class<T>)` (find by type, resolve, `type.cast`), `getBean(String)`,
`getBeanDefinitionNames()`. Implement `AutoCloseable`: `close()` runs
`@PreDestroy` in reverse order and clears singletons; make it **idempotent** (a
`volatile boolean closed`); `getBean` after close throws.

**Why eager, why AutoCloseable:** eager instantiation turns a typo into a startup
crash with a clear message instead of a mysterious null later. `AutoCloseable`
lets `try (var ctx = ApplicationContext.run(...)) { ... }` guarantee
`@PreDestroy` runs.

**Test (integration):** a real `Controller → Service → Repository` graph — assert
wiring, by-type/by-name lookup, shared singletons, `@PostConstruct` order,
`@PreDestroy` reverse order on close, idempotent close, use-after-close throws.

At this point you have a complete DI container. Everything after is the AI layer,
built *on top* without touching the core.

### Lesson 8 — Tools

**Goal:** expose bean methods as named, invocable "tools."

**Build `@Tool`** (method-targeted; `name()` defaults to the method name;
`description()`). `ToolMetadata` (immutable: name, description, owning bean,
`Method`, parameter types) with `invoke(args...)` that unwraps
`InvocationTargetException` and also wraps an argument mismatch
(`IllegalArgumentException`) into a `ToolExecutionException`. `ToolRegistry` built
from a collection of beans (or, ergonomically, `fromContext` — pull each bean and
scan its declared methods for `@Tool`); unique tool names enforced; `invoke(name,
args...)`.

**Insight:** this reuses the exact reflection skills from the resolver. A tool is
just "a method we call reflectively, by name, on a managed bean."

### Lesson 9 — Tool schemas

**Goal:** describe each tool in a machine-readable form an LLM can consume.

**Concepts:** `-parameters` for real names (1.3); mapping Java types → JSON types.

**Build** `ToolParameter` (name + JSON type) and `ToolSchema` (name, description,
parameters) with `toInputSchema()` producing
`{"type":"object","properties":{...},"required":[...]}`. Map Java scalars to
`"string"`/`"integer"`/`"number"`/`"boolean"`, else `"object"`. Keep it
**provider-neutral** — emit plain maps; let a vendor adapter serialize them.

**Why this is the missing bridge:** tools were invocable in code, but an LLM
can't *choose* one without a description. This is what turns the tool registry
into something an agent can actually drive.

### Lesson 10 — Stereotypes and agents

**Goal:** make `@Agent` a first-class, discoverable component *category*.

**Concept:** meta-annotations (1.4). `@Agent` is meta-annotated with `@Omnissiah`,
so an `@Agent` class *is* a component — but remember `getAnnotation(Omnissiah)`
returns null on it, so you must resolve stereotypes explicitly.

**Build** a `Stereotypes` helper (`isComponent`, `findOmnissiah` — direct, then
one level of meta-annotation) and route the scanner's filter and the definition's
naming through it. Add `@Agent` and an `AgentRegistry` that picks `@Agent` beans
out of a context. Directly-annotated classes behave exactly as before (verify
with your existing tests — no regression).

**Restraint:** stop here. What an agent *does* — memory, conversation, an
execution loop — is the opinionated part. Design it against a *real* use case,
not speculatively (Part 4).

### Lesson 11 — The LLM seam

**Goal:** talk to a model without depending on any vendor.

**Build** a tiny `LlmClient` interface (`LlmResponse complete(LlmRequest)`),
immutable `LlmRequest`/`LlmResponse` value types, and an in-memory `EchoLlmClient`
(echo or canned reply) that records requests so tests can assert what was sent.
Document that real implementations throw `LlmException`.

**Why an interface + in-memory impl:** the interface is the *seam* (1.8) — core
stays vendor-free and offline-testable; the real Claude adapter is a separate
module you write later. Keep the interface minimal (single-turn) until an agent
needs more — designing streaming/tool-use now would be guessing.

---

## Part 4 — Building the next things yourself

You can now extend Forge alone. Sketches, not full designs — the point is you can
finish them.

### The real LLM adapter (`forge-llm-anthropic`)
A separate Maven module depending on `forge-core` and an HTTP client. Implement
`LlmClient.complete` by POSTing to the provider, mapping `LlmRequest` →
provider request and provider response → `LlmResponse`, wrapping transport/HTTP
errors in `LlmException`. Feed it the tool schemas from Lesson 9 as the
provider's tool definitions. Keep it *out* of core so core stays dependency-free.
Target the latest models; consult the provider's current API docs for the request
shape, tool-use protocol, and model IDs.

### The agent runtime (the opinionated core)
This is where you decide what Forge *is*. Questions to answer with a real app in
hand, not before:
- Does an agent own **conversation state / memory**? Where does it live, and what
  is its lifecycle relative to the bean?
- Is there an **execution loop**: send request → model asks for a tool → invoke
  it via `ToolRegistry` → feed the result back → repeat until done? (This is the
  standard agent loop; you have every piece to build it.)
- Is there an **`AgentRuntime`** per `@Agent` bean, binding its `LlmClient` +
  its tools + its memory?

Build the *loop* first against `EchoLlmClient` (deterministic), then swap in the
real adapter. Test the loop's control flow without a network.

### Missing infrastructure
- **Field/setter injection** — only if you truly need it; prefer constructor.
- **Bean scopes** beyond singleton (prototype, request) — add a `scope` to
  `BeanDefinition` and branch in the resolver.
- **Qualifiers** (`@Qualifier`, `@Primary`) — to resolve by-type ambiguity.
- **JAR scanning** — handle the `jar:` protocol in the scanner
  (`JarFile`/`FileSystems`), turning the current guard into a second strategy.
- **A prompt engine, a CLI** — later layers.

For each: write the issue/spec first (a Given/When/Then table), then TDD it.

---

## Part 5 — The engineering discipline

Being technically strong is as much *practice* as *knowledge*.

- **Test-first, and test failure modes.** For every feature, test the happy path
  *and* every way it can fail — nulls, blanks, cycles, ambiguity, wrapped causes,
  concurrency. A test that only covers success is half a test. (One Forge test
  caught the `InvocationTargetException` unwrapping bug — that's the payoff.)
- **Structure tests as a spec.** JUnit 5 `@Nested` groups + `@DisplayName`
  sentences make the report read like documentation.
- **Fixtures near the test.** Small purpose-built classes in a `fixtures`/`app`
  sub-package.
- **One responsibility per class.** If you can't describe a class in one
  sentence, split it. Scanner discovers; registry stores; resolver builds;
  context orchestrates.
- **Immutable, non-null, fail-fast** — the recurring trio.
- **Git/PR workflow.** Every feature: issue with a spec → branch off `main` →
  implement + tests → green build → PR that closes the issue → review → merge.
  **Branch off `main` for independent work; only *stack* branches when one truly
  depends on another — and then merge strictly bottom-up, letting the platform
  retarget each PR to `main` as the one below it lands.** (Merging a stack
  all-at-once lands the upper PRs in intermediate branches, not `main` — a real
  trap.)
- **CI.** A workflow that builds and tests on every push/PR, on the *target* JDK
  (not just your local one), so "works on my machine" never ships.
- **Reproducible builds.** Pin dependency and plugin versions centrally.
- **Semantic versioning & API stability.** Once something is public, changing it
  breaks users. Adding an annotation member is safe; removing one isn't. Think
  before you expose.

---

## Part 6 — Exercises

Do these without looking at the source; they build real muscle.

1. **From memory**, write `@Omnissiah` with the correct meta-annotations, and a
   test that reflectively proves the retention and target. Explain why each
   meta-annotation is load-bearing.
2. Implement `scan` for a package with **nested subpackages**. Then deliberately
   break it: remove `initialize=false` and add a fixture with a `static {}` that
   prints — watch the side effect. Restore it.
3. Implement the resolver's cycle detection. Write tests for a self-cycle
   (`A→A`) and a 3-node cycle (`A→B→C→A`); assert the message names the full
   path.
4. Write the concurrency test: 16 threads resolve one bean; assert they all get
   the same instance. Then remove the lock and see it (sometimes) fail.
5. Add **prototype scope**: a `scope` field on `BeanDefinition`, and a resolver
   branch that returns a *new* instance each time for prototypes. Test that a
   singleton is shared but a prototype is not.
6. Build the **agent loop** against `EchoLlmClient`: given a canned "call tool X
   with args Y" reply, have your loop invoke the tool via `ToolRegistry` and
   assert the tool ran. No network.
7. Add **`@Qualifier`** so two beans of the same type can be disambiguated by
   name at an injection point. (Hint: you'll need parameter annotations, which is
   why `@Inject`-style constructor param inspection matters.)

---

## Part 7 — How the giants do it

Study these to see your decisions mirrored (and where they diverge at scale):

- **Spring** — `@Component`/`@Service` stereotypes (meta-annotations, exactly like
  your `@Agent`), `BeanDefinition`, `BeanFactory`/`ApplicationContext`,
  `@PostConstruct`. Spring reads bytecode with **ASM** during scanning instead of
  loading classes — so unloadable classes never matter. That's the "right"
  end-state for a scanner; you deferred it deliberately.
- **Quarkus / Micronaut** — resolve injection at **build time** with annotation
  processors, for fast startup and native images. Contrast with Forge/Spring's
  runtime reflection; understand the trade-off (startup speed & native-image
  friendliness vs. runtime flexibility).
- **JSR-330 (`jakarta.inject`)** — the standard `@Inject`/`@Named`/`Provider`
  vocabulary. Read it; it will make your API choices feel familiar.
- **The Java Language Specification**, chapters on classes, initialization, and
  annotations — the ground truth for everything in Part 1.

---

*Companion documents: `docs/CODEBASE_GUIDE.md` explains the current code
class-by-class; `README.md` is the project overview. This masterclass teaches you
to build and extend it yourself — which is the whole point.*
