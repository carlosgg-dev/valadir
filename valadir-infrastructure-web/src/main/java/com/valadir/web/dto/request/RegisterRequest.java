package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String email,
    @NotBlank String password,
    @NotBlank String fullName,
    String givenName) {

}
