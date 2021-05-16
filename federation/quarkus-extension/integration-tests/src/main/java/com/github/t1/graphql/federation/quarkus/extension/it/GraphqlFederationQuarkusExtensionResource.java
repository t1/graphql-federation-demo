package com.github.t1.graphql.federation.quarkus.extension.it;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/graphql-federation-quarkus-extension")
@ApplicationScoped
public class GraphqlFederationQuarkusExtensionResource {
    @GET
    public String hello() {
        return "hi graphql-federation-quarkus-extension";
    }
}
