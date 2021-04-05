package graphql.federation;

import io.smallrye.graphql.api.Directive;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;

import java.lang.annotation.Retention;

import static io.smallrye.graphql.api.DirectiveLocation.FIELD_DEFINITION;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** <b><code>directive @provides(fields: _FieldSet!) on FIELD_DEFINITION</code></b> */
@Directive(on = FIELD_DEFINITION)
@Description("The @provides directive is used to annotate the expected returned fieldset from a field on a base type " +
    "that is guaranteed to be selectable by the gateway.")
@Retention(RUNTIME)
public @interface Provides {
    @NonNull String[] fields();
}
