package com.redhat.service.smartevents.performance.webhook.services;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.performance.webhook.exceptions.BridgeNotFoundException;
import com.redhat.service.smartevents.performance.webhook.exceptions.EventNotFoundException;
import com.redhat.service.smartevents.performance.webhook.models.Event;
import io.quarkus.panache.common.Parameters;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class WebhookServiceImpl implements WebhookService {

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
        log.info("event persisted with id {}", event.getId());
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
        log.info("event deleted {}", event.getId());
        return event;
    }

    @Override
    @Transactional
    public long deleteAll() {
        long deletedRows = Event.deleteAll();
        log.info("rows deleted {}", deletedRows);
        return deletedRows;
    }
}