package com.valadir.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InitiatePasswordResetRequest(
    @NotBlank @Email @Size(max = 255) String email) {

}
