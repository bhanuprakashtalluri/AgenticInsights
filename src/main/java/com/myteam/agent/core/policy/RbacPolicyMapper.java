package com.myteam.agent.core.policy;

import java.util.Set;

public class RbacPolicyMapper {

    // For now, allow all roles to execute agent requests
    // In future phases, this can be refined based on intent or tool
    public static boolean canExecuteAgent(String role) {
        return Set.of("ADMIN", "MANAGER", "TEAMLEAD", "EMPLOYEE").contains(role.toUpperCase());
    }

    // Placeholder for future tool-specific permissions
    public static boolean canUseTool(String role, String toolName) {
        // For mock tools, allow all
        return true;
    }
}
