package graphql.federation;

import io.smallrye.graphql.api.Directive;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;

import java.lang.annotation.Retention;

import static io.smallrye.graphql.api.DirectiveLocation.FIELD_DEFINITION;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** <b><code>directive @requires(fields: _FieldSet!) on FIELD_DEFINITION</code></b> */
@Directive(on = FIELD_DEFINITION)
@Description("The @requires directive is used to annotate the required input fieldset from a base type for a resolver. " +
    "It is used to develop a query plan where the required fields may not be needed by the client, " +
    "but the service may need additional information from other services.")
@Retention(RUNTIME)
public @interface Requires {
    @NonNull String[] fields();
}
