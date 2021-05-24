package com.github.t1.graphql.federation.quarkus.extension.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

public class GraphqlFederationQuarkusExtensionDevModeTest {

    // Start hot reload (DevMode) test with your extension loaded
    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void shouldReturnExtensionGreeting() {
        when()
            .get("/q/graphql-federation")
            .then()
            .statusCode(200)
            .body(containsString("entities:"));
    }
}
