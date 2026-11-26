package com.ecommerce.ext.web.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.LongConsumer;

/**
 * SePay. The webhook is signed ({@code X-SePay-Signature: sha256=<hex>} over
 * {@code <X-SePay-Timestamp>.<raw body>}) or, as a fallback, carries
 * {@code Authorization: Apikey <key>}; the poll job reads incoming
 * transactions from the v2 list API with a bearer token as a safety net when a webhook is lost.
 * The base URL is a setting so the sandbox ({@code https://userapi-sandbox.sepay.vn/v2}) works.
 */
@EzySingleton
public class KnSepayProvider implements KnBankTxnProvider {

    public static final String NAME = "sepay";
    static final String DEFAULT_API_BASE_URL = "https://userapi.sepay.vn/v2";
    static final int PAGE_SIZE = 100;
    static final int MAX_PAGES = 20;
    /** The API allows 3 calls per second. */
    static final long MIN_CALL_GAP_MS = 350;
    static final long MAX_CLOCK_SKEW_SECONDS = 300;
    private static final long FIRST_POLL_LOOKBACK_DAYS = 2;
    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper JSON = new ObjectMapper();

    @EzyAutoBind
    private KnPaymentSettings settings;

    private final Function<String, String> keyOverride;
    private final BiFunction<String, String, String> fetcher;
    private final LongConsumer pause;
    private final LongSupplier clockSeconds;

    /** Id of the newest transaction read so far; only the polling thread touches it. */
    private volatile String lastSeenId = "";

    public KnSepayProvider() {
        this.keyOverride = null;
        this.fetcher = KnHttp::get;
        this.pause = KnSepayProvider::sleep;
        this.clockSeconds = () -> System.currentTimeMillis() / 1000;
    }

    KnSepayProvider(Function<String, String> keys) {
        this(keys, KnHttp::get, millis -> { });
    }

    KnSepayProvider(
        Function<String, String> keys,
        BiFunction<String, String, String> fetcher,
        LongConsumer pause
    ) {
        this(keys, fetcher, pause, () -> System.currentTimeMillis() / 1000);
    }

    KnSepayProvider(Function<String, String> keys, LongSupplier clockSeconds) {
        this(keys, KnHttp::get, millis -> { }, clockSeconds);
    }

