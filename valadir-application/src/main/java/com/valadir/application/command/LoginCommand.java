package com.valadir.application.command;

public record LoginCommand(
    String email,
    String password) {

}
