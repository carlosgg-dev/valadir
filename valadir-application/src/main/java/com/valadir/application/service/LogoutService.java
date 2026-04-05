package com.valadir.application.service;

import com.valadir.application.command.LogoutCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutService implements LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutService.class);

    private final RefreshTokenStore refreshTokenStore;
    private final AccessTokenBlacklist accessTokenBlacklist;

    public LogoutService(RefreshTokenStore refreshTokenStore, AccessTokenBlacklist accessTokenBlacklist) {

        this.refreshTokenStore = refreshTokenStore;
        this.accessTokenBlacklist = accessTokenBlacklist;
    }

    @Override
    public void logout(LogoutCommand command) {

        try {
            accessTokenBlacklist.revoke(command.accessTokenJti(), command.accessTokenRemainingTtlSeconds());
        } catch (Exception e) {
            throw new ApplicationException("Failed to revoke access token during logout", ErrorCode.TOKEN_REVOCATION_FAILED);
        }

        try {
            refreshTokenStore.delete(command.refreshToken());
        } catch (Exception e) {
            log.error("Failed to delete refresh token during logout for jti={}", command.accessTokenJti(), e);
        }
    }
}
