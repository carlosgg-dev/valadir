package com.valadir.application.config;

import java.time.Duration;

public record PasswordResetConfig(
    Duration otpTtl,
    Duration verificationTokenTtl) {

}
