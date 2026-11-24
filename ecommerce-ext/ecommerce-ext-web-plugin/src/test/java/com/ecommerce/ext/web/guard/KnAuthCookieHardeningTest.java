package com.ecommerce.ext.web.guard;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class KnAuthCookieHardeningTest {

    @Test
    public void accessTokenCookieGetsHttpOnlyAndSameSite() {
        assertEquals(
            KnAuthGuardInterceptor.hardenCookie("accessToken=abc; Path=/; Max-Age=604799"),
            "accessToken=abc; Path=/; Max-Age=604799; HttpOnly; SameSite=Lax"
        );
    }

    @Test
    public void existingFlagsAreNotDuplicated() {
        String c = "accessToken=abc; Path=/; HttpOnly; SameSite=Strict";
        assertEquals(KnAuthGuardInterceptor.hardenCookie(c), c);
    }

    @Test
    public void otherCookiesAreUntouched() {
        String c = "accessTokenExpiredAt=1; Path=/";
        assertEquals(KnAuthGuardInterceptor.hardenCookie(c), c);
    }
}
