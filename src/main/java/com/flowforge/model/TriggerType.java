package com.flowforge.model;

/**
 * Enumerates the types of triggers that can start a workflow.
 *
 * WHY: Replaces the if-else chain on trigger type strings in
 * WorkflowEngine.checkTriggers(). Each trigger type has well-defined
 * semantics; an enum makes this explicit and exhaustive.
 */
public enum TriggerType {
    CRON,
    WEBHOOK,
    FILE_WATCH,
    MANUAL,
    EVENT
}