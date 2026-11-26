package com.ecommerce.ext.web.payment;

/** What happened to a reported bank transaction. Stored in {@code ecm_payment_txn.result}. */
public enum KnTxnResult {
    /** The order was waiting and is now paid. */
    PAID,
    /** The same provider transaction was already processed. */
    DUPLICATE,
    /** The transfer content holds no order code. */
    NO_CODE,
    ORDER_NOT_FOUND,
    /** The order was not placed with bank transfer. */
    WRONG_METHOD,
    /** The order is not waiting for payment (already paid, cancelled, expired...). */
    NOT_WAITING,
    /** The money went to an account other than the shop's receiving account. */
    WRONG_ACCOUNT,
    /** The transferred amount is below the order total. */
    UNDERPAID,
    /** The one-tap confirmation link is malformed or its signature does not match. */
    INVALID_TOKEN,
    /** The one-tap confirmation link is older than its lifetime. */
    EXPIRED_TOKEN,
    /** The one-tap confirmation link was already used. */
    TOKEN_USED
}
