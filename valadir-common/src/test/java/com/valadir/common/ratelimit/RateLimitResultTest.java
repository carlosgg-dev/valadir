package com.valadir.common.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitResultTest {

    @Test
    void remaining_requestCountBelowLimit_returnsAvailableSlots() {

        var result = new RateLimitResult(true, 3L, 10, 60L);

        assertThat(result.remaining()).isEqualTo(7L);
    }

    @Test
    void remaining_requestCountEqualsLimit_returnsZero() {

        var result = new RateLimitResult(false, 10L, 10, 60L);

        assertThat(result.remaining()).isZero();
    }

    @Test
    void remaining_requestCountExceedsLimit_returnsZero() {

        var result = new RateLimitResult(false, 15L, 10, 60L);

        assertThat(result.remaining()).isZero();
    }

    @Test
    void isMoreRestrictiveThan_fewerRemainingRequests_returnsTrue() {

        var restrictive = new RateLimitResult(true, 8L, 10, 60L);  // 2 remaining
        var lenient = new RateLimitResult(true, 5L, 10, 60L);       // 5 remaining

        assertThat(restrictive.isMoreRestrictiveThan(lenient)).isTrue();
    }

    @Test
    void isMoreRestrictiveThan_moreRemainingRequests_returnsFalse() {

        var lenient = new RateLimitResult(true, 5L, 10, 60L);       // 5 remaining
        var restrictive = new RateLimitResult(true, 8L, 10, 60L);   // 2 remaining

        assertThat(lenient.isMoreRestrictiveThan(restrictive)).isFalse();
    }

    @Test
    void isMoreRestrictiveThan_equalRemainingRequests_returnsFalse() {

        var a = new RateLimitResult(true, 7L, 10, 60L);  // 3 remaining
        var b = new RateLimitResult(true, 7L, 10, 60L);  // 3 remaining

        assertThat(a.isMoreRestrictiveThan(b)).isFalse();
    }
}
