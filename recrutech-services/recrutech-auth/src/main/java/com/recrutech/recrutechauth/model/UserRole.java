package com.recrutech.recrutechauth.model;

import lombok.Getter;

/**
 * Enum representing different user roles in the system.
 * Each role has specific authorities and permissions.
 */
@Getter
public enum UserRole {
    COMPANY_ADMIN("ROLE_COMPANY_ADMIN"),
    HR("ROLE_HR"),
    APPLICANT("ROLE_APPLICANT");

    private final String authority;

    UserRole(String authority) {
        this.authority = authority;
    }

    @Override
    public String toString() {
        return authority;
    }
}