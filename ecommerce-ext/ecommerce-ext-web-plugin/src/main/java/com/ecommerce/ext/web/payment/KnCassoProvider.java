package com.ecommerce.ext.web.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Casso (webhook v1): the request carries the secure token in the {@code secure-token} header. */
@EzySingleton
public class KnCassoProvider implements KnBankTxnProvider {

    public static final String NAME = "casso";

    @EzyAutoBind
    private KnPaymentSettings settings;

    private final Function<String, String> keyOverride;

    public KnCassoProvider() {
        this.keyOverride = null;
    }

    KnCassoProvider(Function<String, String> keys) {
        this.keyOverride = keys;
    }

    private String token() {
        return keyOverride != null
            ? keyOverride.apply(KnPaymentSettings.CASSO_SECURE_TOKEN)
            : settings.get(KnPaymentSettings.CASSO_SECURE_TOKEN);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabled() {
        return !token().isEmpty();
    }

    @Override
    public List<KnBankTxn> parseWebhook(Map<String, String> headers, String body) {
        if (!KnSecrets.sameSecret(token(), headers.getOrDefault("secure-token", ""))) {
            throw new KnWebhookRejectedException("invalid secure token", true);
        }
        JsonNode data = KnSepayProvider.readJson(body).path("data");
        List<KnBankTxn> result = new ArrayList<>();
        for (JsonNode item : data) {
            String id = item.path("tid").asText("");
            if (id.isEmpty()) {
                id = item.path("id").asText("");
            }
            if (id.isEmpty() || !item.path("amount").canConvertToLong()) {
                throw new KnWebhookRejectedException("missing id or amount", false);
            }
            long amount = item.path("amount").asLong();
            if (amount > 0) {
                result.add(new KnBankTxn(
                    NAME,
                    id,
                    BigDecimal.valueOf(amount),
                    item.path("description").asText("")
                ));
            }
        }
        return result;
    }
}
