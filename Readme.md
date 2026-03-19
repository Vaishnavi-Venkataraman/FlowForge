# FlowForge — Intelligent Workflow Automation Platform

A Java 21 workflow automation engine (inspired by Zapier/n8n) demonstrating
**16 design patterns**, **5 architectural patterns**, **refactoring**, and
**static code analysis** through an evolving Git history.

---

## Building & Running

```bash
mvn clean compile
mvn exec:java
```

Requires: Java 21+, Maven 3.8+

---

## Commit History

### Commit 1 — Naive Monolithic Implementation
God class, HashMap data, if-else chains, inline everything. Intentionally bad.

### Commit 2 — Domain Models + Interfaces
`WorkflowDefinition`, `TaskConfig`, `TaskResult`, `Task` interface, enums, exception hierarchy.
**Foundation for**: SRP, OCP.

### Commit 3 — Command + Factory Patterns
`TaskFactory` with runtime registration. Eliminates if-else chain for task types.
**Patterns**: Command, Factory Method.

### Commit 4 — Strategy Pattern
`ExecutionStrategy` → Sequential, Parallel, Conditional. Pluggable execution models.
**Patterns**: Strategy.

### Commit 5 — Observer + Pub-Sub Event System
`EventBus`, `WorkflowEvent`, `LoggingListener`, `NotificationListener`, `MetricsListener`.
**Patterns**: Observer. **Architecture**: Pub-Sub / Event-Driven.

### Commit 6 — Builder Pattern
`WorkflowBuilder` with fluent API and build-time validation.
**Patterns**: Builder.

### Commit 7 — Template Method + Decorator
`AbstractTask` lifecycle (validate→execute→cleanup). `RetryDecorator`, `LoggingDecorator`, `TimeoutDecorator`.
**Patterns**: Template Method, Decorator.

### Commit 8 — Chain of Responsibility + Pipeline
`Pipeline` with `ValidationHandler`, `TransformHandler`, `AuthorizationHandler`, `RateLimitHandler`.
**Patterns**: Chain of Responsibility. **Architecture**: Pipes & Filters.

### Commit 9 — Microkernel / Plugin Architecture
`Plugin`, `PluginContext`, `PluginRegistry`. Builtin plugins: Slack, FileOps, AuditTrail.
**Architecture**: Microkernel.

### Commit 10 — Adapter Pattern
`ExternalService` target interface. Adapters for REST, SOAP, Cloud Storage.
`ServiceRegistry`, `ExternalServiceTask`.
**Patterns**: Adapter.


---

## All Design Patterns

| # | Pattern | Category | Class(es) | Commit |
|---|---------|----------|-----------|--------|
| 1 | Factory Method | Creational | `TaskFactory` | 3 |
| 2 | Builder | Creational | `WorkflowBuilder` | 6 |
| 3 | Command | Behavioral | `Task` interface | 3 |
| 4 | Chain of Responsibility | Behavioral | `PipelineHandler` | 8 |
| 5 | Strategy | Behavioral | `ExecutionStrategy` | 4 |
| 6 | Observer | Behavioral | `EventListener`, `EventBus` | 5 |
| 7 | Template Method | Behavioral | `AbstractTask` | 7 |
| 8 | Decorator | Structural | `RetryDecorator`, etc. | 7 |
| 9 | Adapter | Structural | `RestServiceAdapter`, etc. | 10 |


## All Architectural Patterns (5)

| # | Pattern | Where | Commit |
|---|---------|-------|--------|
| 1 | Pub-Sub / Event-Driven | `EventBus` + listeners | 5 |
| 2 | Pipes & Filters | `Pipeline` + handlers | 8 |
| 3 | Microkernel | `Plugin` system | 9 |
| 4 | Microservices (in-process) | Service boundaries | 11 |
| 5 | Modular Monolith | Package structure | All |

---

## Package Structure

```
com.flowforge/
├── Main.java
├── adapter/
│   ├── ExternalService.java
│   ├── ServiceResponse.java
│   ├── ServiceRegistry.java
│   ├── ExternalServiceTask.java
│   ├── RestServiceAdapter.java
│   ├── SoapServiceAdapter.java
│   ├── CloudStorageAdapter.java
│   └── thirdparty/
│       ├── RestApiClient.java
│       ├── SoapServiceClient.java
│       └── CloudStorageSDK.java
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
├── exception/
│   ├── FlowForgeException.java
│   ├── WorkflowNotFoundException.java
│   └── TaskExecutionException.java
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
├── plugin/
│   ├── Plugin.java
│   ├── PluginContext.java
│   ├── PluginRegistry.java
│   └── builtin/
│       ├── SlackNotificationPlugin.java
│       ├── FileOperationsPlugin.java
│       └── AuditTrailPlugin.java
└── task/
    ├── Task.java
    ├── AbstractTask.java
    ├── TaskFactory.java
    ├── HttpTask.java
    ├── EmailTask.java
    ├── TransformTask.java
    ├── DatabaseTask.java
    ├── DelayTask.java
    └── decorator/
        ├── TaskDecorator.java
        ├── RetryDecorator.java
        ├── LoggingDecorator.java
        └── TimeoutDecorator.java
```