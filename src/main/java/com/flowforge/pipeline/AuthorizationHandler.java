package com.flowforge.pipeline;

import java.util.Set;

/*Pipeline handler that checks authorization before workflow execution. */
public class AuthorizationHandler implements PipelineHandler {

    private final String currentUser;
    private final Set<String> restrictedWorkflows;

    /**
     * @param currentUser        the user attempting execution
     * @param restrictedWorkflows workflow names that require admin access
     */
    public AuthorizationHandler(String currentUser, Set<String> restrictedWorkflows) {
        this.currentUser = currentUser;
        this.restrictedWorkflows = restrictedWorkflows;
    }

    @Override
    public String getName() {
        return "AuthorizationHandler";
    }

    @Override
    public void handle(PipelineContext context, PipelineHandler next) {
        String workflowName = context.getWorkflow().getName();
        context.addLog("[AuthorizationHandler] Checking permissions for user '"
                + currentUser + "' on workflow '" + workflowName + "'");

        if (restrictedWorkflows.contains(workflowName) && !"admin".equals(currentUser)) {
            context.abort("User '" + currentUser
                    + "' does not have permission to execute restricted workflow '"
                    + workflowName + "'");
            return;
        }

        context.addLog("[AuthorizationHandler] Access granted");
        next.handle(context, null);
    }
}