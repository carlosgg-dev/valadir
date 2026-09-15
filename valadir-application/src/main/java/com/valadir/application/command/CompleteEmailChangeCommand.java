package com.valadir.application.command;

public record CompleteEmailChangeCommand(
    String accountId,
    String code) {

}
