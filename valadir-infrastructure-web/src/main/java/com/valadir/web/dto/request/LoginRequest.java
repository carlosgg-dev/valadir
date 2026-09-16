package com.valadir.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password,
    // Optional: only supplied on the retry once a CAPTCHA step-up (CAPTCHA_REQUIRED) is in effect
    String captchaToken) {

}
