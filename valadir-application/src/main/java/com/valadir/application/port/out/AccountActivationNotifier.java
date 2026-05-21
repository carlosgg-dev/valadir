package com.valadir.application.port.out;

import com.valadir.application.otp.PlainOtp;
import com.valadir.domain.model.Email;

public interface AccountActivationNotifier {

    void sendActivationCode(Email email, PlainOtp plainOtp);
}
