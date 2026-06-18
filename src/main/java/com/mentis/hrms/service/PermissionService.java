package com.mentis.hrms.service;

import com.mentis.hrms.model.RolePermission;
import com.mentis.hrms.repository.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("permissionChecker")
public class PermissionService {

    @Autowired
    private RolePermissionRepository repo;

    public boolean hasAccess(Boolean isSuperAdmin, String moduleName, String featureName, String requiredLevel) {
        // 1. Safety Check: If parameters are missing, deny access instead of crashing
        if (moduleName == null || featureName == null) return false;

        // 2. Super Admin bypass
        if (isSuperAdmin != null && isSuperAdmin) {
            return true;
        }

        // 3. Fetch current permission for HR
        String roleName = "hr";
        RolePermission perm = repo.findByRoleNameAndModuleNameAndFeatureName(roleName, moduleName, featureName)
                .orElse(null);

        if (perm == null || perm.getAccessLevel() == null) {
            return false;
        }

        String currentLevel = perm.getAccessLevel();

        // 4. Access Hierarchy Logic
        if (currentLevel.equals("disable")) return false;
        if (currentLevel.equals("full")) return true; // Full access allows everything

        if (requiredLevel.equals("view")) {
            // view required: allows 'view', 'edit', or 'full'
            return currentLevel.equals("view") || currentLevel.equals("edit");
        }
        else if (requiredLevel.equals("edit")) {
            // edit required: only allows 'edit' (since 'full' was handled above)
            return currentLevel.equals("edit");
        }

        return false;
    }
}