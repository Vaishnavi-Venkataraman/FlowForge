package com.flowforge.plugin.builtin;

import com.flowforge.event.WorkflowEvent;
import com.flowforge.pipeline.PipelineContext;
import com.flowforge.pipeline.PipelineHandler;
import com.flowforge.plugin.Plugin;
import com.flowforge.plugin.PluginContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditTrailPlugin implements Plugin {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final List<String> auditLog = Collections.synchronizedList(new ArrayList<>());
    private int auditSequence = 0;

    @Override
    public String getId() {
        return "flowforge.audit-trail";
    }

    @Override
    public String getName() {
        return "Audit Trail Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize(PluginContext context) {
        // 1. Add pipeline handler for audit stamping
        context.addPipelineHandler(new PipelineHandler() {
            @Override
            public String getName() {
                return "AuditStampHandler";
            }

            @Override
            public void handle(PipelineContext ctx, PipelineHandler next) {
                String auditId = "AUDIT-" + (++auditSequence);
                ctx.setAttribute("auditId", auditId);
                ctx.addLog("[AuditTrailPlugin] Assigned audit ID: " + auditId);

                String entry = FORMATTER.format(Instant.now())
                        + " | " + auditId
                        + " | EXECUTE | " + ctx.getWorkflow().getName()
                        + " | tasks=" + ctx.getProcessedTasks().size();
                auditLog.add(entry);

                next.handle(ctx, null);
            }
        });

        // 2. Subscribe to all events for comprehensive audit log
        context.subscribeToAllEvents(event -> {
            String entry = FORMATTER.format(event.getTimestamp())
                    + " | EVENT | " + event.getType()
                    + " | " + event.getMessage();
            auditLog.add(entry);
        });

        System.out.println("    [AuditPlugin] Pipeline handler + event listener registered");
    }

    @Override
    public void start() {
        auditLog.add(FORMATTER.format(Instant.now()) + " | SYSTEM | Audit trail started");
    }

    @Override
    public void stop() {
        auditLog.add(FORMATTER.format(Instant.now()) + " | SYSTEM | Audit trail stopped");
    }

    /**
     * Prints the complete audit trail.
     */
    public void printAuditTrail() {
        System.out.println("=== Audit Trail (" + auditLog.size() + " entries) ===");
        for (String entry : auditLog) {
            System.out.println("  " + entry);
        }
    }
}