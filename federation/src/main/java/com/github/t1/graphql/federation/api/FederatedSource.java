package com.github.t1.graphql.federation.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A federated resolver is a method that resolves a batch of data in a federated request.
 * The input type and the output type are both lists of the same type, annotated as {@link Extends @extends},
 * and it adds those fields that are not annotated as {@link External @external}.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface FederatedSource {
}
