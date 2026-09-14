package com.valadir.application.command;

public record ChangePasswordCommand(
    String accountId,
    String currentPassword,
    String newPassword) {

}
