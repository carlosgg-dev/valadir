package com.valadir.application.result;

public record AuthTokenResult(
    String accessToken,
    String refreshToken) {

}
