package com.ecommerce.ext.web.payment;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class KnPaymentMailerRedirectTest {

    @Test
    public void redirectIsOffByDefault() {
        assertNull(KnPaymentMailer.redirectTarget(null));
        assertNull(KnPaymentMailer.redirectTarget(""));
        assertNull(KnPaymentMailer.redirectTarget("   "));
        assertNull(KnPaymentMailer.redirectTarget("off"));
        assertNull(KnPaymentMailer.redirectTarget("OFF"));
        assertNull(KnPaymentMailer.redirectTarget("not-an-email"));
    }

    @Test
    public void redirectOnlyToAValidEmail() {
        assertEquals(
            KnPaymentMailer.redirectTarget(" shop+qa@gmail.com "),
            "shop+qa@gmail.com"
        );
    }
}
