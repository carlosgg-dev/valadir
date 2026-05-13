package com.valadir.application.port.out;

import java.time.Instant;

public interface ExpiredPendingActivationAccountCleaner {

    int delete(Instant cutoff);
}
