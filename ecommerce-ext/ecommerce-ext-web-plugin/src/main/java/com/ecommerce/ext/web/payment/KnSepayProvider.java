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
import java.util.function.Function;

/**
 * SePay. The webhook carries {@code Authorization: Apikey <key>}; the poll job reads the
 * recent transaction list with a bearer token as a safety net when a webhook is lost.
 */
@EzySingleton
public class KnSepayProvider implements KnBankTxnProvider {

    public static final String NAME = "sepay";
    static final String LIST_URL = "https://my.sepay.vn/userapi/transactions/list?limit=50";

    private static final ObjectMapper JSON = new ObjectMapper();

    @EzyAutoBind
    private KnPaymentSettings settings;

    private final Function<String, String> keyOverride;

    public KnSepayProvider() {
        this.keyOverride = null;
    }

    KnSepayProvider(Function<String, String> keys) {
        this.keyOverride = keys;
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
        return !key(KnPaymentSettings.SEPAY_WEBHOOK_KEY).isEmpty();
    }

    @Override
    public boolean isPollable() {
        return !key(KnPaymentSettings.SEPAY_API_TOKEN).isEmpty();
    }

    @Override
    public List<KnBankTxn> parseWebhook(Map<String, String> headers, String body) {
        String expected = key(KnPaymentSettings.SEPAY_WEBHOOK_KEY);
        String authorization = headers.getOrDefault("authorization", "");
        if (!KnSecrets.sameSecret(expected, authorization.replaceFirst("(?i)^apikey\\s+", ""))) {
            throw new KnWebhookRejectedException("invalid api key", true);
        }
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
            contentOf(node.path("content"), node.path("code"))
        ));
        return result;
    }

    @Override
    public List<KnBankTxn> poll() {
        String token = key(KnPaymentSettings.SEPAY_API_TOKEN);
        List<KnBankTxn> result = new ArrayList<>();
        if (token.isEmpty()) {
            return result;
        }
        String body = KnHttp.get(LIST_URL, "Bearer " + token);
        JsonNode list = readJson(body).path("transactions");
        for (JsonNode item : list) {
            BigDecimal amount = amountOf(item.path("amount_in"));
            String id = item.path("id").asText("");
            if (id.isEmpty() || amount == null || amount.signum() <= 0) {
                continue;
            }
            result.add(new KnBankTxn(
                NAME,
                id,
                amount,
                contentOf(item.path("transaction_content"), item.path("code"))
            ));
        }
        return result;
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
