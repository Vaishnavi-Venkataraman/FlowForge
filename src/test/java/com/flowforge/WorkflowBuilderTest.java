package com.flowforge;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.model.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Builder pattern — WorkflowBuilder.
 */
class WorkflowBuilderTest {

    @Test
    void shouldBuildMinimalWorkflow() {
        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Test Workflow")
                .manualTrigger()
                .addHttpGet("fetch", "https://api.test.com")
                .build();

        assertEquals("Test Workflow", wf.getName());
        assertEquals(1, wf.getTasks().size());
        assertEquals("sequential", wf.getExecutionStrategyName());
    }

    @Test
    void shouldBuildWithMultipleTasks() {
        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Multi Task")
                .cronTrigger("0 9 * * *")
                .addHttpGet("step1", "https://api.test.com")
                .addTransformTask("step2", "data", "normalize")
                .addEmailTask("step3", "user@test.com", "Done")
                .parallel()
                .build();

        assertEquals(3, wf.getTasks().size());
        assertEquals("parallel", wf.getExecutionStrategyName());
        assertEquals("CRON", wf.getTriggerTypeName());
    }

    @Test
    void shouldPreserveExistingId() {
        WorkflowDefinition wf = WorkflowBuilder.create()
                .withId("my-fixed-id-123")
                .name("Persisted Workflow")
                .manualTrigger()
                .addDelayTask("wait", 1)
                .build();

        assertEquals("my-fixed-id-123", wf.getId());
    }

    @Test
    void shouldGenerateNewIdWhenNotSpecified() {
        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("New Workflow")
                .manualTrigger()
                .addDelayTask("wait", 1)
                .build();

        assertNotNull(wf.getId());
        assertFalse(wf.getId().isBlank());
    }

    @Test
    void shouldFailWithoutName() {
        assertThrows(IllegalStateException.class, () ->
                WorkflowBuilder.create()
                        .manualTrigger()
                        .addDelayTask("wait", 1)
                        .build());
    }

    @Test
    void shouldFailWithoutTrigger() {
        assertThrows(IllegalStateException.class, () ->
                WorkflowBuilder.create()
                        .name("No Trigger")
                        .addDelayTask("wait", 1)
                        .build());
    }

    @Test
    void shouldFailWithoutTasks() {
        assertThrows(IllegalStateException.class, () ->
                WorkflowBuilder.create()
                        .name("No Tasks")
                        .manualTrigger()
                        .build());
    }
}
