package com.tuckersoft.branchengine.domain;

import java.util.Set;

/** Los dos unicos roles del sistema. Se guardan como String en la columna 'role'. */
public final class Roles {

    public static final String USER = "ROLE_USER";
    public static final String ADMIN = "ROLE_ADMIN";

    public static final Set<String> VALIDOS = Set.of(USER, ADMIN);

    private Roles() {
    }
}
