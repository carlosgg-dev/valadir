package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.PlainOtp;

import java.time.Duration;

public record OtpNotification(
    Email email,
    PlainOtp otp,
    Duration ttl,
    Language language) {

}
