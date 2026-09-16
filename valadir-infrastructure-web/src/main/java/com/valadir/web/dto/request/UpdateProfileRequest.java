package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank String fullName,
    String givenName,
    @NotBlank String language) {

}
