package com.valadir.application.command;

import java.time.Duration;

public record LogoutCommand(
    String accessTokenJti,
    Duration accessTokenRemainingTtl,
    String refreshToken,
    String accountId) {

}
