package com.valadir.web.dto.response;

import java.util.List;

public record ErrorResponse(
    String code,
    List<FieldError> errors) {

    public ErrorResponse(String code) {

        this(code, null);
    }

    public record FieldError(String field, String message) {

    }
}
