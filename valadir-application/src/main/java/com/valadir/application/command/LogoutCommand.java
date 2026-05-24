package com.valadir.application.command;

import com.valadir.domain.model.AccountId;

import java.time.Duration;

public record LogoutCommand(
    String accessTokenJti,
    Duration accessTokenRemainingTtl,
    String refreshToken,
    AccountId accountId) {

}
