package com.valadir.application.command;

public record VerifyEmailCommand(
    String email,
    String code) {

}
