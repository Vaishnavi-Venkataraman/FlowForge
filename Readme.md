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

## Project Evolution

### Commit 1 — Naive Monolithic Implementation
**State**: Single God class `WorkflowEngine` with everything.
**Problems**: HashMap data, if-else chains, inline logging, magic strings, no patterns.

### Commit 2 — Domain Models + Interfaces (SRP, OCP foundation)
**Changes**: `WorkflowDefinition`, `TaskConfig`, `TaskResult`, `Task` interface,
enums (`WorkflowStatus`, `TaskStatus`, `TriggerType`), exception hierarchy.
**Why**: Type safety, compile-time checks, meaningful domain language.

### Commit 3 — Command + Factory Patterns (Task System)
**Patterns**: Command, Factory Method + Registry.
**Changes**: `TaskFactory` with `registerTaskType()` replaces engine's switch statement.
**Why**: OCP — new task types without modifying engine.

### Commit 4 — Strategy Pattern (Execution Engine)
**Patterns**: Strategy.
**Changes**: `ExecutionStrategy` interface → `SequentialStrategy`, `ParallelStrategy`, `ConditionalStrategy`.
**Why**: Different workflows need different execution semantics.

### Commit 5 — Observer Pattern + Pub-Sub Event System
**Patterns**: Observer. **Architecture**: Pub-Sub / Event-Driven.
**Changes**: `EventBus`, `WorkflowEvent`, `LoggingListener`, `NotificationListener`, `MetricsListener`.
**Why**: Decouple cross-cutting concerns from orchestration logic.

### Commit 6 — Builder Pattern (Workflow Construction)
**Patterns**: Builder (fluent API).
**Changes**: `WorkflowBuilder` with fluent chainable methods and build-time validation.
**Why**: Readable, self-documenting workflow construction.

### Commit 7 — Template Method + Decorator Patterns
**Patterns**: Template Method, Decorator.
**Changes**: `AbstractTask` (validate→doExecute→cleanup lifecycle),
`RetryDecorator`, `LoggingDecorator`, `TimeoutDecorator`.
**Why Template Method**: All tasks share lifecycle; eliminates duplication.
**Why Decorator**: Add retry/logging/timeout without modifying task classes.

### Commit 8 — Chain of Responsibility + Pipes & Filters
**Patterns**: Chain of Responsibility. **Architecture**: Pipes & Filters.
**Changes**: `Pipeline`, `PipelineHandler`, `PipelineContext`,
`ValidationHandler`, `TransformHandler`, `AuthorizationHandler`, `RateLimitHandler`.
**Why**: Pre-execution concerns (validation, auth, rate limit, enrichment)
are independent handlers — not crammed into the engine.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     FlowForge Core                          │
├──────────┬───────────┬──────────┬──────────┬───────────────┤
│ Pipeline │  Engine   │  Event   │  Task    │   Plugin      │
│ (CoR)    │  Layer    │  System  │  System  │   System      │
│          │           │          │          │               │
│ Validate │ Strategy  │ EventBus │ Factory  │  Microkernel  │
│ Auth     │ Seq/Par   │ Pub-Sub  │ Command  │  (Commit 9)   │
│ RateLimit│ Cond.     │ Observer │ Template │               │
│ Transform│           │          │ Decorator│               │
├──────────┴───────────┴──────────┴──────────┴───────────────┤
│                   Domain Models & Events                    │
└─────────────────────────────────────────────────────────────┘
```

### Modular Monolith → Future Microservices

This project implements a **modular monolith** structured so each package
could become an independent microservice:

| Package | Would Become | Communication |
|---------|-------------|---------------|
| `engine` | Execution Service | EventBus → Message Broker |
| `event` | Event Bus Service | Kafka / RabbitMQ |
| `task` | Task Worker Service | Queue-based |
| `pipeline` | Gateway / Middleware | API Gateway filters |
| `plugin` | Plugin Registry Service | Service mesh |

### Design Patterns

| Category | Pattern | Class(es) | Why |
|----------|---------|-----------|-----|
| Creational | Factory Method | `TaskFactory` | Decouple task creation |
| Creational | Builder | `WorkflowBuilder` | Readable construction |
| Behavioral | Command | `Task` interface | Encapsulate execution |
| Behavioral | Strategy | `ExecutionStrategy` | Pluggable execution |
| Behavioral | Observer | `EventListener`, `EventBus` | Reactive events |
| Behavioral | Template Method | `AbstractTask` | Standardize lifecycle |
| Behavioral | Chain of Responsibility | `PipelineHandler`, `Pipeline` | Flexible pre-processing |
| Structural | Decorator | `RetryDecorator`, `LoggingDecorator`, `TimeoutDecorator` | Dynamic enhancement |

### Architectural Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Pub-Sub / Event-Driven | `EventBus` + listeners | Decouple concerns |
| Pipes & Filters | `Pipeline` + handlers | Modular pre-processing |
| Modular Monolith | Package structure | Clean separation |

---

## Package Structure

```
com.flowforge/
├── Main.java
├── engine/
│   ├── WorkflowEngine.java
│   ├── WorkflowBuilder.java
│   └── strategy/
│       ├── ExecutionStrategy.java
│       ├── SequentialStrategy.java
│       ├── ParallelStrategy.java
│       └── ConditionalStrategy.java
├── event/
│   ├── WorkflowEvent.java
│   ├── EventListener.java
│   ├── EventBus.java
│   ├── LoggingListener.java
│   ├── NotificationListener.java
│   └── MetricsListener.java
├── model/
│   ├── WorkflowDefinition.java
│   ├── WorkflowStatus.java
│   ├── TaskConfig.java
│   ├── TaskResult.java
│   ├── TaskStatus.java
│   ├── TriggerConfig.java
│   └── TriggerType.java
├── pipeline/
│   ├── Pipeline.java
│   ├── PipelineContext.java
│   ├── PipelineHandler.java
│   ├── ValidationHandler.java
│   ├── TransformHandler.java
│   ├── AuthorizationHandler.java
│   └── RateLimitHandler.java
├── task/
│   ├── Task.java
│   ├── AbstractTask.java
│   ├── TaskFactory.java
│   ├── HttpTask.java
│   ├── EmailTask.java
│   ├── TransformTask.java
│   ├── DatabaseTask.java
│   ├── DelayTask.java
│   └── decorator/
│       ├── TaskDecorator.java
│       ├── RetryDecorator.java
│       ├── LoggingDecorator.java
│       └── TimeoutDecorator.java
└── exception/
    ├── FlowForgeException.java
    ├── WorkflowNotFoundException.java
    └── TaskExecutionException.java
```