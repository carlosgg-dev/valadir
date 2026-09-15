package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompleteEmailChangeRequest(
    @NotBlank @Pattern(regexp = "\\d{6}") String code) {

}
