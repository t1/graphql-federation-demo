package com.github.t1.graphql.federation.quarkus.extension.test;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

public class GraphqlFederationQuarkusExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void shouldReturnDev() {
        when()
            .get("/q/graphql-federation")
            .then()
            .statusCode(200)
            .body(containsString("entities:"));
    }
}
