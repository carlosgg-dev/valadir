package com.valadir.application.command;

public record LogoutCommand(
    String accessTokenJti,
    long accessTokenRemainingTtlSeconds,
    String refreshToken) {

}
