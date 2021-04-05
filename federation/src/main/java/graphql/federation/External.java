package graphql.federation;

import io.smallrye.graphql.api.Directive;
import org.eclipse.microprofile.graphql.Description;

import java.lang.annotation.Retention;

import static io.smallrye.graphql.api.DirectiveLocation.FIELD_DEFINITION;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** <b><code>directive @external on FIELD_DEFINITION</code></b> */
@Directive(on = FIELD_DEFINITION)
@Description("The @external directive is used to mark a field as owned by another service. " +
    "This allows service A to use fields from service B while also knowing at runtime the types of that field.")
@Retention(RUNTIME)
public @interface External {
}
