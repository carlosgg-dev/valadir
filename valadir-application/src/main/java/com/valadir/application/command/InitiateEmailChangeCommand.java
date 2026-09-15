package com.valadir.application.command;

public record InitiateEmailChangeCommand(
    String accountId,
    String newEmail,
    String password) {

}
