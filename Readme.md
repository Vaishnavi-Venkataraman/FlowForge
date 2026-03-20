# FlowForge — Intelligent Workflow Automation Platform

A Java 21 Zapier-lite workflow automation engine with a **web dashboard**,
demonstrating **12 design patterns**, **4 architectural patterns**,
progressive **refactoring**, and **static code analysis**.

---

## Quick Start

```bash
mvn clean compile
mvn exec:java
```

Open **http://localhost:8080** — Sign up, create workflows, run them, view logs.
Data persists in `data/` folder across restarts.

See **USER_MANUAL.md** for detailed examples of every workflow type.

---

## Commit History

| # | Commit | Patterns |
|---|--------|----------|
| 1 | Naive monolithic God class | — |
| 2 | Domain models + interfaces | SRP, OCP |
| 3 | Task system | Command, Factory |
| 4 | Execution strategies | Strategy |
| 5 | Event system | Observer, Pub-Sub |
| 6 | Workflow construction | Builder |
| 7 | Task lifecycle + enhancement | Template Method, Decorator |
| 8 | Pre-execution pipeline | Chain of Responsibility, Pipes & Filters |
| 9 | Plugin system | Microkernel |
| 10 | External integrations | Adapter |
| 11 | Service boundaries | Microservices |
| 12 | Cleanup redundancy | — |
| 13 | Simplified API | Singleton, Facade |
| 14 | SOLID refactoring | Extract Method, Encapsulation, Thread Safety |
| 15 | Web UI + persistence | File-based storage, REST API, SPA |

---

## Design Patterns (12)

| # | Pattern | Class(es) | Why |
|---|---------|-----------|-----|
| 1 | Factory Method | `TaskFactory` | Decouple task creation |
| 2 | Builder | `WorkflowBuilder` | Complex construction |
| 3 | Singleton | `FlowForgeFacade` | One engine per JVM |
| 4 | Command | `Task` | Encapsulate execution |
| 5 | Strategy | `ExecutionStrategy` | Pluggable algorithms |
| 6 | Observer | `EventBus` | Reactive events |
| 7 | Template Method | `AbstractTask` | Standard lifecycle |
| 8 | Chain of Responsibility | `PipelineHandler` | Pre-processing |
| 9 | Decorator | `RetryDecorator`, `LoggingDecorator`, `TimeoutDecorator` | Dynamic enhancement |
| 10 | Adapter | `RestServiceAdapter`, `SoapServiceAdapter`, `CloudStorageAdapter` | Unified external API |
| 11 | Facade | `FlowForgeFacade` | Hide complexity |

## Architectural Patterns (4)

| # | Pattern | Where |
|---|---------|-------|
| 1 | Pub-Sub | `EventBus` + listeners |
| 2 | Pipes & Filters | `Pipeline` + handlers |
| 3 | Microkernel | `Plugin` system |
| 4 | Microservices | `ServiceBus` + 5 services |

---

## Web UI Features

- Sign Up / Sign In with password validation (6+ chars, uppercase, number, symbol)
- Dark mode toggle (persisted)
- Dashboard with stats (workflows, executions, success rate)
- Visual workflow builder with type-specific parameter fields
- Task types: http, email, transform, database, delay, ftp, file_copy, file_delete, external_service
- Execution history with expandable color-coded logs
- File-based persistence — data survives restarts

---

## Architecture

```
Browser (SPA) ──HTTP──> WebServer ──> AuthHandler / WorkflowApiHandler
                                          │
                                    FlowForgeFacade (Singleton)
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    │                     │                      │
               TriggerService      ExecutionService        NotificationService
                    │              (WorkflowEngine)        AnalyticsService
                    │                     │                AuditService
                    └──── ServiceBus ─────┘
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    │                     │                      │
                Pipeline            TaskFactory              EventBus
              (Validation       (Command+Factory)          (Observer)
               Auth, Rate         │       │
               Transform)    Decorators  Strategies
                              (Retry     (Sequential
                               Log        Parallel
                               Timeout)   Conditional)
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    │                     │                      │
              PluginRegistry       ServiceRegistry         Domain Models
              (FileOpsPlugin)    (REST, SOAP, S3          (WorkflowDefinition
                                  Adapters)                TaskConfig, etc.)
```

---
