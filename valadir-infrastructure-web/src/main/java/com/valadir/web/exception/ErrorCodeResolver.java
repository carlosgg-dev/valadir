package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.springframework.http.HttpStatusCode;

import java.util.Set;

/**
 * The inverse of {@link HttpStatusResolver}: where the application throws, the code decides the
 * status; where the framework rejects, the status is given and the code is what must be derived.
 */
public class ErrorCodeResolver {

    private static final Set<String> PRESENCE_CONSTRAINTS = Set.of("NotBlank", "NotNull", "NotEmpty");

    public ErrorCode resolve(HttpStatusCode status) {

        return status.is4xxClientError()
            ? ErrorCode.MALFORMED_REQUEST
            : ErrorCode.INTERNAL_SERVER_ERROR;
    }

    // Bean Validation names the constraint it rejected; an unmapped name, or none at all, answers
    // invalid_field rather than leaking the constraint or its message, which Hibernate localizes
    public ErrorCode resolveConstraint(String constraintName) {

        return constraintName != null && PRESENCE_CONSTRAINTS.contains(constraintName)
            ? ErrorCode.REQUIRED_FIELD_MISSING
            : ErrorCode.INVALID_FIELD;
    }
}
