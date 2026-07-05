package com.valadir.application.port.out;

public interface CaptchaVerifier {

    boolean isValid(String token);
}
