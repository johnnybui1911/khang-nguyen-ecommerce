package com.ecommerce.ext.web.payment;

import java.math.BigDecimal;
import lombok.Getter;

/** What a customer needs to pay one order by bank transfer. */
@Getter
public class KnBankTransferInfo {

    private final boolean configured;
    private final String bankName;
    private final String accountNumber;
    private final String accountOwner;
    private final BigDecimal amount;
    private final String content;
    private final String qrUrl;

    public KnBankTransferInfo(
        boolean configured,
        String bankName,
        String accountNumber,
        String accountOwner,
        BigDecimal amount,
        String content,
        String qrUrl
    ) {
        this.configured = configured;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountOwner = accountOwner;
        this.amount = amount;
        this.content = content;
        this.qrUrl = qrUrl;
    }
}
