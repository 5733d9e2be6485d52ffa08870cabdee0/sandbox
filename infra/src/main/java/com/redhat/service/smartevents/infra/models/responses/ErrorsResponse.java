package com.redhat.service.smartevents.infra.models.responses;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ErrorsList")
public class ErrorsResponse extends ListResponse<ErrorResponse> {

    public static ErrorsResponse toErrors(ErrorResponse error) {
        ErrorsResponse errors = new ErrorsResponse();
        errors.setItems(List.of(error));
        return errors;
    }

    public ErrorsResponse() {
        super("Errors");
    }

}
