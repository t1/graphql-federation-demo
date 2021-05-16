package com.github.t1.graphql.federation.quarkus.extension.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

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
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("Hello"));
    }
}
