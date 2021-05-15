package com.github.t1.graphql.federation.api;

import io.smallrye.graphql.api.Directive;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;

import java.lang.annotation.Retention;

import static io.smallrye.graphql.api.DirectiveLocation.INTERFACE;
import static io.smallrye.graphql.api.DirectiveLocation.OBJECT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** <b><code>directive @key(fields: _FieldSet!) on OBJECT | INTERFACE</code></b> */
@Directive(on = {OBJECT, INTERFACE})
@Description("The @key directive is used to indicate a combination of fields that can be used to uniquely identify " +
    "and fetch an object or interface.")
@Retention(RUNTIME)
public @interface Key {
    @NonNull String[] fields();
}
