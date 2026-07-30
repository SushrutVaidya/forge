# Forge

> **An AI-native Java framework for building intelligent applications.**

Forge is an open-source Java framework that treats AI as a first-class runtime concern rather than another SDK or library.

Instead of forcing developers to manually wire prompts, tools, agents, and LLM providers into existing application frameworks, Forge aims to provide a unified runtime where AI components are discovered, managed, and executed as naturally as traditional Java components.

Forge is currently under active development.

---

# Vision

Traditional Java frameworks were designed around web applications, REST APIs, databases, and dependency injection.

Modern AI applications introduce entirely new runtime concerns:

- Agent discovery
- Prompt management
- Tool registration
- LLM abstraction
- Conversation lifecycle
- Context propagation
- Structured outputs
- AI observability

Today these concerns are typically implemented differently in every project.

Forge aims to standardize this layer.

Just as Spring simplified enterprise Java development, Forge aims to simplify AI-native application development.

---

# Long-Term Architecture

```text
CLI
 │
 ▼
Project Bootstrap
 │
 ▼
Component Scanner
 │
 ▼
Bean Registry
 │
 ▼
Dependency Injection
 │
 ▼
Application Context
 │
 ▼
Agent Registry
 │
 ▼
Tool Registry
 │
 ▼
Prompt Engine
 │
 ▼
LLM Provider Layer
 │
 ▼
AI Runtime
 │
 ▼
Developer Applications
```

Each layer builds upon the previous one.

---

# Design Principles

Forge is built around several core principles.

## AI First

AI is not another library.

It is part of the runtime.

---

## Modular

Every subsystem has a single responsibility.

Examples include:

- Scanner
- Bean Registry
- Dependency Injection
- Prompt Engine
- Tool Registry
- Runtime

---

## Convention Over Configuration

Developers should spend their time solving business problems—not wiring infrastructure.

---

## Provider Agnostic

Forge should work with multiple LLM providers through a common abstraction layer.

No application should depend directly on a specific AI vendor.

---

## Production Ready

Every architectural decision prioritizes:

- Maintainability
- Extensibility
- Testability
- Clean architecture

---

# Current Progress

The project is in its early development stages.

### Completed

- Multi-module Maven project
- Runtime annotation infrastructure
- Project architecture
- GitHub workflow

### In Progress

- Component Scanner

### Planned

- Bean Registry
- Dependency Injection
- Application Context
- Configuration System
- CLI
- Agent Runtime
- Tool Discovery
- Prompt Processing
- LLM Provider Abstraction
- AI Runtime

---

# Example (Future)

The long-term goal is to allow developers to write code like this:

```java
@Omnissiah
public class WeatherAgent {

    @Tool
    public Weather getWeather(String city) {
        ...
    }

    @Prompt
    public String systemPrompt() {
        return "...";
    }
}
```

…and let Forge automatically:

- Discover components
- Register tools
- Build prompt metadata
- Resolve dependencies
- Connect to configured LLM providers
- Execute the AI runtime

without additional boilerplate.

---

# Technology Stack

- Java 21
- Maven
- Reflection API
- Runtime Annotations
- JUnit (planned)

---

# Roadmap

- [x] Project Foundation
- [x] Runtime Annotation Infrastructure
- [ ] Component Scanner
- [ ] Bean Registry
- [ ] Dependency Injection
- [ ] Application Context
- [ ] Configuration System
- [ ] Forge CLI
- [ ] Agent Discovery
- [ ] Tool Registration
- [ ] Prompt Engine
- [ ] LLM Provider Abstraction
- [ ] AI Runtime

---

# Contributing

Forge is currently under active development.

As the project matures, contribution guidelines, documentation, and design proposals will be published.

---

# License

This project is licensed under the MIT License.
