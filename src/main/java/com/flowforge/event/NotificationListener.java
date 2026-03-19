package com.flowforge.event;

/**
 * Listens only to workflow-level completion and failure events and sends notifications.
 * Now notifications are decoupled. The engine just publishes events;
 * this listener decides which events deserve a notification.
 */
public class NotificationListener implements EventListener {

    @Override
    public void onEvent(WorkflowEvent event) {
        switch (event.getType()) {
            case WORKFLOW_STARTED ->
                    System.out.println("NOTIFY [INFO]: Workflow '" + event.getWorkflowName() + "' started");
            case WORKFLOW_COMPLETED ->
                    System.out.println("NOTIFY [SUCCESS]: Workflow '" + event.getWorkflowName() + "' completed");
            case WORKFLOW_FAILED ->
                    System.out.println("NOTIFY [ALERT]: Workflow '" + event.getWorkflowName()
                            + "' FAILED — " + event.getMetadata().getOrDefault("error", "unknown"));
            default -> { /* Only react to workflow-level events */ }
        }
    }
}