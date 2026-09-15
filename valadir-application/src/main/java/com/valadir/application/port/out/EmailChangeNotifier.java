package com.valadir.application.port.out;

public interface EmailChangeNotifier {

    void sendConfirmationCode(OtpNotification notification);
}
