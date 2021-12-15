package com.redhat.service.bridge.rhoas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Topics extends PaginatedList<Topic> {

}
