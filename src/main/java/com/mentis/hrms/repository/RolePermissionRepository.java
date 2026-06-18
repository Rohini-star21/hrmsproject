package com.mentis.hrms.repository;

import com.mentis.hrms.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    // Existing method used for saving/updating
    Optional<RolePermission> findByRoleNameAndModuleNameAndFeatureName(String roleName, String moduleName, String featureName);

    // NEW METHOD: Used to fetch all saved permissions when the page loads
    List<RolePermission> findByRoleName(String roleName);
}