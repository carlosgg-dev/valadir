package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;

public interface PasswordChangedNotifier {

    void notifyPasswordChanged(Email email, Language language);
}
