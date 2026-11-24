package com.ecommerce.ext.web.payment;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KnSepayPollTest {

    private final List<String> urls = new ArrayList<>();
    private final List<String> auths = new ArrayList<>();
    private final List<Long> pauses = new ArrayList<>();

    @BeforeMethod
    public void reset() {
        urls.clear();
        auths.clear();
        pauses.clear();
    }

    private static String page(boolean hasMore, String... ids) {
        StringBuilder data = new StringBuilder();
        for (String id : ids) {
            if (data.length() > 0) {
                data.append(',');
            }
            data.append("{\"id\":\"").append(id).append("\",\"transfer_type\":\"in\",")
                .append("\"amount_in\":250000,\"transaction_content\":\"KN0001")
                .append(id.substring(id.length() - 1)).append("\",\"code\":null}");
        }
        return "{\"status\":\"success\",\"data\":[" + data + "],\"meta\":{\"pagination\":"
            + "{\"has_more\":" + hasMore + "}}}";
    }

    private KnSepayProvider provider(Function<String, String> keys, String... pages) {
        int[] next = {0};
        return new KnSepayProvider(keys, (url, auth) -> {
            urls.add(url);
            auths.add(auth);
            return pages[Math.min(next[0]++, pages.length - 1)];
        }, pauses::add);
    }

    private static Function<String, String> keys(String baseUrl) {
        return name -> {
            if (KnPaymentSettings.SEPAY_API_TOKEN.equals(name)) {
                return "tok";
            }
            return KnPaymentSettings.SEPAY_API_BASE_URL.equals(name) ? baseUrl : "";
        };
    }

    @Test
    public void readsEveryPageWithTheV2QueryAndBearerToken() {
        KnSepayProvider sepay = provider(keys(""), page(true, "a1", "a2"), page(false, "a3"));
        List<KnBankTxn> txns = sepay.poll();
        assertEquals(txns.size(), 3);
        assertEquals(txns.get(2).getTxnId(), "a3");
        assertEquals(urls.size(), 2);
        assertTrue(urls.get(0).startsWith("https://userapi.sepay.vn/v2/transactions?"));
        assertTrue(urls.get(0).contains("transfer_type=in"));
        assertTrue(urls.get(0).contains("transaction_date_sort=asc"));
        assertTrue(urls.get(0).contains("page=1"));
        assertTrue(urls.get(1).contains("page=2"));
        assertEquals(auths.get(0), "Bearer tok");
        assertEquals(pauses.size(), 1);
        assertTrue(pauses.get(0) >= 334);
    }

    @Test
    public void nextPollResumesFromTheLastIdOnlyAfterAllPagesWereRead() {
        KnSepayProvider sepay = provider(
            keys("https://userapi-sandbox.sepay.vn/v2/"),
            page(true, "a1", "a2"), page(false, "a3"), page(false)
        );
        sepay.poll();
        sepay.poll();
        assertTrue(urls.get(0).startsWith("https://userapi-sandbox.sepay.vn/v2/transactions?"));
        assertTrue(urls.get(0).contains("transaction_date_from="));
        assertTrue(urls.get(1).contains("page=2"));
        assertTrue(!urls.get(1).contains("since_id"));
        assertTrue(urls.get(2).contains("since_id=a3"));
        assertTrue(!urls.get(2).contains("transaction_date_from"));
    }

    @Test
    public void aFailedPollKeepsTheCursor() {
        KnSepayProvider sepay = new KnSepayProvider(keys(""), (url, auth) -> {
            urls.add(url);
            if (urls.size() == 2) {
                throw new IllegalStateException("provider answered 429");
            }
            return urls.size() == 1 ? page(true, "a1") : page(false, "a2");
        }, pauses::add);
        try {
            sepay.poll();
        } catch (IllegalStateException expected) {
            // the cursor must not have moved to a1
        }
        sepay.poll();
        assertTrue(!urls.get(2).contains("since_id"));
    }
}
