package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompletePasswordResetRequest(
    @NotBlank String verificationToken,
    @NotBlank String newPassword) {

}
