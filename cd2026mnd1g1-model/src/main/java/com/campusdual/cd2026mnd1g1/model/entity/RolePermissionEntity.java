package com.campusdual.cd2026mnd1g1.model.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol_permiso")
public class RolePermissionEntity {

    @EmbeddedId
    private RolePermissionId id;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private RoleEntity role;

    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private PermissionEntity permission;

    public RolePermissionId getId() { return id; }
    public void setId(RolePermissionId id) { this.id = id; }
    public RoleEntity getRole() { return role; }
    public void setRole(RoleEntity role) { this.role = role; }
    public PermissionEntity getPermission() { return permission; }
    public void setPermission(PermissionEntity permission) { this.permission = permission; }
}
