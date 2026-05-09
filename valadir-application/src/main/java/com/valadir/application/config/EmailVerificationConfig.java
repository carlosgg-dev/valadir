package com.valadir.application.config;

import java.time.Duration;

public record EmailVerificationConfig(
    Duration otpTtl) {

}
