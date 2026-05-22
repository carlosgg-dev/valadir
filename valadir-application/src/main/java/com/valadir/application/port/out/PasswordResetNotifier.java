package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;

public interface PasswordResetNotifier {

    void sendResetCode(Email email, PlainOtp plainOtp);
}
