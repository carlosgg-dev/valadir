package com.valadir.application.config;

import java.time.Duration;

public record VerificationConfig(
    Duration tokenTtl) {

}
