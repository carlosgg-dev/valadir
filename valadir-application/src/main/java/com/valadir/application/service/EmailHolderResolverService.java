package com.valadir.application.service;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class EmailHolderResolverService implements EmailHolderResolver {

    private static final Logger log = LoggerFactory.getLogger(EmailHolderResolverService.class);

    private final AccountRepository accountRepository;

    public EmailHolderResolverService(AccountRepository accountRepository) {

        this.accountRepository = accountRepository;
    }

    // An address belongs to the account that proved it. One still pending activation only claimed it,
    // so whoever proves it next takes it over; an active one keeps it against every other flow.
    @Override
    public Optional<AccountId> replaceableHolderFor(Email email) {

        return accountRepository.findByEmail(email)
            .map(this::replaceableIdOf);
    }

    private AccountId replaceableIdOf(Account holder) {

        return switch (holder.getStatus()) {
            case PENDING_ACTIVATION -> holder.getId();
            case ACTIVE -> {
                log.warn("Email already held by an active account, existingAccountId={}", holder.getId().value());
                throw new ApplicationException("Email already registered", ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        };
    }
}
