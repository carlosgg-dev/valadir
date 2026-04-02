package com.valadir.application.command;

public record RegisterCommand(
    String email,
    String password,
    String fullName,
    String givenName) {

}
