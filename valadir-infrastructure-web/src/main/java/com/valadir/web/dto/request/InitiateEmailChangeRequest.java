package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InitiateEmailChangeRequest(
    @NotBlank String newEmail,
    @NotBlank String password) {

}
