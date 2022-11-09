package com.redhat.service.smartevents.performance.webhook.services;

import java.util.List;

import com.redhat.service.smartevents.performance.webhook.exceptions.BridgeNotFoundException;
import com.redhat.service.smartevents.performance.webhook.exceptions.EventNotFoundException;
import com.redhat.service.smartevents.performance.webhook.models.Event;

public interface WebhookService {
    List<Event> findAll();

    List<Event> getEventsByBridgeId(String bridgeId) throws BridgeNotFoundException;

    Event getEvent(Long id) throws EventNotFoundException;

    Event create(Event event);

    Long countEventsReceived(String bridgeId);

    Event delete(Long id) throws EventNotFoundException;

    long deleteAll();
}
