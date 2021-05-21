package com.github.t1.graphql.federation.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A federated resolver method is the federated equivalent to a 'local' resolver method (with a parameter annotated as
 * <code>{@link org.eclipse.microprofile.graphql.Source @Source}</code>), i.e. it adds a field with the name of the method
 * and the type of the method return type to the source object.
 * The class of the source parameter must be annotated as <code>{@link Extends @Extends}</code>
 * and have at least one field annotated as <code>{@link External @External}</code>.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface FederatedSource {
}
