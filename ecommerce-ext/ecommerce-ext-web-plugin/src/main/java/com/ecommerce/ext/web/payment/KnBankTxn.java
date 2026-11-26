package com.ecommerce.ext.web.payment;

import java.math.BigDecimal;

/** An incoming bank transaction, as reported by a confirming provider. */
public final class KnBankTxn {

    private final String provider;
    private final String txnId;
    private final BigDecimal amount;
    private final String content;
    private final String accountNumber;
    private final String subAccount;

    public KnBankTxn(String provider, String txnId, BigDecimal amount, String content) {
        this(provider, txnId, amount, content, null, null);
    }

    /**
     * Creates a transaction that also names the account that received the money.
     *
     * @param accountNumber the receiving bank account, or blank when the provider omits it
     * @param subAccount    the receiving virtual account, or blank when there is none
     */
    public KnBankTxn(
        String provider,
        String txnId,
        BigDecimal amount,
        String content,
        String accountNumber,
        String subAccount
    ) {
        this.provider = provider;
        this.txnId = txnId;
        this.amount = amount;
        this.content = content;
        this.accountNumber = accountNumber == null ? "" : accountNumber;
        this.subAccount = subAccount == null ? "" : subAccount;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getSubAccount() {
        return subAccount;
    }
}
