package com.flowforge;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.pipeline.Pipeline;
import com.flowforge.pipeline.PipelineContext;
import com.flowforge.pipeline.PipelineHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Chain of Responsibility + Pipes & Filters — Pipeline.
 */
class PipelineTest {

    private WorkflowDefinition createTestWorkflow() {
        return new WorkflowDefinition("PipelineTest",
                new TriggerConfig(TriggerType.MANUAL, ""),
                List.of(new TaskConfig("t1", "http", Map.of("url", "http://x", "method", "GET"))));
    }

    @Test
    void shouldExecuteAllHandlers() {
        Pipeline pipeline = new Pipeline();
        pipeline.addHandler(new TestHandler("handler1"));
        pipeline.addHandler(new TestHandler("handler2"));

        PipelineContext result = pipeline.execute(new PipelineContext(createTestWorkflow()));

        assertFalse(result.isAborted());
        assertTrue(result.getProcessingLog().stream().anyMatch(log -> log.contains("handler1")));
        assertTrue(result.getProcessingLog().stream().anyMatch(log -> log.contains("handler2")));
    }

    @Test
    void shouldStopOnAbort() {
        Pipeline pipeline = new Pipeline();
        pipeline.addHandler(new AbortHandler("blocker", "blocked"));
        pipeline.addHandler(new TestHandler("should-not-run"));

        PipelineContext result = pipeline.execute(new PipelineContext(createTestWorkflow()));

        assertTrue(result.isAborted());
        assertEquals("blocked", result.getAbortReason());
    }

    @Test
    void shouldHandleEmptyPipeline() {
        Pipeline pipeline = new Pipeline();
        PipelineContext result = pipeline.execute(new PipelineContext(createTestWorkflow()));
        assertFalse(result.isAborted());
    }

    /**
     * Test handler that logs its name and passes to next.
     */
    private static class TestHandler implements PipelineHandler {
        private final String handlerName;

        TestHandler(String handlerName) {
            this.handlerName = handlerName;
        }

        @Override
        public String getName() {
            return handlerName;
        }

        @Override
        public void handle(PipelineContext context, PipelineHandler next) {
            context.addLog("[" + handlerName + "] processed");
            next.handle(context, next);
        }
    }

    /**
     * Test handler that aborts the pipeline.
     */
    private static class AbortHandler implements PipelineHandler {
        private final String handlerName;
        private final String reason;

        AbortHandler(String handlerName, String reason) {
            this.handlerName = handlerName;
            this.reason = reason;
        }

        @Override
        public String getName() {
            return handlerName;
        }

        @Override
        public void handle(PipelineContext context, PipelineHandler next) {
            context.abort(reason);
            // Do NOT call next — abort stops the chain
        }
    }
}