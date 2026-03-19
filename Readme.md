# FlowForge — Intelligent Workflow Automation Platform

A Java 21 Zapier-lite workflow automation engine demonstrating
**12 design patterns**, **4 architectural patterns**, progressive
**refactoring**, and **static code analysis** through an evolving Git history.

---

## Building & Running

```bash
mvn clean compile
mvn exec:java
```

Requires: Java 21+, Maven 3.8+

---

## What It Does

FlowForge lets you define multi-step workflows with triggers, execute them
sequentially or in parallel, integrate with external services (REST, SOAP,
Cloud Storage), and monitor everything through notifications, analytics,
and audit trails - like a simplified Zapier running on localhost.

---

## Design Patterns (12)

| # | Pattern | Type | Class | Why Needed |
|---|---------|------|-------|------------|
| 1 | Factory Method | Creational | `TaskFactory` | Decouple task creation from engine |
| 2 | Builder | Creational | `WorkflowBuilder` | Complex object construction |
| 3 | Singleton | Creational | `FlowForgeFacade` | One engine instance per JVM |
| 4 | Command | Behavioral | `Task` | Encapsulate task execution |
| 5 | Strategy | Behavioral | `ExecutionStrategy` | Pluggable execution algorithms |
| 6 | Observer | Behavioral | `EventBus` + `EventListener` | Decouple event handling |
| 7 | Template Method | Behavioral | `AbstractTask` | Standard task lifecycle |
| 8 | Chain of Responsibility | Behavioral | `PipelineHandler` | Flexible pre-processing |
| 9 | Decorator | Structural | `RetryDecorator`, `LoggingDecorator`, `TimeoutDecorator` | Dynamic task enhancement |
| 10 | Adapter | Structural | `RestServiceAdapter`, `SoapServiceAdapter`, `CloudStorageAdapter` | Unified external API |
| 11 | Facade | Structural | `FlowForgeFacade` | Hide subsystem complexity |

## Architectural Patterns (4)

| # | Pattern | Where | Why |
|---|---------|-------|-----|
| 1 | Pub-Sub / Event-Driven | `EventBus` (engine internal) | Decouple cross-cutting concerns |
| 2 | Pipes & Filters | `Pipeline` + handlers | Modular pre-processing |
| 3 | Microkernel | `Plugin` system | Extensible without core changes |
| 4 | Microservices | `ServiceBus` + 5 services | Independent service boundaries |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    FlowForgeFacade                        │
│              (Singleton + Facade entry point)             │
├──────────┬───────────┬──────────┬───────────┬───────────┤
│ Pipeline │  Engine   │  Event   │  Plugin   │  Adapter  │
│          │           │  System  │  System   │  Layer    │
│ Validate │ Sequential│          │           │           │
│ Authorize│ Parallel  │ EventBus │ Plugin    │ REST      │
│ RateLimit│ Condition.│ Listener │ Registry  │ SOAP      │
│ Transform│           │          │ FileOps   │ Cloud S3  │
├──────────┴───────────┴──────────┴───────────┴───────────┤
│              Microservices Layer (ServiceBus)             │
│  Trigger │ Execution │ Notification │ Analytics │ Audit  │
├──────────────────────────────────────────────────────────┤
│     Domain Models  │  Tasks + Decorators  │  Exceptions  │
└──────────────────────────────────────────────────────────┘
```

### Two Bus Systems (different layers)

| Bus | Layer | Message Type | Purpose |
|-----|-------|-------------|---------|
| `EventBus` | Engine internal | `WorkflowEvent` | Typed domain events (task completed, workflow failed) |
| `ServiceBus` | Inter-service | `ServiceMessage` | Cross-service communication (like Kafka) |

### Logging at Two Levels (different granularity)

| Logger | Level | What It Logs |
|--------|-------|-------------|
| `LoggingListener` | Workflow | WORKFLOW_STARTED, TASK_COMPLETED, WORKFLOW_FAILED |
| `LoggingDecorator` | Task | Per-task entry/exit with duration |

---