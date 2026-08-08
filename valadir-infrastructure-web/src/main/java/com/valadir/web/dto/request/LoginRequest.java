package com.valadir.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank String password,
    // Optional: only supplied on the retry once a CAPTCHA step-up (SEC-007) is in effect
    String captchaToken) {

}
