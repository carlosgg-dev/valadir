package com.valadir.application.config;

import java.time.Duration;

public record EmailChangeConfig(
    Duration otpTtl) {

}
