package com.flowforge;

import com.flowforge.model.*;
import com.flowforge.exception.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void taskConfigShouldExposeFields() {
        TaskConfig config = new TaskConfig("name", "http", Map.of("url", "http://x"));
        assertEquals("name", config.getName());
        assertEquals("http", config.getType());
        assertEquals("http://x", config.getRequiredParameter("url"));
        assertNotNull(config.getParameters());
    }

    @Test
    void taskConfigShouldThrowOnMissingRequired() {
        TaskConfig config = new TaskConfig("name", "http", Map.of());
        assertThrows(IllegalArgumentException.class, () -> config.getRequiredParameter("url"));
    }

    @Test
    void taskResultSuccessShouldHaveOutput() {
        TaskResult result = TaskResult.success("output", Instant.now());
        assertTrue(result.isSuccess());
        assertEquals("output", result.getOutput());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getDuration());
    }

    @Test
    void taskResultFailureShouldHaveError() {
        TaskResult result = TaskResult.failure("error msg", Instant.now());
        assertFalse(result.isSuccess());
        assertEquals("error msg", result.getErrorMessage());
    }

    @Test
    void workflowStatusShouldTrackStates() {
        assertTrue(WorkflowStatus.CREATED.isExecutable());
        assertFalse(WorkflowStatus.RUNNING.isExecutable());
        assertTrue(WorkflowStatus.COMPLETED.isTerminal());
        assertTrue(WorkflowStatus.FAILED.isTerminal());
        assertFalse(WorkflowStatus.RUNNING.isTerminal());
    }

    @Test
    void triggerConfigShouldExposeFields() {
        TriggerConfig config = new TriggerConfig(TriggerType.CRON, "0 9 * * *");
        assertEquals(TriggerType.CRON, config.getType());
        assertEquals("0 9 * * *", config.getValue());
    }

    @Test
    void triggerTypeShouldHaveAllValues() {
        assertTrue(TriggerType.values().length >= 4);
    }

    @Test
    void flowForgeExceptionShouldWrapMessage() {
        FlowForgeException ex = new TaskExecutionException("task1", "failed");
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("task1"));
    }

    @Test
    void workflowNotFoundExceptionShouldContainId() {
        WorkflowNotFoundException ex = new WorkflowNotFoundException("abc-123");
        assertTrue(ex.getMessage().contains("abc-123"));
    }

    @Test
    void workflowDefinitionToStringShouldWork() {
        WorkflowDefinition wf = new WorkflowDefinition("Test",
                new TriggerConfig(TriggerType.MANUAL, ""),
                java.util.List.of(new TaskConfig("t1", "delay", Map.of("seconds", "0"))));
        String str = wf.toString();
        assertTrue(str.contains("Test"));
        assertTrue(str.contains("sequential"));
    }
}
