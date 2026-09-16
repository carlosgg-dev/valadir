package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActivateAccountRequest(
    @NotBlank String email,
    @NotBlank @Pattern(regexp = "\\d{6}") String code) {

}
