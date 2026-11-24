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
    /** The transferred amount is below the order total. */
    UNDERPAID
}
