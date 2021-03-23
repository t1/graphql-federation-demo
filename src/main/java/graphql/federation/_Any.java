package graphql.federation;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.graphql.Input;

/**
 * The <code>_Any</code> scalar is used to pass representations of entities from external services
 * into the root <code>_entities</code> field for execution. Validation of the <code>_Any</code> scalar is done
 * by matching the <code>__typename</code> and <code>&#64;external</code> fields defined in the schema.
 */
@Input("_Any")
@Getter @Setter
public class _Any {
    public String id; // TODO _Any must be an empty scalar
}
