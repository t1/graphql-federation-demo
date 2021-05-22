package com.github.t1.graphql.federation.quarkus.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import org.acme.greeting.extension.GreetingExtensionServlet;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

class GraphqlFederationQuarkusExtensionProcessor {
    private static final Logger LOG = Logger.getLogger(GraphqlFederationQuarkusExtensionProcessor.class.getName());
    private static final String FEATURE = "graphql-federation";

    private static final DotName KEY = DotName.createSimple("com.github.t1.graphql.federation.api.Key");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void findKeyDirectives(ApplicationIndexBuildItem index) {
        LOG.debug("scanning for @Key annotations");
        for (AnnotationInstance keyAnnotation : index.getIndex().getAnnotations(KEY)) {
            var annotationTarget = keyAnnotation.target().asClass();
            LOG.debug("found " + keyAnnotation + " annotation on " + annotationTarget);
        }
    }

    // @BuildStep
    // RouteBuildItem myExtensionRoute(NonApplicationRootPathBuildItem nonApp) {
    //     return nonApp.routeBuilder()
    //         .route("custom-endpoint")
    //         .handler(new MyCustomHandler())
    //         .displayOnNotFoundPage()
    //         .build();
    // }

    @BuildStep
    ServletBuildItem createServlet() {
        return ServletBuildItem.builder("greeting-extension", GreetingExtensionServlet.class.getName())
            .addMapping("/greeting")
            .build();
    }
}
