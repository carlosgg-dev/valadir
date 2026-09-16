package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InitiatePasswordResetRequest(
    @NotBlank String email) {

}
