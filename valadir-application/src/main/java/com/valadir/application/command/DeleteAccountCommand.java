package com.valadir.application.command;

public record DeleteAccountCommand(
    String accountId,
    String password) {

}
