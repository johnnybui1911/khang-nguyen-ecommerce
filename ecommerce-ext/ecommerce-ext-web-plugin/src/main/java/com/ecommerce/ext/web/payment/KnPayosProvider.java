package com.ecommerce.ext.web.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * payOS. The webhook body is signed: {@code signature} is the HMAC-SHA256 of the
 * {@code data} fields sorted by name and joined as {@code name=value&...}, keyed with the
 * checksum key.
 */
@EzySingleton
public class KnPayosProvider implements KnBankTxnProvider {

    public static final String NAME = "payos";

    @EzyAutoBind
    private KnPaymentSettings settings;

    private final Function<String, String> keyOverride;

    public KnPayosProvider() {
        this.keyOverride = null;
    }

    KnPayosProvider(Function<String, String> keys) {
        this.keyOverride = keys;
    }

    private String checksumKey() {
        return keyOverride != null
            ? keyOverride.apply(KnPaymentSettings.PAYOS_CHECKSUM_KEY)
            : settings.get(KnPaymentSettings.PAYOS_CHECKSUM_KEY);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabled() {
        return !checksumKey().isEmpty();
    }

    @Override
    public List<KnBankTxn> parseWebhook(Map<String, String> headers, String body) {
        String key = checksumKey();
        if (key.isEmpty()) {
            throw new KnWebhookRejectedException("provider disabled", true);
        }
        JsonNode root = KnSepayProvider.readJson(body);
        JsonNode data = root.path("data");
        if (!data.isObject()) {
            throw new KnWebhookRejectedException("missing data", false);
        }
        String expected = KnSecrets.hmacSha256Hex(key, signedText(data));
        if (!KnSecrets.sameSecret(expected, root.path("signature").asText(""))) {
            throw new KnWebhookRejectedException("invalid signature", true);
        }
        List<KnBankTxn> result = new ArrayList<>();
        boolean success = "00".equals(root.path("code").asText())
            && "00".equals(data.path("code").asText());
        if (!success) {
            return result;
        }
        String id = data.path("reference").asText("");
        if (id.isEmpty()) {
            id = data.path("paymentLinkId").asText("");
        }
        if (id.isEmpty() || !data.path("amount").canConvertToLong()) {
            throw new KnWebhookRejectedException("missing reference or amount", false);
        }
        result.add(new KnBankTxn(
            NAME,
            id,
            BigDecimal.valueOf(data.path("amount").asLong()),
            data.path("description").asText("")
        ));
        return result;
    }

    static String signedText(JsonNode data) {
        Map<String, String> sorted = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            sorted.put(field.getKey(), value.isNull() ? "" : value.asText());
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (text.length() > 0) {
                text.append('&');
            }
            text.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return text.toString();
    }
}
