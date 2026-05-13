package com.valadir.application.port.out;

import com.valadir.domain.model.Email;

public interface AccountActivationPort {

    void sendActivationCode(Email email, String code);
}
