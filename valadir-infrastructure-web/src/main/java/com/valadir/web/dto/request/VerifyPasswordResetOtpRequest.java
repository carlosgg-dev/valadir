package com.valadir.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordResetOtpRequest(
    @NotBlank @Email String email,
    @NotBlank String code) {

}
