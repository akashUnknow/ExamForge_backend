package com.examforge.role.domain;

/**
 * The fixed set of platform roles. This is a closed enum, not an
 * admin-creatable list - new roles require a code change and migration,
 * matching the RBAC model specified for the platform.
 */
public enum RoleName {
    USER,
    CONTENT_CREATOR,
    REVIEWER,
    ADMIN,
    SUPER_ADMIN
}
