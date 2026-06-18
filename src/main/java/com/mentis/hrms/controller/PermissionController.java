package com.mentis.hrms.controller;

import com.mentis.hrms.model.RolePermission;
import com.mentis.hrms.repository.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private RolePermissionRepository permissionRepo;

    @PostMapping("/save")
    public ResponseEntity<?> savePermissions(@RequestBody List<RolePermission> permissions) {
        for (RolePermission perm : permissions) {
            // Check if permission rule already exists in DB
            Optional<RolePermission> existing = permissionRepo.findByRoleNameAndModuleNameAndFeatureName(
                    perm.getRoleName(), perm.getModuleName(), perm.getFeatureName());

            if (existing.isPresent()) {
                // If it exists, just update the access level (e.g., changed from 'full' to 'disable')
                RolePermission toUpdate = existing.get();
                toUpdate.setAccessLevel(perm.getAccessLevel());
                permissionRepo.save(toUpdate);
            } else {
                // If it doesn't exist, create a new row
                permissionRepo.save(perm);
            }
        }
        return ResponseEntity.ok().body("{\"success\": true}");
    }
    @GetMapping("/{roleName}")
    public ResponseEntity<List<RolePermission>> getPermissionsByRole(@PathVariable String roleName) {
        // Fetch all permissions for the requested role from the database
        List<RolePermission> permissions = permissionRepo.findByRoleName(roleName);
        return ResponseEntity.ok(permissions);
    }
}
