package com.github.t1.graphql.federation.quarkus.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import org.acme.greeting.extension.GreetingExtensionServlet;

class GraphqlFederationQuarkusExtensionProcessor {

    private static final String FEATURE = "graphql-federation-quarkus-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem createServlet() {
        return ServletBuildItem.builder("greeting-extension", GreetingExtensionServlet.class.getName())
            .addMapping("/greeting")
            .build();
    }
}
