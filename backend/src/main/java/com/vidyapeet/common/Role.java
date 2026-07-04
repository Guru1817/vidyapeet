package com.vidyapeet.common;

/**
 * Platform roles. {@code SUPER_ADMIN} is global (no institute); the others are
 * always scoped to a single institute.
 */
public enum Role {
    SUPER_ADMIN,
    INSTITUTE_ADMIN,
    STUDENT
}
