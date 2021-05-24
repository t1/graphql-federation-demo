package com.github.t1.graphql.federation.quarkus.extension;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public class DevEndpointHandler implements Handler<RoutingContext> {
    private final List<String> entities;

    @SuppressWarnings("unused")
    public DevEndpointHandler() { this.entities = null; }

    public DevEndpointHandler(List<String> entities) { this.entities = entities; }

    @Override public void handle(RoutingContext routingContext) {
        var out = new StringBuilder();
        out.append("entities:\n");
        if (entities == null) out.append("none");
        else entities.forEach(entity -> out.append("> ").append(entity).append("\n"));
        routingContext.response().end(out.toString());
    }
}
