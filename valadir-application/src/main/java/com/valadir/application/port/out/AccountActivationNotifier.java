package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;

public interface AccountActivationNotifier {

    void sendActivationCode(Email email, PlainOtp plainOtp);
}
