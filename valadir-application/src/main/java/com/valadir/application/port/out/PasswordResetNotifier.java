package com.valadir.application.port.out;

import com.valadir.domain.model.Email;

public interface PasswordResetNotifier {

    void sendResetCode(Email email, String code);
}
