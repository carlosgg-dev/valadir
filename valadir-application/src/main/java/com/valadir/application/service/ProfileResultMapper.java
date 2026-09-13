package com.valadir.application.service;

import com.valadir.application.result.ProfileResult;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.User;

final class ProfileResultMapper {

    private ProfileResultMapper() {

    }

    static ProfileResult toResult(Account account, User user) {

        return new ProfileResult(
            account.getEmail().value(),
            user.getFullName().value(),
            user.getGivenName().value(),
            account.getLanguage().tag()
        );
    }
}
