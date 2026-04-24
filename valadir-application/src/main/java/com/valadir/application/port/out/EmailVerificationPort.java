package com.valadir.application.port.out;

import com.valadir.domain.model.Email;

public interface EmailVerificationPort {

    void sendVerificationCode(Email email, String code);
}
