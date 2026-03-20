# FlowForge — User Manual

## Getting Started

```bash
mvn clean compile
mvn exec:java
```

Open **http://localhost:8080** in your browser. Data persists in the `data/` folder.

---

## 1. Create an Account

Click **"Create an account"** on the sign-in page.

**Password requirements:**
- At least 6 characters
- One uppercase letter (A-Z)
- One number (0-9)
- One special character (!@#$%^&*)

**Example:**
- Display Name: `John Doe`
- Username: `john_doe`
- Password: `Secure@123`

---

## 2. Example Workflows to Try

### Example A: Sequential ETL Pipeline (uses HTTP + Transform + Database tasks)

This demonstrates **sequential execution** — each task depends on the previous one.

| Field | Value |
|-------|-------|
| Workflow Name | `Daily CRM Sync` |
| Trigger Type | `Cron Schedule` |
| Trigger Value | `0 9 * * *` |
| Strategy | `Sequential` |

**Tasks:**

| # | Name | Type | Parameters |
|---|------|------|-----------|
| 1 | Fetch Customers | http | URL: `https://api.crm.com/customers` · Method: `GET` |
| 2 | Normalize Data | transform | Input: `customer_json` · Operation: `flatten` |
| 3 | Load to Warehouse | database | Query: `INSERT INTO warehouse SELECT * FROM staging` |

**What you'll see in logs:** HTTP GET → Transform → SQL execution in order.

---

### Example B: Parallel Notifications (uses Email + HTTP + Database tasks)

This demonstrates **parallel execution** — all tasks run simultaneously.

| Field | Value |
|-------|-------|
| Workflow Name | `Order Confirmation` |
| Trigger Type | `Webhook` |
| Trigger Value | `/api/orders/new` |
| Strategy | `Parallel` |

**Tasks:**

| # | Name | Type | Parameters |
|---|------|------|-----------|
| 1 | Email Customer | email | To: `customer@shop.com` · Subject: `Order Confirmed!` |
| 2 | Update CRM | http | URL: `https://api.crm.com/update` · Method: `POST` |
| 3 | Write Audit Log | database | Query: `INSERT INTO audit(event) VALUES('order')` |

**What you'll see in logs:** All 3 tasks run on different threads concurrently.

---

### Example C: Conditional Execution (uses Database + Delay + FTP tasks)

This demonstrates **conditional strategy** — delay tasks are automatically skipped.

| Field | Value |
|-------|-------|
| Workflow Name | `File Export Pipeline` |
| Trigger Type | `Manual` |
| Strategy | `Conditional (skip delays)` |

**Tasks:**

| # | Name | Type | Parameters |
|---|------|------|-----------|
| 1 | Export CSV | database | Query: `COPY sales TO '/tmp/sales.csv'` |
| 2 | Wait for Flush | delay | Seconds: `5` |
| 3 | Upload to Partner | ftp | Host: `ftp.partner.com` · File: `sales.csv` |

**What you'll see in logs:** Task 1 runs, Task 2 is SKIPPED, Task 3 runs.

---

### Example D: External Service Integration (uses REST + SOAP + Cloud Storage adapters)

This demonstrates the **Adapter pattern** — one task type calls different protocols.

| Field | Value |
|-------|-------|
| Workflow Name | `Multi-Protocol Integration` |
| Trigger Type | `Manual` |
| Strategy | `Sequential` |

**Tasks:**

| # | Name | Type | Parameters |
|---|------|------|-----------|
| 1 | Fetch from CRM | external_service | Service ID: `crm-api` · Operation: `GET` · Path: `/deals` |
| 2 | Get ERP Data | external_service | Service ID: `erp-legacy` · Operation: `GetInvoices` · Path: `/pending` |
| 3 | Upload to Lake | external_service | Service ID: `data-lake` · Operation: `UPLOAD` · Path: `/reports/latest.json` |

**What you'll see in logs:** REST call → SOAP call → Cloud Storage upload, all via one task type.

---

### Example E: Plugin Task Types (uses FTP + File Copy + File Delete)

These task types come from the **FileOperationsPlugin** (Microkernel pattern).

| Field | Value |
|-------|-------|
| Workflow Name | `File Cleanup Job` |
| Trigger Type | `Cron Schedule` |
| Trigger Value | `0 0 * * *` |
| Strategy | `Sequential` |

**Tasks:**

| # | Name | Type | Parameters |
|---|------|------|-----------|
| 1 | Download Report | ftp | Host: `ftp.reports.com` · File: `daily.csv` |
| 2 | Copy to Archive | file_copy | Source: `/tmp/daily.csv` · Destination: `/archive/daily.csv` |
| 3 | Cleanup Temp | file_delete | Path: `/tmp/daily.csv` |

---

### Example F: Failure Scenario (intentional error)

This shows how errors are displayed in the execution logs.

| Field | Value |
|-------|-------|
| Workflow Name | `Broken Workflow` |
| Trigger Type | `Manual` |
| Strategy | `Sequential` |

**Tasks:**

| # | Name | Type | Parameters |
|---|------|------|-----------|
| 1 | Fetch Data | http | URL: `https://api.example.com/data` · Method: `GET` |
| 2 | Bad Task | email | *(leave To and Subject EMPTY)* |

**What you'll see:** Task 1 succeeds, Task 2 fails with validation error (missing required parameter). Execution status shows FAILED in red. Expand the log to see the full error trace.

---

## 3. UI Features

### Dark Mode
Click the moon/sun icon in the top-right corner. Preference persists across sessions.

### Execution History
Each workflow tracks all past executions. Click any execution row to expand its logs.

**Log color coding:**
- **Blue** — Pipeline processing and info logs
- **Green** — Success messages
- **Red** — Errors and failures
- **Orange** — Warnings and notifications

### Data Persistence
All data saves to the `data/` folder:
- `data/users.dat` — user accounts
- `data/workflows/{username}.dat` — workflow definitions
- `data/executions/{workflow-id}.dat` — execution history

Stop and restart the app — all your data is still there.

---

## 4. What Happens Under the Hood

When you click **▶ Run**:

1. **TriggerService** detects the trigger and publishes to `execution.requests` channel
2. **ExecutionService** picks up the request from the **ServiceBus**
3. Workflow passes through the **Pipeline** (validation → authorization → rate limit → transform)
4. **WorkflowEngine** selects the **ExecutionStrategy** (sequential/parallel/conditional)
5. Each task is created by **TaskFactory**, wrapped by **Decorators** (logging + retry + timeout)
6. Tasks execute through **AbstractTask** lifecycle (validate → doExecute → cleanup)
7. External service tasks use **Adapters** (REST/SOAP/Cloud) via **ServiceRegistry**
8. Plugin tasks (FTP, file_copy) come from **FileOperationsPlugin** via **PluginRegistry**
9. Events published to **EventBus** → picked up by **LoggingListener**
10. Results published to **ServiceBus** → **NotificationService**, **AnalyticsService**, **AuditService**
11. Everything saved to disk for persistence

**All 12 design patterns and 4 architectural patterns are exercised in every workflow run.**