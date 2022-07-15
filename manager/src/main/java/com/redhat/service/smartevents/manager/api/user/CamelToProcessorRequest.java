package com.redhat.service.smartevents.manager.api.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.manager.api.models.requests.CamelProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;

public class CamelToProcessorRequest {

    private CamelProcessorRequest camelProcessorRequest;

    ObjectMapper mapper = new ObjectMapper();

    public CamelToProcessorRequest(CamelProcessorRequest camelProcessorRequest) {
        this.camelProcessorRequest = camelProcessorRequest;
    }

    public ProcessorRequest convert() {

        ArrayNode flows = camelProcessorRequest.getFlow();
        List<JsonNode> tos = flows.findValues("to");

        Set<Action> actions = new HashSet<>();
        for (JsonNode to : tos) {
            ObjectNode toObjectNode = (ObjectNode) to;
            Action action = actionFromTo(toObjectNode);
            toObjectNode.remove("parameters");
            toObjectNode.set("uri", new TextNode(action.getName()));
            actions.add(action);
        }

        ProcessorRequest processorRequest = new ProcessorRequest(camelProcessorRequest.getName(), (Action) null);

        Processing processing = new Processing();
        processing.setType("cameldsl_0.1");

        ObjectNode inputFlow = (ObjectNode) flows.get(0);

        ObjectNode spec = mapper.createObjectNode();
        processing.setSpec(spec);
        spec.set("flow", inputFlow);

        processorRequest.setProcessing(processing);
        processorRequest.setActions(actions);

        return processorRequest;
    }

    private Action actionFromTo(ObjectNode toObjectNode) {

        String[] uri = toObjectNode.get("uri").asText().split(":");

        Action action = new Action();
        action.setName(uri[2]);
        action.setType(uri[1]);
        ObjectNode inputParameters = (ObjectNode) toObjectNode.get("parameters");
        HashMap<String, String> parameters = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> iterator = inputParameters.fields(); iterator.hasNext();) {
            Map.Entry<String, JsonNode> kv = iterator.next();
            parameters.put(kv.getKey(), kv.getValue().asText());
        }
        action.setMapParameters(parameters);
        return action;
    }
}
