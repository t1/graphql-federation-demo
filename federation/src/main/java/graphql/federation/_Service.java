package graphql.federation;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;

public class _Service {
    private final String sdl;

    public _Service(String sdl) { this.sdl = sdl; }

    @Description("The sdl representing the federated service capabilities. " +
        "Includes federation directives, removes federation types, and includes rest of full schema after schema directives have been applied")
    public @NonNull String getSdl() { return sdl; }
}
