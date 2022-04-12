package com.redhat.service.rhose.shard.operator.cucumber.logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

import io.fabric8.kubernetes.api.model.events.v1.Event;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * Used to store events in a namespace.
 */
public class EventCollector {

    private EventCollector() {
    }

    /**
     * Store events in provided namespace
     * 
     * @param client
     * @param logFolderName Name of a folder used to store logs
     * @param namespace
     * @throws IOException In case event log cannot be stored on filesystem for any reason
     */
    public static void storeNamespaceEvents(OpenShiftClient client, String logFolderName, String namespace) throws IOException {
        Path logParentDirectory = LogConfig.getLogParentDirectory(logFolderName);
        Files.createDirectories(logParentDirectory);

        List<Event> events = client.events().v1().events().inNamespace(namespace).list().getItems();
        List<Map<EventEntry, String>> eventContent = new ArrayList<>();
        Map<EventEntry, String> eventHeader = new HashMap<>();
        eventHeader.put(EventEntry.LAST_SEEN, EventEntry.LAST_SEEN.toString());
        eventHeader.put(EventEntry.FIRST_SEEN, EventEntry.FIRST_SEEN.toString());
        eventHeader.put(EventEntry.COUNT, EventEntry.COUNT.toString());
        eventHeader.put(EventEntry.NAME, EventEntry.NAME.toString());
        eventHeader.put(EventEntry.KIND, EventEntry.KIND.toString());
        eventHeader.put(EventEntry.SUBOBJECT, EventEntry.SUBOBJECT.toString());
        eventHeader.put(EventEntry.TYPE, EventEntry.TYPE.toString());
        eventHeader.put(EventEntry.REASON, EventEntry.REASON.toString());
        eventHeader.put(EventEntry.ACTION, EventEntry.ACTION.toString());
        eventHeader.put(EventEntry.MESSAGE, EventEntry.MESSAGE.toString());
        eventContent.add(eventHeader);

        for (Event event : events) {
            Map<EventEntry, String> eventEntry = new HashMap<>();
            eventEntry.put(EventEntry.LAST_SEEN, getLastSeen(event));
            eventEntry.put(EventEntry.FIRST_SEEN, getFirstSeen(event));
            eventEntry.put(EventEntry.COUNT, event.getSeries() != null ? event.getSeries().getCount().toString() : "1");
            eventEntry.put(EventEntry.NAME, event.getRegarding().getName());
            eventEntry.put(EventEntry.KIND, event.getRegarding().getKind());
            eventEntry.put(EventEntry.SUBOBJECT, event.getRegarding().getFieldPath());
            eventEntry.put(EventEntry.TYPE, event.getType());
            eventEntry.put(EventEntry.REASON, event.getReason());
            eventEntry.put(EventEntry.ACTION, event.getAction());
            eventEntry.put(EventEntry.MESSAGE, event.getNote());
            eventContent.add(eventEntry);
        }
        for (Map<EventEntry, String> eventEntry : eventContent) {
            Files.write(logParentDirectory.resolve(Paths.get("events.log")),
                    getEventLogLine(eventEntry).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE);
        }
    }

    /**
     * @param event
     * @return Timestamp of the last event observation
     */
    private static String getLastSeen(Event event) {
        if (event.getSeries() != null && event.getSeries().getLastObservedTime() != null) {
            return event.getSeries().getLastObservedTime().getTime();
        }
        if (event.getEventTime() != null) {
            return event.getEventTime().getTime();
        }
        // Using deprecated values if new ones are not available - https://kubernetes.io/docs/reference/_print/#event-v125
        if (!Strings.isNullOrEmpty(event.getDeprecatedLastTimestamp())) {
            return event.getDeprecatedLastTimestamp();
        }
        return "N/A";
    }

    /**
     * @param event
     * @return Timestamp of the first event observation
     */
    private static String getFirstSeen(Event event) {
        if (event.getEventTime() != null) {
            return event.getEventTime().getTime();
        }
        // Using deprecated values if new ones are not available - https://kubernetes.io/docs/reference/_print/#event-v125
        if (!Strings.isNullOrEmpty(event.getDeprecatedFirstTimestamp())) {
            return event.getDeprecatedFirstTimestamp();
        }
        return "N/A";
    }

    /**
     * @param eventEntry
     * @return Event log line printing event entry in defined format
     */
    private static String getEventLogLine(Map<EventEntry, String> eventEntry) {
        StringBuilder eventLogLine = new StringBuilder();
        eventLogLine.append(String.format("%27s |", eventEntry.get(EventEntry.LAST_SEEN)));
        eventLogLine.append(String.format(" %27s |", eventEntry.get(EventEntry.FIRST_SEEN)));
        eventLogLine.append(String.format(" %5s |", eventEntry.get(EventEntry.COUNT)));
        eventLogLine.append(String.format(" %40s |", eventEntry.get(EventEntry.NAME)));
        eventLogLine.append(String.format(" %20s |", eventEntry.get(EventEntry.KIND)));
        eventLogLine.append(String.format(" %30s |", eventEntry.get(EventEntry.SUBOBJECT)));
        eventLogLine.append(String.format(" %10s |", eventEntry.get(EventEntry.TYPE)));
        eventLogLine.append(String.format(" %25s |", eventEntry.get(EventEntry.REASON)));
        eventLogLine.append(String.format(" %10s |", eventEntry.get(EventEntry.ACTION)));
        eventLogLine.append(String.format(" %s\n", eventEntry.get(EventEntry.MESSAGE)));
        return eventLogLine.toString();
    }

    private enum EventEntry {
        LAST_SEEN,
        FIRST_SEEN,
        COUNT,
        NAME,
        KIND,
        SUBOBJECT,
        TYPE,
        REASON,
        ACTION,
        MESSAGE;
    }
}
