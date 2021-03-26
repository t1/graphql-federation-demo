package graphql.federation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <b><code>directive @external on FIELD_DEFINITION</code></b>
 * <p>
 * The <code>@external</code> directive is used to mark a field as owned by another service.
 * This allows service A to use fields from service B while also knowing at runtime the types of that field.
 */
@Retention(RUNTIME)
public @interface External {
}
