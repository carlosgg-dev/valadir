package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(min = 2, max = 255) String fullName,
    @Size(max = 100) String givenName,
    @NotBlank String language) {

}
