package com.valadir.application.command;

public record LogoutCommand(
    String accessTokenJti,
    String refreshToken) {

}
