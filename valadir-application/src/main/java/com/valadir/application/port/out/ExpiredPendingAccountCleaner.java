package com.valadir.application.port.out;

import java.time.Instant;

public interface ExpiredPendingAccountCleaner {

    int delete(Instant cutoff);
}
