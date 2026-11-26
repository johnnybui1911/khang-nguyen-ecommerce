package com.ecommerce.ext.web.payment;

/** The webhook request failed authentication or could not be understood. */
public class KnWebhookRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean unauthorized;

    public KnWebhookRejectedException(String message, boolean unauthorized) {
        super(message);
        this.unauthorized = unauthorized;
    }

    /** True when credentials were missing or wrong (401); false for a malformed body (400). */
    public boolean isUnauthorized() {
        return unauthorized;
    }
}
