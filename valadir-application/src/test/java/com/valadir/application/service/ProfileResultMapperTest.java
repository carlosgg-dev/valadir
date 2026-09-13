package com.valadir.application.service;

import com.valadir.application.result.ProfileResult;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.Language;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.UserMother;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileResultMapperTest {

    // Every field is a String: a swap between two of them compiles, so each one carries a value of its own.
    @Test
    void toResult_accountAndUser_mapsEachFieldToItsOwnSlot() {

        var email = "bruce.wayne@email.com";
        var fullName = "Bruce Wayne";
        var givenName = "Batman";

        // Deliberately not the fallback language: a result answering EN regardless would still pass.
        var account = AccountMother.active()
            .withEmail(Email.from(email))
            .withLanguage(Language.ES)
            .build();

        var user = UserMother.builder()
            .withAccountId(account.getId())
            .withFullName(FullName.from(fullName))
            .withGivenName(GivenName.from(givenName))
            .build();

        var result = ProfileResultMapper.toResult(account, user);

        assertThat(result).isEqualTo(new ProfileResult(email, fullName, givenName, Language.ES.tag()));
    }

    @Test
    void toResult_userWithoutGivenName_leavesItNull() {

        var account = AccountMother.active().build();

        var user = UserMother.builder()
            .withAccountId(account.getId())
            .withGivenName(GivenName.from(null))
            .build();

        var result = ProfileResultMapper.toResult(account, user);

        assertThat(result.givenName()).isNull();
    }
}
