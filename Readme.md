# FlowForge — Intelligent Workflow Automation Platform

A Java-based workflow automation engine (inspired by Zapier/n8n) built to
demonstrate **software design patterns**, **architectural patterns**,
**refactoring techniques**, and **static code analysis** through an
evolving Git history.

---

## Building & Running

```bash
mvn clean compile
mvn exec:java
```

Requires: Java 21+, Maven 3.8+

---

## Project Evolution (Git Commit History)

Each commit introduces specific improvements with clear justifications.

### Commit 1 — Naive Monolithic Implementation
**State**: Single God class `WorkflowEngine` with everything.
**Problems**: HashMap data, if-else chains, inline logging, magic strings, no patterns.

### Commit 2 — Domain Models + Interfaces (SRP, OCP foundation)
**Patterns**: None yet — foundational extraction.
**Changes**:
- `WorkflowDefinition`, `TaskConfig`, `TaskResult` replace HashMaps
- `WorkflowStatus`, `TaskStatus`, `TriggerType` enums replace magic strings
- `Task` interface replaces if-else chain
- Exception hierarchy (`FlowForgeException`, `WorkflowNotFoundException`, `TaskExecutionException`)
**Why**: Type safety, compile-time checks, meaningful domain language.

### Commit 3 — Command + Factory Patterns (Task System)
**Patterns**: Command, Factory Method + Registry.
**Changes**:
- `TaskFactory` with `registerTaskType()` replaces engine's switch statement
- Engine depends only on `TaskFactory`, not concrete task classes
- Custom task types registered at runtime (e.g., FTP task)
**Why**: Open/Closed Principle — new task types without modifying engine.

### Commit 4 — Strategy Pattern (Execution Engine)
**Patterns**: Strategy.
**Changes**:
- `ExecutionStrategy` interface with `execute(tasks, factory)`
- `SequentialStrategy` — fail-fast linear execution
- `ParallelStrategy` — concurrent via thread pool + CountDownLatch
- `ConditionalStrategy` — predicate-based task filtering
- `WorkflowDefinition` declares preferred strategy by name
**Why**: Different workflows need different execution semantics. ETL = sequential,
notifications = parallel. Strategy makes execution model pluggable.

### Commit 5 — Observer Pattern + Pub-Sub Event System
**Patterns**: Observer. **Architecture**: Pub-Sub / Event-Driven.
**Changes**:
- `EventBus` with typed and wildcard subscription
- `WorkflowEvent` domain events with factory methods
- `LoggingListener` — replaces inline `log()` calls
- `NotificationListener` — replaces inline `NOTIFICATION:` prints
- `MetricsListener` — replaces `printStats()` with reactive counters
- Engine has ZERO `System.out.println` — only publishes events
**Why**: Cross-cutting concerns (logging, alerting, metrics) don't belong in
orchestration logic. Pub-Sub decouples publishers from consumers.

### Commit 6 — Builder Pattern (Workflow Construction)
**Patterns**: Builder (fluent API).
**Changes**:
- `WorkflowBuilder` with fluent chainable methods
- Convenience methods: `addHttpGet()`, `addEmailTask()`, `cronTrigger()`, etc.
- Validation at `build()` time with aggregated error messages
**Why**: WorkflowDefinition construction was verbose and error-prone
(nested constructors, positional params, inline `Map.of()` calls).
Builder provides readable, self-documenting construction.

---

## Architecture

### What We Implement

```
┌─────────────────────────────────────────────────────┐
│                    FlowForge Core                    │
├──────────┬──────────┬──────────┬────────────────────┤
│  Engine  │  Event   │  Task    │  Pipeline          │
│  Layer   │  System  │  System  │  Processing        │
│          │          │          │                    │
│ Strategy │ EventBus │ Factory  │ Chain of           │
│ Pattern  │ Pub-Sub  │ Command  │ Responsibility     │
├──────────┴──────────┴──────────┴────────────────────┤
│              Plugin System (Microkernel)             │
├─────────────────────────────────────────────────────┤
│              Domain Models & Events                  │
└─────────────────────────────────────────────────────┘
```

### What About Microservices?

This project implements a **modular monolith** — the code is structured
with the same separation of concerns that microservices would have:

| Package | Would Become Microservice |
|---------|--------------------------|
| `engine` | Execution Service |
| `event` | Event Bus / Message Broker |
| `task` | Task Worker Service |
| `trigger` | Trigger/Scheduler Service |
| `plugin` | Plugin Registry Service |

We don't implement actual microservices (Docker, REST APIs, service
discovery) because that's an infrastructure concern, not a design
patterns concern. The modular architecture we build IS the foundation
that enables microservice extraction later — which is the correct
approach (monolith-first, extract when needed).

### Architectural Patterns Implemented

| Pattern | Where | Why |
|---------|-------|-----|
| **Pub-Sub / Event-Driven** | `EventBus` + listeners | Decouple cross-cutting concerns |
| **Pipes & Filters** | Pipeline (Commit 8) | Modular data processing |
| **Microkernel** | Plugin system (Commit 9) | Extensible platform |
| **Modular Monolith** | Package structure | Clean separation, future extraction |

### Design Patterns Implemented

| Category | Pattern | Where | Why |
|----------|---------|-------|-----|
| Creational | **Factory Method** | `TaskFactory` | Decouple task creation from engine |
| Creational | **Builder** | `WorkflowBuilder` | Readable complex object construction |
| Behavioral | **Command** | `Task` interface | Encapsulate execution as objects |
| Behavioral | **Strategy** | `ExecutionStrategy` | Pluggable execution algorithms |
| Behavioral | **Observer** | `EventListener` + `EventBus` | Reactive event handling |

---

## Package Structure

```
com.flowforge/
├── Main.java                          # Entry point & demo
├── engine/
│   ├── WorkflowEngine.java           # Core orchestrator
│   ├── WorkflowBuilder.java          # Builder pattern
│   └── strategy/
│       ├── ExecutionStrategy.java     # Strategy interface
│       ├── SequentialStrategy.java    # Sequential execution
│       ├── ParallelStrategy.java      # Parallel execution
│       └── ConditionalStrategy.java   # Conditional execution
├── event/
│   ├── WorkflowEvent.java            # Domain event
│   ├── EventListener.java            # Observer interface
│   ├── EventBus.java                 # Pub-Sub backbone
│   ├── LoggingListener.java          # Log subscriber
│   ├── NotificationListener.java     # Alert subscriber
│   └── MetricsListener.java          # Metrics subscriber
├── model/
│   ├── WorkflowDefinition.java       # Workflow domain model
│   ├── WorkflowStatus.java           # Workflow lifecycle enum
│   ├── TaskConfig.java               # Task configuration
│   ├── TaskResult.java               # Task execution result
│   ├── TaskStatus.java               # Task lifecycle enum
│   ├── TriggerConfig.java            # Trigger configuration
│   └── TriggerType.java              # Trigger type enum
├── task/
│   ├── Task.java                     # Command interface
│   ├── TaskFactory.java              # Factory + Registry
│   ├── HttpTask.java                 # HTTP task
│   ├── EmailTask.java                # Email task
│   ├── TransformTask.java            # Transform task
│   ├── DatabaseTask.java             # Database task
│   └── DelayTask.java                # Delay task
└── exception/
    ├── FlowForgeException.java        # Base exception
    ├── WorkflowNotFoundException.java # Lookup failure
    └── TaskExecutionException.java    # Execution failure
```