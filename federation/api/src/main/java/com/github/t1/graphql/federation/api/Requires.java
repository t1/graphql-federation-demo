package com.github.t1.graphql.federation.api;

import io.smallrye.graphql.api.Directive;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;

import java.lang.annotation.Retention;

import static io.smallrye.graphql.api.DirectiveLocation.FIELD_DEFINITION;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** <b><code>directive @requires(fields: _FieldSet!) on FIELD_DEFINITION</code></b> */
@Directive(on = FIELD_DEFINITION)
@Description("In order to resolve the annotated field, this service needs these additional `@external` fields, " +
    "even when the client didn't request them.")
@Retention(RUNTIME)
public @interface Requires {
    @NonNull String[] fields();
}