    private KnSepayProvider(
        Function<String, String> keys,
        BiFunction<String, String, String> fetcher,
        LongConsumer pause,
        LongSupplier clockSeconds
    ) {
        this.clockSeconds = clockSeconds;
        this.keyOverride = keys;
        this.fetcher = fetcher;
        this.pause = pause;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String key(String name) {
        return keyOverride != null ? keyOverride.apply(name) : settings.get(name);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabled() {
        return !key(KnPaymentSettings.SEPAY_WEBHOOK_KEY).isEmpty()
            || !key(KnPaymentSettings.SEPAY_HMAC_SECRET).isEmpty();
    }

    @Override
    public boolean isPollable() {
        return !key(KnPaymentSettings.SEPAY_API_TOKEN).isEmpty();
    }

    @Override
    public List<KnBankTxn> parseWebhook(Map<String, String> headers, String body) {
        authenticate(headers, body);
        JsonNode node = readJson(body);
        List<KnBankTxn> result = new ArrayList<>();
        if (!"in".equalsIgnoreCase(node.path("transferType").asText())) {
            return result;
        }
        String id = node.path("id").asText("");
        BigDecimal amount = amountOf(node.path("transferAmount"));
        if (id.isEmpty() || amount == null) {
            throw new KnWebhookRejectedException("missing id or amount", false);
        }
        result.add(new KnBankTxn(
            NAME,
            id,
            amount,
            contentOf(node.path("content"), node.path("code")),
            node.path("accountNumber").asText(""),
            node.path("subAccount").asText("")
        ));
        return result;
    }

    /**
     * A signed request must carry a valid, fresh signature; there is no fallback once the
     * signature header is present. Only unsigned requests fall back to the API key.
     */
    private void authenticate(Map<String, String> headers, String body) {
        String hmacSecret = key(KnPaymentSettings.SEPAY_HMAC_SECRET);
        String signature = headers.getOrDefault("x-sepay-signature", "");
        if (!hmacSecret.isEmpty() && !signature.isEmpty()) {
            verifySignature(hmacSecret, signature, headers.getOrDefault("x-sepay-timestamp", ""),
                body == null ? "" : body);
            return;
        }
        String expected = key(KnPaymentSettings.SEPAY_WEBHOOK_KEY);
        String authorization = headers.getOrDefault("authorization", "");
        if (!KnSecrets.sameSecret(expected, authorization.replaceFirst("(?i)^apikey\\s+", ""))) {
            throw new KnWebhookRejectedException("invalid api key", true);
        }
    }

    private void verifySignature(String secret, String signature, String timestamp, String body) {
        long sentAt;
        try {
            sentAt = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            throw new KnWebhookRejectedException("invalid timestamp", true);
        }
        if (Math.abs(clockSeconds.getAsLong() - sentAt) > MAX_CLOCK_SKEW_SECONDS) {
            throw new KnWebhookRejectedException("stale timestamp", true);
        }
        String signed = timestamp.trim() + "." + body;
        String expected = "sha256=" + KnSecrets.hmacSha256Hex(secret, signed);
        if (!KnSecrets.sameSecret(expected, signature.trim().toLowerCase())) {
            throw new KnWebhookRejectedException("invalid signature", true);
        }
    }

    @Override
    public List<KnBankTxn> poll() {
        String token = key(KnPaymentSettings.SEPAY_API_TOKEN);
        List<KnBankTxn> result = new ArrayList<>();
        if (token.isEmpty()) {
            return result;
        }
        String newest = lastSeenId;
        for (int page = 1; page <= MAX_PAGES; ++page) {
            if (page > 1) {
                pause.accept(MIN_CALL_GAP_MS);
            }
            JsonNode body = readJson(fetcher.apply(pageUrl(page), "Bearer " + token));
            for (JsonNode item : body.path("data")) {
                String id = item.path("id").asText("");
                if (id.isEmpty()) {
                    continue;
                }
                newest = id;
                BigDecimal amount = amountOf(item.path("amount_in"));
                if (amount == null || amount.signum() <= 0) {
                    continue;
                }
                result.add(new KnBankTxn(
                    NAME,
                    id,
                    amount,
                    contentOf(item.path("transaction_content"), item.path("code")),
                    item.path("account_number").asText(""),
                    item.path("va").asText("")
                ));
            }
            if (!body.path("meta").path("pagination").path("has_more").asBoolean(false)) {
                break;
            }
        }
        // Saved only after every page was read: since_id filters but does not reorder, so a
        // cursor taken mid-way would skip transactions on the pages not yet read.
        lastSeenId = newest;
        return result;
    }

    private String pageUrl(int page) {
        String base = key(KnPaymentSettings.SEPAY_API_BASE_URL);
        base = base.isEmpty() ? DEFAULT_API_BASE_URL : base.replaceAll("/+$", "");
        StringBuilder url = new StringBuilder(base)
            .append("/transactions?transfer_type=in&transaction_date_sort=asc")
            .append("&per_page=").append(PAGE_SIZE)
            .append("&page=").append(page);
        if (lastSeenId.isEmpty()) {
            String from = LocalDateTime.now()
                .minusDays(FIRST_POLL_LOOKBACK_DAYS).format(DATE_TIME);
            url.append("&transaction_date_from=").append(from.replace(" ", "%20"));
        } else {
            url.append("&since_id=").append(lastSeenId);
        }
        return url.toString();
    }

    private static String contentOf(JsonNode content, JsonNode code) {
        // SePay may split the payment code out of the content; keep both so either matches.
        return (content.asText("") + " " + code.asText("")).trim();
    }

    private static BigDecimal amountOf(JsonNode node) {
        try {
            return node.isMissingNode() || node.isNull() ? null : new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static JsonNode readJson(String body) {
        try {
            return JSON.readTree(body == null ? "" : body);
        } catch (IOException e) {
            throw new KnWebhookRejectedException("malformed body", false);
        }
    }
}
