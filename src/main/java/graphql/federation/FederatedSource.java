package graphql.federation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A federated resolver is a method that resolves a batch of data in a federated request.
 * The input type and the output type are both lists of the same type, annotated as {@link graphql.federation.Extends @extends},
 * and it adds those fields that are not annotated as {@link graphql.federation.External @external}.
 */
@Retention(RUNTIME)
public @interface FederatedSource {
}
