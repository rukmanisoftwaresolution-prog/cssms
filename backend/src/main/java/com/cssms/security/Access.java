package com.cssms.security;

/** SpEL expressions for method security, mirroring the role matrix in docs/API.md. */
public final class Access {

    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String ADMIN_MANAGER = "hasAnyRole('ADMIN','MANAGER')";
    public static final String ALL_ROLES = "hasAnyRole('ADMIN','MANAGER','SALES_EXECUTIVE')";

    private Access() {
    }
}
