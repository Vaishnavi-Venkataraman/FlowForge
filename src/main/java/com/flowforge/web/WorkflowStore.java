package com.flowforge.web;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;

/**
 * Each user's workflows are saved to data/workflows/{username}.dat
 * Execution history saved to data/executions/{workflowId}.dat
 */
public class WorkflowStore {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path WF_DIR = DATA_DIR.resolve("workflows");
    private static final Path EXEC_DIR = DATA_DIR.resolve("executions");

    public record StoredWorkflow(
            String id, String name, String owner,
            String triggerType, String triggerValue, String strategy,
            List<TaskInfo> tasks, long createdAt,
            List<ExecutionRecord> executions
    ) {}

    public record TaskInfo(String name, String type, Map<String, String> params) {}

    public record ExecutionRecord(
            String executionId, String status,
            long startedAt, long finishedAt, List<String> logs
    ) {}

    private final Map<String, List<StoredWorkflow>> store = new ConcurrentHashMap<>();

    public WorkflowStore() {
        loadAllFromDisk();
    }

    public void saveWorkflow(String owner, StoredWorkflow workflow) {
        store.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>()).add(workflow);
        saveUserWorkflows(owner);
    }

    public List<StoredWorkflow> getWorkflows(String owner) {
        return store.getOrDefault(owner, Collections.emptyList());
    }

    public StoredWorkflow getWorkflow(String owner, String workflowId) {
        return getWorkflows(owner).stream()
                .filter(w -> w.id().equals(workflowId))
                .findFirst().orElse(null);
    }

    public void addExecution(String owner, String workflowId, ExecutionRecord record) {
        StoredWorkflow wf = getWorkflow(owner, workflowId);
        if (wf != null) {
            wf.executions().add(record);
            saveExecution(workflowId, record);
        }
    }

    public Set<String> getAllOwners() {
        return store.keySet();
    }

    public boolean deleteWorkflow(String owner, String workflowId) {
        List<StoredWorkflow> workflows = store.get(owner);
        if (workflows == null) return false;
        boolean removed = workflows.removeIf(w -> w.id().equals(workflowId));
        if (removed) {
            saveUserWorkflows(owner);
            try { Files.deleteIfExists(EXEC_DIR.resolve(workflowId + ".dat")); } catch (IOException ignored) {}
        }
        return removed;
    }

    // --- Persistence ---

    private void saveUserWorkflows(String owner) {
        try {
            Files.createDirectories(WF_DIR);
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(WF_DIR.resolve(owner + ".dat")))) {
                for (StoredWorkflow wf : getWorkflows(owner)) {
                    // Format: id|name|triggerType|triggerValue|strategy|createdAt|task1json;task2json
                    StringBuilder tasks = new StringBuilder();
                    for (int i = 0; i < wf.tasks().size(); i++) {
                        TaskInfo t = wf.tasks().get(i);
                        if (i > 0) tasks.append(";");
                        tasks.append(t.name()).append("~").append(t.type()).append("~");
                        t.params().forEach((k, v) -> tasks.append(k).append("=").append(v).append(","));
                    }
                    pw.println(wf.id() + "|" + wf.name() + "|" + wf.triggerType() + "|"
                            + wf.triggerValue() + "|" + wf.strategy() + "|" + wf.createdAt() + "|" + tasks);
                }
            }
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Save failed: " + e.getMessage());
        }
    }

    private void saveExecution(String workflowId, ExecutionRecord record) {
        try {
            Files.createDirectories(EXEC_DIR);
            Path file = EXEC_DIR.resolve(workflowId + ".dat");
            try (PrintWriter pw = new PrintWriter(new FileWriter(file.toFile(), true))) {
                String logsEncoded = String.join("\\n", record.logs().stream()
                        .map(l -> l.replace("|", "¦").replace("\n", "\\n")).toList());
                pw.println(record.executionId() + "|" + record.status() + "|"
                        + record.startedAt() + "|" + record.finishedAt() + "|" + logsEncoded);
            }
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Exec save failed: " + e.getMessage());
        }
    }

    private void loadAllFromDisk() {
        try {
            if (!Files.exists(WF_DIR)) return;
            try (var files = Files.list(WF_DIR)) {
                files.filter(p -> p.toString().endsWith(".dat")).forEach(this::loadUserFile);
            }
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Load failed: " + e.getMessage());
        }
    }

    private void loadUserFile(Path file) {
        String owner = file.getFileName().toString().replace(".dat", "");
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;

                String id = parts[0], name = parts[1], trigType = parts[2],
                        trigVal = parts[3], strategy = parts[4];
                long createdAt = Long.parseLong(parts[5]);
                String tasksStr = parts[6];

                List<TaskInfo> tasks = new ArrayList<>();
                if (!tasksStr.isBlank()) {
                    for (String tStr : tasksStr.split(";")) {
                        String[] tParts = tStr.split("~", 3);
                        if (tParts.length < 2) continue;
                        Map<String, String> params = new HashMap<>();
                        if (tParts.length == 3 && !tParts[2].isBlank()) {
                            for (String kv : tParts[2].split(",")) {
                                String[] kvParts = kv.split("=", 2);
                                if (kvParts.length == 2) params.put(kvParts[0], kvParts[1]);
                            }
                        }
                        tasks.add(new TaskInfo(tParts[0], tParts[1], params));
                    }
                }

                List<ExecutionRecord> execs = loadExecutions(id);
                StoredWorkflow wf = new StoredWorkflow(id, name, owner, trigType, trigVal,
                        strategy, tasks, createdAt, new CopyOnWriteArrayList<>(execs));
                store.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>()).add(wf);
            }
            System.out.println("[WorkflowStore] Loaded " + getWorkflows(owner).size()
                    + " workflows for user: " + owner);
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Failed loading " + file + ": " + e.getMessage());
        }
    }

    private List<ExecutionRecord> loadExecutions(String workflowId) {
        List<ExecutionRecord> execs = new ArrayList<>();
        Path file = EXEC_DIR.resolve(workflowId + ".dat");
        if (!Files.exists(file)) return execs;
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 5);
                if (parts.length < 5) continue;
                List<String> logs = new ArrayList<>(Arrays.asList(
                        parts[4].replace("\\n", "\n").replace("¦", "|").split("\n")));
                execs.add(new ExecutionRecord(parts[0], parts[1],
                        Long.parseLong(parts[2]), Long.parseLong(parts[3]), logs));
            }
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Failed loading executions for " + workflowId);
        }
        return execs;
    }
}