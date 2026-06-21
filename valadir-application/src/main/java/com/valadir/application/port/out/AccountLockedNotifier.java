package com.valadir.application.port.out;

import com.valadir.domain.model.Email;

import java.time.Duration;

public interface AccountLockedNotifier {

    void notifyAccountLocked(Email email, Duration lockoutDuration);
}
