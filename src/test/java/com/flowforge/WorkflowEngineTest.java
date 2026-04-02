package com.flowforge;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.pipeline.Pipeline;
import com.flowforge.pipeline.ValidationHandler;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowEngineTest {

    private WorkflowEngine engine;
    private TaskFactory factory;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        factory = new TaskFactory();
        eventBus = new EventBus();
        engine = new WorkflowEngine(factory, eventBus);
        engine.registerStrategy(new ParallelStrategy());
    }

    @Test
    void shouldRegisterAndExecuteWorkflow() {
        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Engine Test")
                .manualTrigger()
                .addDelayTask("wait", 0)
                .build();
        String id = engine.registerWorkflow(wf);
        assertDoesNotThrow(() -> engine.executeWorkflow(id));
    }

    @Test
    void shouldThrowOnUnknownWorkflow() {
        assertThrows(WorkflowNotFoundException.class,
                () -> engine.executeWorkflow("nonexistent-id"));
    }

    @Test
    void shouldExecuteWithPipeline() {
        Pipeline pipeline = new Pipeline();
        pipeline.addHandler(new ValidationHandler(factory));
        engine.setPipeline(pipeline);

        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Pipeline Engine Test")
                .manualTrigger()
                .addDelayTask("wait", 0)
                .build();
        String id = engine.registerWorkflow(wf);
        assertDoesNotThrow(() -> engine.executeWorkflow(id));
    }

    @Test
    void shouldExecuteParallel() {
        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Parallel Test")
                .manualTrigger()
                .addDelayTask("w1", 0)
                .addDelayTask("w2", 0)
                .parallel()
                .build();
        String id = engine.registerWorkflow(wf);
        assertDoesNotThrow(() -> engine.executeWorkflow(id));
    }
}
