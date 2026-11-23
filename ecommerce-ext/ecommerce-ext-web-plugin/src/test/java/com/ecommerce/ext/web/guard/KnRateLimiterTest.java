package com.ecommerce.ext.web.guard;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class KnRateLimiterTest {

    @Test
    public void sixthAttemptInTheWindowIsRejectedAndTheWindowReopens() {
        KnRateLimiter limiter = new KnRateLimiter(5, 60_000);
        for (int i = 0; i < 5; i++) {
            assertEquals(limiter.tryAcquire("ip", 1_000 + i), 0);
        }
        assertTrue(limiter.tryAcquire("ip", 2_000) > 0);
        assertEquals(limiter.tryAcquire("other", 2_000), 0);
        assertEquals(limiter.tryAcquire("ip", 61_001), 0);
    }
}
