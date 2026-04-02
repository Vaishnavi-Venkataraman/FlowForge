package com.flowforge;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WorkflowDefinition — state machine transitions.
 */
class WorkflowDefinitionTest {

    private WorkflowDefinition workflow;

    @BeforeEach
    void setUp() {
        workflow = new WorkflowDefinition(
                "Test WF",
                new TriggerConfig(TriggerType.MANUAL, ""),
                List.of(new TaskConfig("t1", "http", Map.of("url", "http://x", "method", "GET")))
        );
    }

    @Test
    void shouldStartInCreatedStatus() {
        assertEquals(WorkflowStatus.CREATED, workflow.getStatus());
    }

    @Test
    void shouldTransitionToRunning() {
        workflow.markRunning();
        assertEquals(WorkflowStatus.RUNNING, workflow.getStatus());
    }

    @Test
    void shouldTransitionToCompleted() {
        workflow.markRunning();
        workflow.markCompleted();
        assertEquals(WorkflowStatus.COMPLETED, workflow.getStatus());
    }

    @Test
    void shouldTransitionToFailed() {
        workflow.markRunning();
        workflow.markFailed();
        assertEquals(WorkflowStatus.FAILED, workflow.getStatus());
    }

    @Test
    void shouldResetFromCompleted() {
        workflow.markRunning();
        workflow.markCompleted();
        workflow.reset();
        assertEquals(WorkflowStatus.CREATED, workflow.getStatus());
    }

    @Test
    void shouldNotCompleteFromCreated() {
        assertThrows(IllegalStateException.class, () -> workflow.markCompleted());
    }

    @Test
    void shouldNotResetFromRunning() {
        workflow.markRunning();
        assertThrows(IllegalStateException.class, () -> workflow.reset());
    }

    @Test
    void shouldRejectNullName() {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkflowDefinition(null,
                        new TriggerConfig(TriggerType.MANUAL, ""),
                        List.of(new TaskConfig("t1", "http", Map.of()))));
    }

    @Test
    void shouldRejectEmptyTasks() {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkflowDefinition("WF",
                        new TriggerConfig(TriggerType.MANUAL, ""),
                        List.of()));
    }
}
