package com.github.t1.graphql.federation.quarkus.extension.deployment;

import com.github.t1.graphql.federation.quarkus.extension.EntitiesRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.smallrye.graphql.federation.impl.Federation;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class GraphqlFederationQuarkusExtensionProcessor {
    private static final String FEATURE = "graphql-federation";
    private static final Logger LOG = Logger.getLogger(GraphqlFederationQuarkusExtensionProcessor.class.getName());
    private static final DotName KEY = DotName.createSimple("io.smallrye.graphql.federation.api.Key");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    EntitiesBuildItem findKeyDirectives(ApplicationIndexBuildItem index) {
        var entities = new EntitiesBuildItem();
        for (AnnotationInstance keyAnnotation : index.getIndex().getAnnotations(KEY)) {
            var annotationTarget = keyAnnotation.target().asClass();
            LOG.info("found " + keyAnnotation + " on " + annotationTarget);
            entities.add(annotationTarget.name().toString());
        }
        return entities;
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    RouteBuildItem devModeRoute(
            NonApplicationRootPathBuildItem nonApp,
            EntitiesBuildItem entitiesBuildItem,
            EntitiesRecorder entitiesRecorder
    ) {
        return nonApp.routeBuilder()
                .route(FEATURE)
                .handler(entitiesRecorder.createHandler(entitiesBuildItem.getEntities()))
                .displayOnNotFoundPage()
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(Federation.class);
    }
}
