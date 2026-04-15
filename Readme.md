# FlowForge — Intelligent Workflow Automation Platform

A Java 21 Zapier-lite workflow automation engine with a React 18 web dashboard, demonstrating **design patterns**, **architectural patterns**, **frameworks**, **SOLID principles**, progressive **refactoring**, and **static code analysis** via SonarCloud.

## Quick Start

```bash
mvn clean compile
mvn exec:java
```

Open **http://localhost:8080** — Sign up, create workflows, run them, view execution logs.
Data persists in `data/` folder across restarts. See **USER_MANUAL.md** for example workflows.

## Frameworks & Technologies

| Framework | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Core language |
| Maven | 3.9+ | Build tool |
| React | 18 | Frontend UI (CDN, component-based SPA) |
| Gson | 2.11.0 | JSON serialization/deserialization |
| jBCrypt | 0.4 | Password hashing (industry-standard) |
| JUnit 5 | 5.11.3 | Unit testing framework |
| Mockito | 5.14.2 | Mocking framework for tests |
| JaCoCo | 0.8.12 | Code coverage reporting |
| Checkstyle | 10.20.1 | Coding standards enforcement |
| PMD | 7.7.0 | Code quality analysis |
| SonarCloud | — | Comprehensive static analysis dashboard |

## Design Patterns

| # | Pattern | Category | Class(es) | Purpose |
|---|---------|----------|-----------|---------|
| 1 | Factory Method | Creational | `TaskFactory` | Creates task instances by type name at runtime |
| 2 | Builder | Creational | `WorkflowBuilder` | Fluent construction of complex WorkflowDefinition |
| 3 | Singleton | Creational | `FlowForgeFacade` (Holder idiom) | One engine instance per JVM, thread-safe |
| 4 | Command | Behavioral | `Task` interface | Encapsulates actions as objects with `execute()` |
| 5 | Strategy | Behavioral | `ExecutionStrategy` → Sequential, Parallel, Conditional | Pluggable execution algorithms |
| 6 | Observer | Behavioral | `EventBus` + `EventListener` + `LoggingListener` | Reactive event notification |
| 7 | Template Method | Behavioral | `AbstractTask` → HttpTask, EmailTask, etc. | Fixed lifecycle: validate → doExecute → cleanup |
| 8 | Chain of Responsibility | Behavioral | `PipelineHandler` → Validation, Auth, RateLimit, Transform | Pre-execution handler pipeline |
| 9 | Decorator | Structural | `TaskDecorator` → Retry, Logging, Timeout | Dynamic task enhancement without modifying code |
| 10 | Adapter | Structural | `ExternalService` → REST, SOAP, CloudStorage adapters | Unified interface for different protocols |
| 11 | Facade | Structural | `FlowForgeFacade` | Single simplified API hiding 10+ subsystems |

## Architectural Patterns

| # | Pattern | Implementation | How It Works |
|---|---------|----------------|-------------|
| 1 | Event-Driven | `EventBus` + `WorkflowEvent` | Workflow lifecycle events published and consumed reactively |
| 2 | Pipes & Filters | `Pipeline` + `PipelineHandler` chain | Request flows through Validation → Auth → RateLimit → Transform |
| 3 | Microkernel | `Plugin` + `PluginContext` + `PluginRegistry` | Core is minimal; features added via plugins (FileOperationsPlugin) |
| 4 | Microservices | `ServiceBus` + 5 services | Independent services communicate via message bus channels |
| 5 | Pub-Sub | `ServiceBus` + `ServiceMessage` | Asynchronous inter-service messaging |

## Design Principles

| Principle | Where Applied |
|-----------|--------------|
| Single Responsibility | Each class has one job: TaskFactory creates, Pipeline chains, EventBus distributes |
| Open/Closed | New task types via `registerTaskType()`, new strategies, plugins, adapters — all additive |
| Liskov Substitution | All ExecutionStrategy and Task implementations are interchangeable |
| Interface Segregation | Focused interfaces: Task, ExecutionStrategy, PipelineHandler, Plugin, ExternalService |
| Dependency Inversion | Engine depends on Strategy interface, not concrete. Task depends on ExternalService interface, not REST/SOAP |
| DRY | WorkflowBuildHelper eliminates duplicate workflow construction logic |

