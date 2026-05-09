package com.valadir.application.config;

import java.time.Duration;

public record PendingAccountPurgeConfig(
    Duration accountGracePeriod) {

}
