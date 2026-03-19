package com.flowforge.plugin.builtin;

import com.flowforge.event.WorkflowEvent;
import com.flowforge.plugin.Plugin;
import com.flowforge.plugin.PluginContext;

public class SlackNotificationPlugin implements Plugin {

    private String webhookUrl;
    private String channel;
    private boolean active;

    public SlackNotificationPlugin(String webhookUrl, String channel) {
        this.webhookUrl = webhookUrl;
        this.channel = channel;
    }

    @Override
    public String getId() {
        return "flowforge.slack-notifier";
    }

    @Override
    public String getName() {
        return "Slack Notification Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize(PluginContext context) {
        // Subscribe to workflow failure events
        context.subscribeToEvent(WorkflowEvent.Type.WORKFLOW_FAILED, event -> {
            if (active) {
                sendSlackMessage(
                        ":red_circle: *Workflow Failed*: " + event.getWorkflowName()
                                + "\nError: " + event.getMetadata().getOrDefault("error", "unknown")
                );
            }
        });

        // Subscribe to workflow completion for success notifications
        context.subscribeToEvent(WorkflowEvent.Type.WORKFLOW_COMPLETED, event -> {
            if (active) {
                sendSlackMessage(
                        ":white_check_mark: *Workflow Completed*: " + event.getWorkflowName()
                                + " (" + event.getMetadata().getOrDefault("taskCount", "?") + " tasks)"
                );
            }
        });
    }

    @Override
    public void start() {
        active = true;
        System.out.println("    [SlackPlugin] Connected to " + channel + " via " + webhookUrl);
    }

    @Override
    public void stop() {
        active = false;
        System.out.println("    [SlackPlugin] Disconnected from Slack");
    }

    private void sendSlackMessage(String message) {
        // Simulated — in reality, HTTP POST to Slack webhook
        System.out.println("    [SLACK → " + channel + "] " + message);
    }
}