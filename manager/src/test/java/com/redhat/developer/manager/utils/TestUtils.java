package com.redhat.developer.manager.utils;

import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.manager.api.models.requests.ConnectorRequest;

import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class TestUtils {

    public static Response getConnectors() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/connectors");
    }

    public static Response createConnector(ConnectorRequest request) {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(request)
                .post("/api/v1/connectors");
    }

    public static Response getConnectorsToDeploy() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/shard/connectors/toDeploy");
    }

    public static Response updateConnector(ConnectorDTO connectorDTO) {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(connectorDTO)
                .post("/api/v1/shard/connectors/toDeploy");
    }
}
