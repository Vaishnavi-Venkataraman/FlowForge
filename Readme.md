# FlowForge — Intelligent Workflow Automation Platform

## Building & Running

```
mvn compile
mvn exec:java
```

## Current State: Commit 2 — Domain Models & Interfaces

| Problem in Commit 1 | Fix in Commit 2 |
|----------------------|-----------------|
| HashMap<String,Object> for workflows | WorkflowDefinition domain model |
| HashMap for tasks | TaskConfig with typed parameters |
| Magic strings "completed","failed" | WorkflowStatus and TaskStatus enums |
| Raw trigger strings | TriggerConfig + TriggerType enum |
| if-else chain for task types | Task interface + concrete implementations |
| println("ERROR:...") + return null | Exception hierarchy (FlowForgeException) |
| Linear workflow search O(n) | HashMap lookup O(1) |
| Public mutable ArrayList for logs | Private list with unmodifiable copy |

### Design decisions

**Task interface** — This is the foundation for the Command pattern (Commit 3).
Each task type encapsulates its own execution logic. The engine calls `task.execute(config)`
without knowing or caring what happens inside.

**Exception hierarchy** — `FlowForgeException` → `WorkflowNotFoundException`,
`TaskExecutionException`. Callers can now catch specific failures and decide how to react.

**Immutable where possible** — `TaskConfig.parameters` is unmodifiable,
`WorkflowDefinition.tasks` is an unmodifiable list, `getLogs()` returns a copy.

### Remaining problems

- Task creation is a switch statement in the engine → Factory pattern
- Only sequential execution → Strategy pattern
- Logging/notifications are inline → Observer pattern
- Workflow creation is verbose → Builder pattern 