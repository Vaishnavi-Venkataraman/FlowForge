package com.flowforge;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.model.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacadeTest {

    @Test
    void shouldGetSingletonInstance() {
        FlowForgeFacade facade1 = FlowForgeFacade.getInstance();
        FlowForgeFacade facade2 = FlowForgeFacade.getInstance();
        assertSame(facade1, facade2);
    }

    @Test
    void shouldRegisterWorkflow() {
        FlowForgeFacade facade = FlowForgeFacade.getInstance();
        WorkflowDefinition wf = WorkflowBuilder.create()
                .name("Facade Test WF")
                .manualTrigger()
                .addDelayTask("wait", 0)
                .build();
        String id = facade.registerWorkflow(wf);
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    void shouldAddRestService() {
        FlowForgeFacade facade = FlowForgeFacade.getInstance();
        assertDoesNotThrow(() ->
                facade.addRestService("test-api", "Test API", "https://api.test.com"));
    }

    @Test
    void shouldInstallPlugin() {
        FlowForgeFacade facade = FlowForgeFacade.getInstance();
        assertDoesNotThrow(() ->
                facade.installPlugin(new com.flowforge.plugin.builtin.FileOperationsPlugin()));
    }
}
