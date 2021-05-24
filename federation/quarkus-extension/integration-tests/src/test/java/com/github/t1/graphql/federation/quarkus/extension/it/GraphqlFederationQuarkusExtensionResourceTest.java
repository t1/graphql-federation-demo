package com.github.t1.graphql.federation.quarkus.extension.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class GraphqlFederationQuarkusExtensionResourceTest {

    @Test
    public void testNormalEndpoint() {
        given()
            .when().get("/graphql-federation-quarkus-extension")
            .then()
            .statusCode(200)
            .body(is("hi graphql-federation-quarkus-extension"));
    }

    @Test
    public void testExtensionEndpoint() {
        given()
            .when().get("/q/graphql-federation")
            .then()
            .statusCode(200)
            .body(containsString("entities:"));
    }
}
