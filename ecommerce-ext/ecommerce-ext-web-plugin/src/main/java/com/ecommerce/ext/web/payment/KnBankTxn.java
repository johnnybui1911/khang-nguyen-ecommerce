package com.ecommerce.ext.web.payment;

import java.math.BigDecimal;

/** An incoming bank transaction, as reported by a confirming provider. */
public final class KnBankTxn {

    private final String provider;
    private final String txnId;
    private final BigDecimal amount;
    private final String content;

    public KnBankTxn(String provider, String txnId, BigDecimal amount, String content) {
        this.provider = provider;
        this.txnId = txnId;
        this.amount = amount;
        this.content = content;
    }

    public String getProvider() {
        return provider;
    }

    public String getTxnId() {
        return txnId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getContent() {
        return content;
    }
}
