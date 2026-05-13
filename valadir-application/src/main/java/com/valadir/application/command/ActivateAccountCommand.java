package com.valadir.application.command;

public record ActivateAccountCommand(
    String email,
    String code) {

}
