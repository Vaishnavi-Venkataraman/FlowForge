package com.flowforge.pipeline;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.WorkflowDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable context object passed through the pipeline chain.
 * PipelineContext acts as the "request" object flowing through the chain.
 * Handlers enrich it, validate it, or halt it.
 */
public class PipelineContext {

    private final WorkflowDefinition workflow;
    private final Map<String, Object> attributes;
    private final List<String> processingLog;
    private List<TaskConfig> processedTasks;
    private boolean aborted;
    private String abortReason;

    public PipelineContext(WorkflowDefinition workflow) {
        this.workflow = workflow;
        this.attributes = new HashMap<>();
        this.processingLog = new ArrayList<>();
        this.processedTasks = new ArrayList<>(workflow.getTasks());
        this.aborted = false;
    }

    public WorkflowDefinition getWorkflow() {
        return workflow;
    }

    /**
     * Arbitrary key-value store for handlers to share data.
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        return (T) attributes.get(key);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * The task list — handlers can filter, reorder, or enrich this.
     */
    public List<TaskConfig> getProcessedTasks() {
        return processedTasks;
    }

    public void setProcessedTasks(List<TaskConfig> tasks) {
        this.processedTasks = tasks;
    }

    /**
     * Abort the pipeline — subsequent handlers will not run.
     */
    public void abort(String reason) {
        this.aborted = true;
        this.abortReason = reason;
        addLog("ABORTED: " + reason);
    }

    public boolean isAborted() {
        return aborted;
    }

    public String getAbortReason() {
        return abortReason;
    }

    /**
     * Processing log — each handler records what it did.
     */
    public void addLog(String message) {
        processingLog.add(message);
    }

    public List<String> getProcessingLog() {
        return List.copyOf(processingLog);
    }
}