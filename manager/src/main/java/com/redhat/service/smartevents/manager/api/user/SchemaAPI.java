package com.redhat.service.smartevents.manager.api.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorCatalogResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorSchemaEntryResponse;

import io.quarkus.security.Authenticated;

@Tag(name = "Schema Catalog", description = "The API that provide the catalog of the available action/source processors definition and their JSON schema.")
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path(APIConstants.SCHEMA_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SchemaAPI {

    private static final String ACTIONS_DIR_PATH = "/schemas/actions/";
    private static final String SOURCES_DIR_PATH = "/schemas/sources/";
    private static final String JSON_FILE_EXTENSION = ".json";
    private static final String CATALOG_FILENAME = "catalog.json";
    private static final String ACTION_TYPE = "action";
    private static final String SOURCE_TYPE = "source";

    private List<String> actions;
    private List<String> sources;

    @Inject
    ObjectMapper mapper;

    @PostConstruct
    void init() throws IOException {
        actions = mapper.readValue(readFile(ACTIONS_DIR_PATH, CATALOG_FILENAME), new TypeReference<>() {
        });
        sources = mapper.readValue(readFile(SOURCES_DIR_PATH, CATALOG_FILENAME), new TypeReference<>() {
        });
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessorCatalogResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Get processor catalog", description = "Get the processor catalog with all the available sources and actions.")
    @GET
    public Response getCatalog() {
        List<ProcessorSchemaEntryResponse> entries = new ArrayList<>();
        entries.addAll(
                actions.stream().map(x -> new ProcessorSchemaEntryResponse(x, ACTION_TYPE, APIConstants.ACTIONS_SCHEMA_API_BASE_PATH + x))
                        .collect(Collectors.toList()));
        entries.addAll(sources.stream().map(x -> new ProcessorSchemaEntryResponse(x, SOURCE_TYPE, APIConstants.SOURCES_SCHEMA_API_BASE_PATH + x))
                .collect(Collectors.toList()));
        ProcessorCatalogResponse response = new ProcessorCatalogResponse(entries);
        return Response.ok(response).build();
    }

    @APIResponses(value = {
            // we can't use JsonSchema.class because of https://github.com/swagger-api/swagger-ui/issues/8046
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Get source processor schema", description = "Get the source processor JSON schema.")
    @GET
    @Path("/sources/{name}")
    public Response getSourceProcessorSchema(@PathParam("name") String name) {
        if (!sources.contains(name)) {
            throw new ItemNotFoundException(String.format("The processor json schema '%s' is not in the catalog.", name));
        }

        return Response.ok(readFile(SOURCES_DIR_PATH, name)).build();
    }

    @APIResponses(value = {
            // we can't use JsonSchema.class because of https://github.com/swagger-api/swagger-ui/issues/8046
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Get action processor schema", description = "Get the action processor JSON schema.")
    @GET
    @Path("/actions/{name}")
    public Response getActionProcessorSchema(@PathParam("name") String name) {
        if (!actions.contains(name)) {
            throw new ItemNotFoundException(String.format("The processor json schema '%s' is not in the catalog.", name));
        }

        return Response.ok(readFile(ACTIONS_DIR_PATH, name)).build();
    }

    protected String readFile(String resourceDirectory, String name) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceDirectory + name + JSON_FILE_EXTENSION);

        if (is == null) {
            throw new ItemNotFoundException(String.format("Could not find '%s'.", name));
        }

        return new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
    }
}
