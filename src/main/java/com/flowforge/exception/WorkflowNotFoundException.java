package com.flowforge.exception;

/**
 * Thrown when a workflow ID cannot be resolved.
 */
public class WorkflowNotFoundException extends FlowForgeException {

    public WorkflowNotFoundException(String workflowId) {
        super("Workflow not found: " + workflowId);
    }
}