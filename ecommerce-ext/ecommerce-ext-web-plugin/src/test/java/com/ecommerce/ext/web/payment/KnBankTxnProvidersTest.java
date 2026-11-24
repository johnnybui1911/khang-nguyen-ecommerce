package com.ecommerce.ext.web.payment;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Field;
import org.testng.annotations.Test;

public class KnBankTxnProvidersTest {

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    public void everyProviderIsFoundByItsWebhookName() throws Exception {
        KnBankTxnProviders providers = new KnBankTxnProviders();
        inject(providers, "sepay", new KnSepayProvider(name -> ""));
        inject(providers, "payos", new KnPayosProvider());
        inject(providers, "casso", new KnCassoProvider());
        assertEquals(providers.all().size(), 3);
        assertTrue(providers.find("sepay").isPresent());
        assertTrue(providers.find("payos").isPresent());
        assertTrue(providers.find("casso").isPresent());
        assertFalse(providers.find("nope").isPresent());
    }
}
