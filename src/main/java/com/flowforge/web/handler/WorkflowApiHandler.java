package com.flowforge.web.handler;

import com.flowforge.exception.FlowForgeException;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.web.JsonUtil;
import com.flowforge.web.UserStore;
import com.flowforge.web.WorkflowStore;
import com.flowforge.web.WorkflowStore.*;
import com.flowforge.FlowForgeFacade;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST API for workflow CRUD and execution.
 * Refactored: workflow building extracted to WorkflowBuildHelper (DRY).
 */
public class WorkflowApiHandler implements HttpHandler {

    private final UserStore userStore;
    private final WorkflowStore workflowStore;
    private final FlowForgeFacade flowforge;

    public WorkflowApiHandler(UserStore userStore, WorkflowStore workflowStore, FlowForgeFacade flowforge) {
        this.userStore = userStore;
        this.workflowStore = workflowStore;
        this.flowforge = flowforge;
    }

    /**
     * Re-registers persisted workflows with the engine on startup.
     */
    public void reloadPersistedWorkflows() {
        int count = 0;
        for (String owner : workflowStore.getAllOwners()) {
            for (StoredWorkflow stored : workflowStore.getWorkflows(owner)) {
                try {
                    WorkflowDefinition wfDef = WorkflowBuildHelper.buildWorkflow(
                            stored.name(), stored.triggerType(), stored.triggerValue(),
                            stored.strategy(), stored.tasks());
                    flowforge.registerWorkflowWithId(stored.id(), wfDef);
                    count++;
                } catch (Exception e) {
                    System.err.println("[Reload] Failed: " + stored.name() + " — " + e.getMessage());
                }
            }
        }
        if (count > 0) System.out.println("[WorkflowApi] Reloaded " + count + " workflows from disk");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange); return;
        }

        String sessionId = HttpHelper.getSessionId(exchange);
        String username = userStore.getUsername(sessionId);
        if (username == null) {
            HttpHelper.sendJson(exchange, 401, Map.of("error", "Not authenticated")); return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/api/workflows") && "GET".equals(method)) {
                listWorkflows(exchange, username);
            } else if (path.equals("/api/workflows") && "POST".equals(method)) {
                createWorkflow(exchange, username);
            } else if (path.matches("/api/workflows/[^/]+/run") && "POST".equals(method)) {
                runWorkflow(exchange, username, path.split("/")[3]);
            } else if (path.matches("/api/workflows/[^/]+") && "GET".equals(method)) {
                getWorkflow(exchange, username, path.split("/")[3]);
            } else if (path.matches("/api/workflows/[^/]+") && "DELETE".equals(method)) {
                deleteWorkflow(exchange, username, path.split("/")[3]);
            } else {
                HttpHelper.sendJson(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception e) {
            HttpHelper.sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void listWorkflows(HttpExchange exchange, String username) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        for (StoredWorkflow wf : workflowStore.getWorkflows(username)) {
            result.add(toSummaryMap(wf));
        }
        HttpHelper.sendJsonArray(exchange, 200, JsonUtil.toJsonArray(result));
    }

    private void createWorkflow(HttpExchange exchange, String username) throws IOException {
        Map<String, String> body = HttpHelper.parseBody(exchange);
        String name = body.get("name");
        String triggerType = body.getOrDefault("triggerType", "MANUAL");
        String triggerValue = body.getOrDefault("triggerValue", "");
        String strategy = body.getOrDefault("strategy", "sequential");
        String tasksJson = body.getOrDefault("tasks", "[]");

        if (name == null || name.isBlank()) {
            HttpHelper.sendJson(exchange, 400, Map.of("error", "Workflow name is required")); return;
        }

        List<TaskInfo> taskInfos = parseTasksFromJson(tasksJson);
        if (taskInfos.isEmpty()) {
            HttpHelper.sendJson(exchange, 400, Map.of("error", "At least one task is required")); return;
        }

        // Build via shared helper (DRY — same logic used by reloadPersistedWorkflows)
        WorkflowDefinition wfDef = WorkflowBuildHelper.buildWorkflow(name, triggerType, triggerValue, strategy, taskInfos);
        String wfId = flowforge.registerWorkflow(wfDef);

        StoredWorkflow stored = new StoredWorkflow(wfId, name, username, triggerType, triggerValue,
                strategy, taskInfos, System.currentTimeMillis(), new CopyOnWriteArrayList<>());
        workflowStore.saveWorkflow(username, stored);
        HttpHelper.sendJson(exchange, 201, toSummaryMap(stored));
    }

    private void getWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        StoredWorkflow wf = workflowStore.getWorkflow(username, id);
        if (wf == null) { HttpHelper.sendJson(exchange, 404, Map.of("error", "Workflow not found")); return; }
        HttpHelper.sendJson(exchange, 200, toDetailMap(wf));
    }

    private void runWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        StoredWorkflow wf = workflowStore.getWorkflow(username, id);
        if (wf == null) { HttpHelper.sendJson(exchange, 404, Map.of("error", "Workflow not found")); return; }

        String execId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();
        String status;

        // Capture stdout during execution
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream capture = new java.io.PrintStream(baos);
        java.io.PrintStream original = System.out;

        try {
            System.setOut(capture);
            flowforge.fireTrigger(id);
            System.out.flush();
            status = "COMPLETED";
        } catch (FlowForgeException e) {
            System.out.flush();
            status = "FAILED";
            logs.add("ERROR: " + e.getMessage());
        } catch (Exception e) {
            System.out.flush();
            status = "FAILED";
            logs.add("ERROR: " + e.getMessage());
        } finally {
            System.setOut(original);
        }

        String captured = baos.toString();
        if (!captured.isBlank()) logs.addAll(0, Arrays.asList(captured.split("\n")));

        long endTime = System.currentTimeMillis();
        ExecutionRecord record = new ExecutionRecord(execId, status, startTime, endTime, logs);
        workflowStore.addExecution(username, id, record);
        System.out.println("[Execute] " + wf.name() + " -> " + status + " (" + (endTime - startTime) + "ms)");

        HttpHelper.sendJson(exchange, 200, toExecutionMap(record));
    }

    private void deleteWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        if (!workflowStore.deleteWorkflow(username, id)) {
            HttpHelper.sendJson(exchange, 404, Map.of("error", "Workflow not found")); return;
        }
        HttpHelper.sendJson(exchange, 200, Map.of("success", true));
    }

    // --- Mapping helpers ---

    private Map<String, Object> toSummaryMap(StoredWorkflow wf) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", wf.id()); m.put("name", wf.name());
        m.put("triggerType", wf.triggerType()); m.put("triggerValue", wf.triggerValue());
        m.put("strategy", wf.strategy()); m.put("taskCount", wf.tasks().size());
        m.put("createdAt", wf.createdAt()); m.put("executionCount", wf.executions().size());
        if (!wf.executions().isEmpty()) {
            ExecutionRecord last = wf.executions().get(wf.executions().size() - 1);
            m.put("lastStatus", last.status()); m.put("lastRunAt", last.startedAt());
        } else {
            m.put("lastStatus", "NEVER_RUN"); m.put("lastRunAt", 0);
        }
        return m;
    }

    private Map<String, Object> toDetailMap(StoredWorkflow wf) {
        Map<String, Object> m = toSummaryMap(wf);
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (TaskInfo ti : wf.tasks()) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("name", ti.name()); tm.put("type", ti.type()); tm.put("params", ti.params());
            tasks.add(tm);
        }
        m.put("tasks", tasks);
        List<Map<String, Object>> execs = new ArrayList<>();
        for (ExecutionRecord er : wf.executions()) execs.add(toExecutionMap(er));
        m.put("executions", execs);
        return m;
    }

    private Map<String, Object> toExecutionMap(ExecutionRecord er) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("executionId", er.executionId()); m.put("status", er.status());
        m.put("startedAt", er.startedAt()); m.put("finishedAt", er.finishedAt());
        m.put("durationMs", er.finishedAt() - er.startedAt());
        m.put("logCount", er.logs().size());
        m.put("logs", String.join("\n", er.logs()));
        return m;
    }

    // --- JSON task parsing ---

    private List<TaskInfo> parseTasksFromJson(String tasksJson) {
        List<TaskInfo> tasks = new ArrayList<>();
        tasksJson = tasksJson.trim();
        if (!tasksJson.startsWith("[")) return tasks;
        tasksJson = tasksJson.substring(1);
        if (tasksJson.endsWith("]")) tasksJson = tasksJson.substring(0, tasksJson.length() - 1);

        int depth = 0, start = 0;
        for (int i = 0; i < tasksJson.length(); i++) {
            char c = tasksJson.charAt(i);
            if (c == '{') depth++; else if (c == '}') depth--;
            else if (c == ',' && depth == 0) {
                addParsedTask(tasks, tasksJson.substring(start, i).trim());
                start = i + 1;
            }
        }
        addParsedTask(tasks, tasksJson.substring(start).trim());
        return tasks;
    }

    private void addParsedTask(List<TaskInfo> tasks, String json) {
        if (json.isEmpty()) return;
        Map<String, String> flat = JsonUtil.parseJsonFlat(json);
        String name = flat.get("name"), type = flat.get("type");
        if (name == null || type == null) return;
        Map<String, String> params = new HashMap<>();
        String paramsStr = flat.get("params");
        if (paramsStr != null && paramsStr.startsWith("{")) params = JsonUtil.parseJsonFlat(paramsStr);
        tasks.add(new TaskInfo(name, type, params));
    }
}