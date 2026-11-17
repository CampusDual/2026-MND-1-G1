package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.RolePermissionEntity;
import com.campusdual.cd2026mnd1g1.model.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

    @Query("select rolePermission.permission.code from RolePermissionEntity rolePermission "
            + "where rolePermission.role.id = :roleId")
    List<String> findPermissionCodesByRoleId(@Param("roleId") Long roleId);
}
