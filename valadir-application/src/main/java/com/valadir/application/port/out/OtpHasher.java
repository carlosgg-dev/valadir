package com.valadir.application.port.out;

import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.PlainOtp;

public interface OtpHasher {

    HashedOtp hash(PlainOtp plainOtp);

    boolean matches(PlainOtp plainOtp, HashedOtp hashedOtp);
}
