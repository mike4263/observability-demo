package com.example.obsdemo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ObservabilityDemoTest {

    @Test
    void healthEndpointIsAvailable() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200);
    }

    @Test
    void logEndpointEmitsLogs() {
        given()
                .queryParam("level", "INFO")
                .queryParam("message", "test log")
                .when().post("/api/logs")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("count", is(1))
                .body("level", equalTo("INFO"));
    }

    @Test
    void cpuLoadEndpointAcceptsRequest() {
        given()
                .queryParam("threads", 1)
                .queryParam("durationSeconds", 1)
                .when().post("/api/cpu/load")
                .then()
                .statusCode(202)
                .contentType(ContentType.JSON)
                .body("accepted", is(true));
    }

    @Test
    void traceEndpointEmitsSimpleSpan() {
        given()
                .queryParam("name", "test-trace")
                .when().post("/api/traces/simple")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("type", equalTo("simple"))
                .body("spanCount", is(1));
    }

    @Test
    void traceEndpointEmitsNestedSpans() {
        given()
                .when().post("/api/traces/nested")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("type", equalTo("nested"))
                .body("spanCount", is(4));
    }
}
