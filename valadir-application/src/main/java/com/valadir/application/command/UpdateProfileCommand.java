package com.valadir.application.command;

public record UpdateProfileCommand(
    String accountId,
    String fullName,
    String givenName,
    String language) {

}
