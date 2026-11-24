package com.ecommerce.ext.web.payment;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class KnPaymentMailerTest {

    @Test
    public void mailboxKeyDropsPlusTagAndCase() {
        assertEquals(KnPaymentMailer.mailboxKey(" Shop+Guest7@Gmail.com "), "shop@gmail.com");
        assertEquals(KnPaymentMailer.mailboxKey("shop@gmail.com"), "shop@gmail.com");
    }

    @Test
    public void mailboxKeyFoldsGmailDotsAndGooglemail() {
        assertEquals(KnPaymentMailer.mailboxKey("k.n.shop+guest1@gmail.com"), "knshop@gmail.com");
        assertEquals(KnPaymentMailer.mailboxKey("KN.Shop@GoogleMail.com"), "knshop@gmail.com");
        assertEquals(KnPaymentMailer.mailboxKey("k.n@example.com"), "k.n@example.com");
    }

    @Test
    public void mailboxKeyKeepsOddAddresses() {
        assertEquals(KnPaymentMailer.mailboxKey("+tag@x.vn"), "+tag@x.vn");
        assertEquals(KnPaymentMailer.mailboxKey("nobody"), "nobody");
    }
}
