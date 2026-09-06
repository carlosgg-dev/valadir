package com.valadir.application.port.out;

public interface AccountActivationNotifier {

    void sendActivationCode(OtpNotification notification);
}
