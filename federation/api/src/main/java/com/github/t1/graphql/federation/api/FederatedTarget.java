package com.github.t1.graphql.federation.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A federated target resolver gets a stub (i.e. an instance with only the {@link Key key} fields set),
 * that the service received from the federation gateway, and fills in the missing (non-external) fields.
 * <p>
 * The method must return <code>void</code>.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface FederatedTarget {
}
