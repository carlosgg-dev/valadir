package com.valadir.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendAccountActivationCodeRequest(
    @NotBlank @Email String email) {

}
