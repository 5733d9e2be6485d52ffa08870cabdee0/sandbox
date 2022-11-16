package com.redhat.service.smartevents.performance.webhook.services;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.performance.webhook.exceptions.BridgeNotFoundException;
import com.redhat.service.smartevents.performance.webhook.exceptions.EventNotFoundException;
import com.redhat.service.smartevents.performance.webhook.models.Event;

import io.quarkus.panache.common.Parameters;

@ApplicationScoped
public class WebhookServiceImpl implements WebhookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookServiceImpl.class);

    @Override
    public List<Event> findAll() {
        return Event.listAll();
    }

    @Override
    public Event getEvent(Long id) throws EventNotFoundException {
        Optional<Event> event = Event.findByIdOptional(id);
        return event.orElseThrow(() -> new EventNotFoundException(id));
    }

    @Override
    public List<Event> getEventsByBridgeId(String bridgeId) throws BridgeNotFoundException {
        List<Event> events = Event.list("bridgeId = :bridgeId", Parameters.with("bridgeId", bridgeId));
        return Optional.ofNullable(events).orElseThrow(() -> new BridgeNotFoundException(bridgeId));
    }

    @Override
    @Transactional
    public Event create(Event event) {
        event.persist();
        LOGGER.info("event persisted with id {}", event.getId());
        return event;
    }

    @Override
    public Long countEventsReceived(String bridgeId) {
        return Event.count("bridgeId = :bridgeId",
                Parameters.with("bridgeId", bridgeId));
    }

    @Override
    @Transactional
    public Event delete(Long id) throws EventNotFoundException {
        Event event = getEvent(id);
        event.delete();
        LOGGER.info("event deleted {}", event.getId());
        return event;
    }

    @Override
    @Transactional
    public long deleteAll() {
        long deletedRows = Event.deleteAll();
        LOGGER.info("rows deleted {}", deletedRows);
        return deletedRows;
    }
}