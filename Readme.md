# FlowForge — Intelligent Workflow Automation Platform

A Java 21 Zapier-lite workflow automation engine with a **web dashboard**,
demonstrating **11 design patterns**, **5 architectural patterns**,
progressive **refactoring**, and **static code analysis** via SonarCloud.

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

## Design Patterns

| # | Pattern | Class(es) | Purpose |
|---|---------|-----------|---------|
| 1 | **Factory Method** | `TaskFactory` | Creates task instances by type name at runtime without hardcoding constructors |
| 2 | **Builder** | `WorkflowBuilder` | Fluent step-by-step construction of complex `WorkflowDefinition` objects |
| 3 | **Singleton** | `FlowForgeFacade` (Holder idiom) | One engine instance per JVM — thread-safe lazy initialization |
| 4 | **Command** | `Task` interface | Encapsulates workflow actions as objects with uniform `execute()` contract |
| 5 | **Strategy** | `ExecutionStrategy` → `SequentialStrategy`, `ParallelStrategy`, `ConditionalStrategy` | Pluggable execution algorithms — switch between sequential, parallel, or conditional at runtime |
| 6 | **Observer** | `EventBus`, `EventListener`, `LoggingListener` | Reactive event notification — decouple workflow engine from logging/monitoring |
| 7 | **Template Method** | `AbstractTask` → `HttpTask`, `EmailTask`, `DatabaseTask`, etc. | Fixed lifecycle (validate → doExecute → cleanup) with customizable steps |
| 8 | **Chain of Responsibility** | `PipelineHandler` → `ValidationHandler`, `AuthorizationHandler`, `RateLimitHandler`, `TransformHandler` | Pre-execution pipeline where each handler decides to process or abort |
| 9 | **Decorator** | `TaskDecorator` → `RetryDecorator`, `LoggingDecorator`, `TimeoutDecorator` | Dynamically wrap tasks with retry, logging, timeout without modifying task code |
| 10 | **Adapter** | `ExternalService` → `RestServiceAdapter`, `SoapServiceAdapter`, `CloudStorageAdapter` | Unified interface for REST, SOAP, and cloud storage — protocol differences hidden |
| 11 | **Facade** | `FlowForgeFacade` | Single simplified API hiding 10+ subsystems (engine, services, plugins, pipeline) |

## Architectural Patterns

| # | Pattern | Where | How |
|---|---------|-------|-----|
| 1 | **Event-Driven** | `EventBus` + `WorkflowEvent` + listeners | Workflow lifecycle events published and consumed reactively |
| 2 | **Pipes & Filters** | `Pipeline` + `PipelineHandler` chain | Request flows through validation → auth → rate-limit → transform filters |
| 3 | **Microkernel** | `Plugin` + `PluginContext` + `PluginRegistry` | Core is minimal; features added via plugins (FileOperationsPlugin registers ftp/file_copy/file_delete tasks) |
| 4 | **Microservices** | `ServiceBus` + `ExecutionService`, `NotificationService`, `AnalyticsService`, `AuditService`, `TriggerService` | Five independent services communicate via message bus — simulates distributed architecture in-process |
| 5 | **Pub-Sub** | `ServiceBus`, `ServiceMessage` | Asynchronous inter-service messaging — services communicate via named channels without direct coupling |

---

## Commit History

| # | Commit | What Changed |
|---|--------|-------------|
| 1 | Naive monolithic God class | Intentionally bad starting point |
| 2 | Domain models + interfaces | SRP, OCP — clean separation |
| 3 | Task system | Command + Factory patterns |
| 4 | Execution strategies | Strategy pattern |
| 5 | Event system | Observer + Pub-Sub |
| 6 | Workflow construction | Builder pattern |
| 7 | Task lifecycle + wrappers | Template Method + Decorator |
| 8 | Pre-execution pipeline | Chain of Responsibility + Pipes & Filters |
| 9 | Plugin system | Microkernel architecture |
| 10 | External integrations | Adapter pattern |
| 11 | Service boundaries | Microservices architecture |
| 12 | Cleanup redundancy | Removed 4 redundant files, re-wired 5 |
| 13 | Simplified API | Singleton + Facade |
| 14 | SOLID refactoring | Guard clauses, thread safety, Extract Method |
| 15 | Web UI + persistence | HTTP server, SPA, file-based storage, dark mode |
| 16 | Static analysis config | Checkstyle, PMD, SonarCloud |

---

## Web UI Features

- **Sign Up / Sign In** with password validation (6+ chars, uppercase, number, symbol)
- **Dark mode** toggle (persisted across sessions)
- **Dashboard** with stats (workflows, executions, success rate)
- **Workflow builder** with 9 task types and type-specific parameter fields
- **Execution history** with expandable color-coded logs (blue=pipeline, green=success, red=error, orange=warning)
- **File-based persistence** — users, workflows, and execution history survive restarts

### Task Types Available in UI

| Type | Description | Parameters |
|------|-------------|------------|
| `http` | HTTP request | url, method |
| `email` | Send email | to, subject |
| `transform` | Data transformation | input, operation |
| `database` | SQL query | query |
| `delay` | Wait N seconds | seconds |
| `ftp` | FTP transfer | host, file |
| `file_copy` | Copy file | source, destination |
| `file_delete` | Delete file | path |
| `external_service` | Call REST/SOAP/S3 adapter | serviceId, operation, path |

---

## Architecture

```
Browser (SPA) ──HTTP──> WebServer ──> AuthHandler / WorkflowApiHandler
                                          │
                                    FlowForgeFacade (Singleton + Facade)
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    │                     │                      │
               TriggerService      ExecutionService        NotificationService
                    │              (WorkflowEngine)        AnalyticsService
                    │                     │                AuditService
                    └──── ServiceBus ─────┘
                              (Pub-Sub / Microservices)
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    │                     │                      │
                Pipeline            TaskFactory              EventBus
              (Chain of Resp.     (Factory+Command)         (Observer)
               Pipes&Filters)       │       │
                                Decorators  Strategies
                                (Decorator) (Strategy)
                                    │
                              AbstractTask
                            (Template Method)
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
              PluginRegistry   ServiceRegistry   Domain Models
              (Microkernel)    (Adapter:         (WorkflowDefinition
               FileOpsPlugin    REST/SOAP/S3)     TaskConfig, etc.)
```
---