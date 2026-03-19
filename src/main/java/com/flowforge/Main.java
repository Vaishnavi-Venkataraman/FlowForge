package com.flowforge;

import com.flowforge.engine.WorkflowEngine;
import com.flowforge.engine.strategy.ConditionalStrategy;
import com.flowforge.engine.strategy.ParallelStrategy;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TriggerConfig;
import com.flowforge.model.TriggerType;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.task.TaskFactory;

import java.util.List;
import java.util.Map;

/**
 * Entry point — demonstrates Strategy pattern with three execution modes.
 */
public class Main {

    public static void main(String[] args) {
        TaskFactory factory = new TaskFactory();
        WorkflowEngine engine = new WorkflowEngine(factory);

        // Register additional strategies
        engine.registerStrategy(new ParallelStrategy());
        engine.registerStrategy(new ConditionalStrategy(
                config -> !config.getType().equals("delay"),
                "skip-delays"
        ));

        // ============================================================
        // WORKFLOW 1: Sequential (default) — tasks depend on each other
        // ============================================================
        WorkflowDefinition seqWorkflow = new WorkflowDefinition(
                "Sequential: ETL Pipeline",
                new TriggerConfig(TriggerType.CRON, "0 2 * * *"),
                List.of(
                        new TaskConfig("Extract Data", "http", Map.of(
                                "url", "https://api.source.com/data", "method", "GET")),
                        new TaskConfig("Transform Data", "transform", Map.of(
                                "input", "raw_data", "operation", "normalize")),
                        new TaskConfig("Load to DB", "database", Map.of(
                                "query", "INSERT INTO warehouse SELECT * FROM staging"))
                ),
                "sequential"
        );

        String id1 = engine.registerWorkflow(seqWorkflow);
        System.out.println("\n========== WORKFLOW 1: Sequential ==========\n");
        engine.executeWorkflow(id1);

        // ============================================================
        // WORKFLOW 2: Parallel — independent notification tasks
        // ============================================================
        WorkflowDefinition parWorkflow = new WorkflowDefinition(
                "Parallel: Multi-Channel Notify",
                new TriggerConfig(TriggerType.EVENT, "user.signup"),
                List.of(
                        new TaskConfig("Send Welcome Email", "email", Map.of(
                                "to", "user@example.com", "subject", "Welcome!")),
                        new TaskConfig("Log Signup Event", "database", Map.of(
                                "query", "INSERT INTO events(type) VALUES('signup')")),
                        new TaskConfig("Fetch User Profile", "http", Map.of(
                                "url", "https://api.crm.com/enrich", "method", "POST"))
                ),
                "parallel"
        );

        String id2 = engine.registerWorkflow(parWorkflow);
        System.out.println("\n========== WORKFLOW 2: Parallel ==========\n");
        engine.executeWorkflow(id2);

        // ============================================================
        // WORKFLOW 3: Conditional — skip delay tasks
        // ============================================================
        WorkflowDefinition condWorkflow = new WorkflowDefinition(
                "Conditional: Fast Processing",
                new TriggerConfig(TriggerType.MANUAL, ""),
                List.of(
                        new TaskConfig("Fetch Config", "http", Map.of(
                                "url", "https://api.config.com/settings", "method", "GET")),
                        new TaskConfig("Wait for Propagation", "delay", Map.of(
                                "seconds", "2")),
                        new TaskConfig("Apply Settings", "transform", Map.of(
                                "input", "config_data", "operation", "apply"))
                ),
                "conditional(skip-delays)"
        );

        String id3 = engine.registerWorkflow(condWorkflow);
        System.out.println("\n========== WORKFLOW 3: Conditional (skip delays) ==========\n");
        engine.executeWorkflow(id3);

        // ============================================================
        // STATS
        // ============================================================
        System.out.println("\n========== Statistics ==========\n");
        engine.printStats();
    }
}