## Refactoring

### Code Refactoring

| Technique | Where | Change |
|-----------|-------|--------|
| Extract Method | WorkflowEngine | 1 giant method → findWorkflow + runPipeline + runWithStrategy + publishTaskResults |
| Encapsulate Field | WorkflowDefinition | Public setStatus → guarded markRunning/markCompleted/markFailed |
| Guard Clause | WorkflowDefinition | Nested if/else → early-return with IllegalStateException |
| Thread Safety | WorkflowEngine, TaskFactory | HashMap → ConcurrentHashMap, volatile → AtomicReference |

### Design Refactoring

| Change | What |
|--------|------|
| Remove Redundancy | Deleted 4 files replaced by microservices |
| Introduce Facade | Main.java reduced from 206 → 60 lines |
| Introduce Singleton | Holder-idiom ensures one engine per JVM |

### Framework Refactoring

| Change | What |
|--------|------|
| Hand-written JSON → Gson | Eliminated cognitive complexity 32 |
| SHA-256 → BCrypt | Industry-standard adaptive password hashing |
| System.out → java.util.logging | ~90 occurrences migrated to proper logging |
| Vanilla JS → React 18 | Component-based architecture with hooks |

## Static Code Analysis

| Metric | Before Refactoring | After Refactoring |
|--------|-------------------|------------------|
| Bugs | 2 (BLOCKER) | 0 |
| Vulnerabilities | 0 | 0 |
| Code Smells | 174 | ~3 (Minor/Info) |
| Security Hotspots | 5 | 0 (reviewed as Safe) |
| Duplications | 4.6% | 1.8% |
| Unit Tests | 0 | 130+ (all passing) |
| Coverage | 0% | 52% line / 39% branch |

## Web UI Features

- Sign Up / Sign In with BCrypt password hashing and validation
- Dark mode toggle (persisted across sessions)
- Dashboard with stats (workflows, executions, success rate)
- Workflow builder with 9 task types and type-specific parameter fields
- Execution history with expandable color-coded logs
- File-based persistence — survives restarts
- React 18 component architecture with hooks

### Task Types

| Type | Parameters |
|------|-----------|
| http | url, method |
| email | to, subject |
| transform | input, operation |
| database | query |
| delay | seconds |
| ftp | host, file |
| file_copy | source, destination |
| file_delete | path |
| external_service | serviceId, operation, path |

## Architecture

```
Browser (React SPA) ──HTTP──> WebServer ──> AuthHandler / WorkflowApiHandler
                                                │
                                          FlowForgeFacade (Singleton + Facade)
                                                │
                      ┌─────────────────────────┼─────────────────────────┐
                      │                         │                         │
                 TriggerService          ExecutionService          NotificationService
                      │                 (WorkflowEngine)          AnalyticsService
                      │                         │                 AuditService
                      └────── ServiceBus ───────┘
                                (Pub-Sub / Microservices)
                                                │
                      ┌─────────────────────────┼─────────────────────────┐
                      │                         │                         │
                  Pipeline                 TaskFactory                EventBus
                (Chain of Resp.          (Factory+Command)           (Observer)
                 Pipes&Filters)            │       │
                                      Decorators  Strategies
                                      (Decorator) (Strategy)
                                          │
                                    AbstractTask
                                  (Template Method)
                                          │
                      ┌───────────────────┼───────────────────┐
                      │                   │                   │
                PluginRegistry      ServiceRegistry      Domain Models
                (Microkernel)       (Adapter: REST/      (WorkflowDefinition
                 FileOpsPlugin       SOAP/S3)             TaskConfig, etc.)
```

## Running Tests & Analysis

```bash
mvn clean test                          # Run 130+ unit tests
mvn jacoco:report                       # Coverage → target/site/jacoco/index.html
mvn checkstyle:checkstyle               # Standards → target/site/checkstyle.html
mvn pmd:pmd                             # Quality → target/site/pmd.html
git push                                # SonarCloud auto-analyzes on push
```