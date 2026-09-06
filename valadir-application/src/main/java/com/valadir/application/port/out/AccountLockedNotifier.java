package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;

import java.time.Duration;

public interface AccountLockedNotifier {

    void notifyAccountLocked(Email email, Duration lockoutDuration, Language language);
}
