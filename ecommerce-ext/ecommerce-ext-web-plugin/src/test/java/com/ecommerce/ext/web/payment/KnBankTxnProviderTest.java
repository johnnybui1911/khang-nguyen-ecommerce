package com.ecommerce.ext.web.payment;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

public class KnBankTxnProviderTest {

    private static Map<String, String> headers(String name, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(name, value);
        return map;
    }

    private final KnSepayProvider sepay = new KnSepayProvider(
        name -> KnPaymentSettings.SEPAY_WEBHOOK_KEY.equals(name) ? "sepay-key" : ""
    );

    @Test
    public void sepayAcceptsIncomingTransferWithTheApiKey() {
        List<KnBankTxn> txns = sepay.parseWebhook(
            headers("authorization", "Apikey sepay-key"),
            "{\"id\":92704,\"transferType\":\"in\",\"transferAmount\":250000,"
                + "\"content\":\"thanh toan KN00012\",\"code\":null}"
        );
        assertEquals(txns.size(), 1);
        assertEquals(txns.get(0).getTxnId(), "92704");
        assertEquals(txns.get(0).getAmount().compareTo(new BigDecimal("250000")), 0);
        assertTrue(txns.get(0).getContent().contains("KN00012"));
    }

    @Test
    public void sepayRejectsWrongOrMissingKey() {
        String body = "{\"id\":1,\"transferType\":\"in\",\"transferAmount\":1}";
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(headers("authorization", "Apikey nope"), body)
        ).isUnauthorized());
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(Collections.emptyMap(), body)
        ).isUnauthorized());
    }

    @Test
    public void sepayIgnoresOutgoingTransfers() {
        assertTrue(sepay.parseWebhook(
            headers("authorization", "Apikey sepay-key"),
            "{\"id\":2,\"transferType\":\"out\",\"transferAmount\":5}"
        ).isEmpty());
    }

    @Test
    public void sepayRejectsMalformedBodyAsBadRequest() {
        assertFalse(expectThrows(
            KnWebhookRejectedException.class,
            () -> sepay.parseWebhook(headers("authorization", "Apikey sepay-key"), "{oops")
        ).isUnauthorized());
    }

    @Test
    public void emptyKeyDisablesTheProvider() {
        KnSepayProvider disabled = new KnSepayProvider(name -> "");
        assertFalse(disabled.isEnabled());
        assertFalse(disabled.isPollable());
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> disabled.parseWebhook(headers("authorization", "Apikey "), "{}")
        ).isUnauthorized());
    }

    private static String payosBody(String key, String amount, String signatureOverride) {
        String data = "{\"orderCode\":123,\"amount\":" + amount
            + ",\"description\":\"KN00012\",\"reference\":\"TF1\",\"code\":\"00\","
            + "\"paymentLinkId\":\"abc\",\"counterAccountName\":null}";
        String signed = "amount=" + amount + "&code=00&counterAccountName="
            + "&description=KN00012&orderCode=123&paymentLinkId=abc&reference=TF1";
        String signature = signatureOverride != null
            ? signatureOverride : KnSecrets.hmacSha256Hex(key, signed);
        return "{\"code\":\"00\",\"success\":true,\"data\":" + data
            + ",\"signature\":\"" + signature + "\"}";
    }

    private final KnPayosProvider payos = new KnPayosProvider(name -> "checksum");

    @Test
    public void payosAcceptsAValidSignature() {
        List<KnBankTxn> txns = payos.parseWebhook(
            Collections.emptyMap(),
            payosBody("checksum", "300000", null)
        );
        assertEquals(txns.size(), 1);
        assertEquals(txns.get(0).getTxnId(), "TF1");
        assertEquals(txns.get(0).getAmount().compareTo(new BigDecimal("300000")), 0);
    }

    @Test
    public void payosRejectsATamperedAmount() {
        String body = payosBody("checksum", "300000", null).replace("300000", "1");
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> payos.parseWebhook(Collections.emptyMap(), body)
        ).isUnauthorized());
    }

    @Test
    public void payosRejectsAWrongSignature() {
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> payos.parseWebhook(
                Collections.emptyMap(),
                payosBody("checksum", "300000", "deadbeef")
            )
        ).isUnauthorized());
    }

    private final KnCassoProvider casso = new KnCassoProvider(name -> "casso-token");

    @Test
    public void cassoReadsIncomingTransactionsAndSkipsOutgoing() {
        List<KnBankTxn> txns = casso.parseWebhook(
            headers("secure-token", "casso-token"),
            "{\"error\":0,\"data\":[{\"tid\":\"A1\",\"amount\":100000,"
                + "\"description\":\"KN00007\"},{\"tid\":\"A2\",\"amount\":-5}]}"
        );
        assertEquals(txns.size(), 1);
        assertEquals(txns.get(0).getTxnId(), "A1");
    }

    @Test
    public void cassoRejectsAWrongToken() {
        assertTrue(expectThrows(
            KnWebhookRejectedException.class,
            () -> casso.parseWebhook(headers("secure-token", "x"), "{\"data\":[]}")
        ).isUnauthorized());
    }
}
