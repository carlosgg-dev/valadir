package com.valadir.application.config;

import java.time.Duration;

public record AccountActivationConfig(
    Duration otpTtl) {

}
