package com.ecommerce.ext.web.payment;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

public class KnSepayWebhookSignatureTest {

    private static final long NOW = 1_800_000_000L;
    private static final String BODY =
        "{\"id\":7,\"transferType\":\"in\",\"transferAmount\":250000,"
            + "\"content\":\"KN00012\"}";

    private final KnSepayProvider sepay = new KnSepayProvider(
        name -> {
            if (KnPaymentSettings.SEPAY_HMAC_SECRET.equals(name)) {
                return "hmac-secret";
            }
            return KnPaymentSettings.SEPAY_WEBHOOK_KEY.equals(name) ? "api-key" : "";
        },
        () -> NOW
    );

    private static Map<String, String> signed(String secret, long timestamp, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-sepay-timestamp", String.valueOf(timestamp));
        headers.put(
            "x-sepay-signature",
            "sha256=" + KnSecrets.hmacSha256Hex(secret, timestamp + "." + body)
        );
        return headers;
    }

    @Test
    public void validSignatureIsAccepted() {
        assertEquals(sepay.parseWebhook(signed("hmac-secret", NOW, BODY), BODY).size(), 1);
        assertEquals(
            sepay.parseWebhook(signed("hmac-secret", NOW - 300, BODY), BODY).size(), 1
        );
    }

    @Test
    public void wrongSignatureIsRejected() {
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(signed("other-secret", NOW, BODY), BODY)
        ).isUnauthorized());
    }

    @Test
    public void aTamperedBodyIsRejected() {
        Map<String, String> headers = signed("hmac-secret", NOW, BODY);
        String tampered = BODY.replace("250000", "999999999");
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(headers, tampered)
        ).isUnauthorized());
    }

    @Test
    public void staleOrFutureTimestampIsRejected() {
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(signed("hmac-secret", NOW - 301, BODY), BODY)
        ).isUnauthorized());
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(signed("hmac-secret", NOW + 301, BODY), BODY)
        ).isUnauthorized());
    }

    @Test
    public void missingOrMalformedTimestampIsRejected() {
        Map<String, String> headers = signed("hmac-secret", NOW, BODY);
        headers.remove("x-sepay-timestamp");
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(headers, BODY)
        ).isUnauthorized());
    }

    @Test
    public void aSignedRequestNeverFallsBackToTheApiKey() {
        Map<String, String> headers = signed("other-secret", NOW, BODY);
        headers.put("authorization", "Apikey api-key");
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(headers, BODY)
        ).isUnauthorized());
    }

    @Test
    public void unsignedRequestsStillWorkWithTheApiKey() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Apikey api-key");
        assertEquals(sepay.parseWebhook(headers, BODY).size(), 1);
    }

    @Test
    public void hmacSecretAloneEnablesTheProvider() {
        KnSepayProvider only = new KnSepayProvider(
            name -> KnPaymentSettings.SEPAY_HMAC_SECRET.equals(name) ? "s" : "",
            () -> NOW
        );
        assertTrue(only.isEnabled());
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> only.parseWebhook(new HashMap<>(), BODY)
        ).isUnauthorized());
    }
}
