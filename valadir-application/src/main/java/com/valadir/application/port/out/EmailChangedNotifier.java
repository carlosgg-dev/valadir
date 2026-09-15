package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;

public interface EmailChangedNotifier {

    void notifyEmailChanged(Email email, Language language);
}
