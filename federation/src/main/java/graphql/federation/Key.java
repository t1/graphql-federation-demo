package graphql.federation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <b><code>directive @key(fields: _FieldSet!) on OBJECT | INTERFACE</code></b>
 * <p>
 * The <code>@key</code> directive is used to indicate a combination of fields that can be used to uniquely identify
 * and fetch an object or interface.
 */
@Retention(RUNTIME)
public @interface Key {
    String fields();
}
