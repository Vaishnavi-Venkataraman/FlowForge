package com.flowforge.web;

import com.flowforge.model.WorkflowStatus;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-user workflow storage with execution history and logs.
 */
public class WorkflowStore {

    /**
     * A stored workflow — contains definition info + execution history.
     */
    public record StoredWorkflow(
            String id,
            String name,
            String owner,
            String triggerType,
            String triggerValue,
            String strategy,
            List<TaskInfo> tasks,
            long createdAt,
            List<ExecutionRecord> executions
    ) {}

    public record TaskInfo(String name, String type, Map<String, String> params) {}

    public record ExecutionRecord(
            String executionId,
            String status,
            long startedAt,
            long finishedAt,
            List<String> logs
    ) {}

    // username → list of workflows
    private final Map<String, List<StoredWorkflow>> store = new ConcurrentHashMap<>();

    public void saveWorkflow(String owner, StoredWorkflow workflow) {
        store.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>()).add(workflow);
    }

    public List<StoredWorkflow> getWorkflows(String owner) {
        return store.getOrDefault(owner, Collections.emptyList());
    }

    public StoredWorkflow getWorkflow(String owner, String workflowId) {
        return getWorkflows(owner).stream()
                .filter(w -> w.id().equals(workflowId))
                .findFirst()
                .orElse(null);
    }

    public void addExecution(String owner, String workflowId, ExecutionRecord record) {
        StoredWorkflow wf = getWorkflow(owner, workflowId);
        if (wf != null) {
            wf.executions().add(record);
        }
    }

    public boolean deleteWorkflow(String owner, String workflowId) {
        List<StoredWorkflow> workflows = store.get(owner);
        if (workflows == null) return false;
        return workflows.removeIf(w -> w.id().equals(workflowId));
    }
}