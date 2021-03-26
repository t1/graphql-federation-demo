package graphql.federation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <b><code>directive @extends on OBJECT | INTERFACE</code></b>
 * <p>
 * Some libraries such as graphql-java don't have native support for type extensions in their printer.
 * Apollo Federation supports using an @extends directive in place of extend type to annotate type references.
 */
@Retention(RUNTIME)
public @interface Extends {
}
