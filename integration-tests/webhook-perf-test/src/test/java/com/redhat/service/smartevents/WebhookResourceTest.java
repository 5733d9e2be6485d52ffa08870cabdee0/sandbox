package com.redhat.service.smartevents;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.redhat.service.smartevents.performance.webhook.exceptions.BridgeNotFoundException;
import com.redhat.service.smartevents.performance.webhook.exceptions.EventNotFoundException;
import com.redhat.service.smartevents.performance.webhook.models.Event;
import com.redhat.service.smartevents.performance.webhook.models.WebhookRequest;
import com.redhat.service.smartevents.performance.webhook.services.WebhookService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class WebhookResourceTest {

    @InjectMock
    WebhookService webhookService;

    @BeforeEach
    void init() {
        RestAssured.basePath = "/webhook";
    }

    @Test
    void testCountEventsReceived() {
        Mockito.when(webhookService.countEventsReceived(any()))
                .thenReturn(0L)
                .thenReturn(2L);

        Long count = given()
            .when()
            .get("/events/bridgeId-3/count")
            .then().statusCode(HttpStatus.SC_OK)
            .extract().as(Long.class);
        assertThat(count, is(0L));

        count = given()
                .when()
                .get("/events/bridgeId-1/count")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().as(Long.class);
        assertThat(count, is(2L));
    }

    @Test
    void testGetAll() {
        given()
            .when()
            .get("/events")
            .then().statusCode(HttpStatus.SC_OK)
            .body("", hasSize(0));

        List<Event> events = new ArrayList<>();
        events.add(new Event().setBridgeId("bridgeId-1"));
        events.add(new Event().setBridgeId("bridgeId-2"));
        Mockito.when(webhookService.findAll()).thenReturn(events);

        String results = given()
                .when()
                .get("/events")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().asString();

        Event[] resultEvents = Json.decodeValue(results, Event[].class);
        assertThat(resultEvents, arrayContaining(events.toArray()));
    }

    @Test
    void testGetEventById() throws EventNotFoundException {
        Event event = new Event().setBridgeId("bridgeId-1");
        Mockito.when(webhookService.getEvent(1L)).thenThrow(new EventNotFoundException(1L)).thenReturn(event);
        given()
            .when()
            .get("/events/bridgeId-1/1")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);

        String result = given()
                .when()
                .get("/events/bridgeId-1/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .body()
                .asString();
        Event resultEvent = Json.decodeValue(result, Event.class);
        assertThat(resultEvent, Matchers.is(event));
    }

    @Test
    void testGetEventsByBridgeId() throws BridgeNotFoundException {
        List<Event> events = new ArrayList<>();
        events.add(new Event().setBridgeId("bridgeId-1"));
        events.add(new Event().setBridgeId("bridgeId-1"));
        Mockito.when(webhookService.getEventsByBridgeId("bridgeId-1")).thenThrow(new BridgeNotFoundException("bridgeId-1")).thenReturn(events);
        given()
                .when()
                .get("/events/bridgeId-1")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);

        String result = given()
                .when()
                .get("/events/bridgeId-1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();
        Event[] resultEvents = Json.decodeValue(result, Event[].class);
        assertThat(resultEvents, arrayContaining(events.toArray()));
    }

    @Test
    void testConsumeEvent() {
        Event expectedEvent = new Event();
        ZonedDateTime submittedAt = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime receivedAt = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(5L);
        expectedEvent.setBridgeId("bridgeId-1");
        expectedEvent.setSubmittedAt(submittedAt);
        expectedEvent.setReceivedAt(receivedAt);

        Mockito.when(webhookService.create(any())).thenReturn(expectedEvent);

        WebhookRequest request = new WebhookRequest("bridgeId-1");
        request.setSubmittedAt(submittedAt);
        String result = given()
                .when()
                .body(Json.encode(request))
                .contentType(ContentType.JSON)
                .log().all()
                .post("/events")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().asString();
        Event response = Json.decodeValue(result, Event.class);

        assertThat(response.getBridgeId(), is(expectedEvent.getBridgeId()));
        assertThat(response.getMessage(), nullValue());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.from(ZoneOffset.UTC));
        assertThat(formatter.format(response.getSubmittedAt()), is(formatter.format(submittedAt)));
        assertThat(formatter.format(response.getReceivedAt()), is(formatter.format(receivedAt)));
    }

    @Test
    void testDelete() throws EventNotFoundException {
        Event expectEvent = new Event();
        expectEvent.setId(1L);
        Mockito.when(webhookService.delete(1L)).thenReturn(expectEvent);

        String result = given()
                .when()
                .contentType(ContentType.JSON)
                .delete("/events/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();
        Event response = Json.decodeValue(result, Event.class);
        assertThat(response, Matchers.is(expectEvent));
    }

    @Test
    void testDeleteAll() {
        Mockito.when(webhookService.deleteAll()).thenReturn(1L);

        String result = given()
                .when()
                .contentType(ContentType.JSON)
                .delete("/events")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();
        Long response = Json.decodeValue(result, Long.class);

        assertThat(response, Matchers.is(1L));
    }

}