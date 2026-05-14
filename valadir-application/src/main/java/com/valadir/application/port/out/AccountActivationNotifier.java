package com.valadir.application.port.out;

import com.valadir.domain.model.Email;

public interface AccountActivationNotifier {

    void sendActivationCode(Email email, String code);
}
