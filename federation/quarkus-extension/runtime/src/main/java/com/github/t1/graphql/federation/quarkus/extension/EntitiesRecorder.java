package com.github.t1.graphql.federation.quarkus.extension;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

@Recorder
public class EntitiesRecorder {
    public Handler<RoutingContext> createHandler(List<String> entities) {
        return new DevEndpointHandler(entities);
    }
}
