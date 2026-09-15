package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;

public record EmailChangeRequest(
    Email newEmail,
    HashedOtp hashedOtp) {

}
