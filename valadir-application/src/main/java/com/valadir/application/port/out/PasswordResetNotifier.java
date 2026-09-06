package com.valadir.application.port.out;

public interface PasswordResetNotifier {

    void sendResetCode(OtpNotification notification);
}
