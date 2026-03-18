# FlowForge — Intelligent Workflow Automation Platform

A Java-based workflow automation platform (like Zapier) built to demonstrate
**software design patterns**, **architectural patterns**, **refactoring**, and **static code analysis**.

## Building & Running

```
mvn compile
mvn exec:java
```

This is the baseline "bad code" with intentional problems:

- **God class**: `WorkflowEngine` handles everything
- **No type safety**: `HashMap<String, Object>` for domain objects
- **Hardcoded if-else**: adding new task types requires editing the engine
- **Tight coupling**: logging, notifications, metrics are inline
- **No error strategy**: exceptions are swallowed or printed
- **Non-thread-safe singleton**
- **Magic strings** everywhere

These will be systematically fixed in subsequent commits.