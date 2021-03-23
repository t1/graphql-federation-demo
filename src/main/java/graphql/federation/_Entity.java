package graphql.federation;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.graphql.Description;

/**
 * A union of all types that use the <code>&#64;key</code> directive,
 * including both types native to the schema and extended types.
 *
 * TODO dynamically determine all types that use the @key directive and turn it into an union
 */
@Getter @Setter
@Description("a union of all types that use the @key directive")
public class _Entity {
    public String dummy;
}
