package com.ecommerce.ext.web.payment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A source of incoming bank transactions. Each provider authenticates and parses its own
 * webhook; the result is handed to {@link KnPaymentConfirmService}. A provider whose key is
 * not configured is disabled and rejects every webhook.
 */
public interface KnBankTxnProvider {

    /** Short stable name, used in the webhook path and as the transaction's provider. */
    String getName();

    /** False while the provider's key is empty. */
    boolean isEnabled();

    /**
     * Authenticates the request and returns the incoming transactions it carries.
     *
     * @param headers request headers keyed by lower-case name
     * @throws KnWebhookRejectedException when authentication fails or the body is malformed
     */
    List<KnBankTxn> parseWebhook(Map<String, String> headers, String body);

    /** Pulls recent transactions from the provider's API; empty when polling is unsupported. */
    default List<KnBankTxn> poll() {
        return Collections.emptyList();
    }

    /** True when the provider can be polled (token configured). */
    default boolean isPollable() {
        return false;
    }
}
