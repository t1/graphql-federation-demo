package com.github.t1.graphql.federation.api;

import io.smallrye.graphql.api.Directive;
import org.eclipse.microprofile.graphql.Description;

import java.lang.annotation.Retention;

import static io.smallrye.graphql.api.DirectiveLocation.INTERFACE;
import static io.smallrye.graphql.api.DirectiveLocation.OBJECT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** <b><code>directive @extends on OBJECT | INTERFACE</code></b> */
@Directive(on = {OBJECT, INTERFACE})
@Description("Some libraries such as graphql-java don't have native support for type extensions in their printer. " +
    "Apollo Federation supports using an @extends directive in place of extend type to annotate type references.")
@Retention(RUNTIME)
public @interface Extends {
}
