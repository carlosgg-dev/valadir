package com.valadir.application.port.out;

import com.valadir.application.otp.PlainOtp;
import com.valadir.domain.model.Email;

public interface PasswordResetNotifier {

    void sendResetCode(Email email, PlainOtp plainOtp);
}
