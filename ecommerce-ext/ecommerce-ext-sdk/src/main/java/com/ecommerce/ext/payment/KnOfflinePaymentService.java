package com.ecommerce.ext.payment;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.Map;
import org.youngmonkeys.ecommerce.constant.PaymentVerifyStatus;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.model.PaymentOrderModel;
import org.youngmonkeys.ecommerce.model.PaymentVerifyResponseModel;
import org.youngmonkeys.ecommerce.model.PaymentVerifyResultModel;
import org.youngmonkeys.ecommerce.service.PaymentService;

/**
 * Base of payment methods that are settled outside the site (cash on delivery,
 * bank transfer). Checkout sends the customer to the order page, and the order
 * is never marked paid through a browser callback: the public callback endpoint
 * always rejects these methods.
 */
public abstract class KnOfflinePaymentService implements PaymentService {

    private final String code;

    protected KnOfflinePaymentService(String code) {
        this.code = code;
    }

    /** Value of the {@code pay} query parameter on the order page. */
    protected abstract String orderPageMode();

    protected abstract PaymentService newInstance(String newCode);

    protected final String orderPageUrl(long orderId) {
        return "/orders/" + orderId + "?pay=" + orderPageMode();
    }

    @Override
    public PaymentVerifyResponseModel verifyPaymentRequestParameters(
        Enumeration<String> parameterNames,
        Map<String, String> parameters,
        String secretKey
    ) {
        return PaymentVerifyResponseModel.builder()
            .code(400)
            .data("offline payment cannot be confirmed by callback")
            .build();
    }

    @Override
    public PaymentOrderModel extractPaymentOrder(Map<String, String> parameters) {
        throw new UnsupportedOperationException("offline payment has no callback");
    }

    @Override
    public PaymentVerifyResultModel verifyPaymentResult(
        OrderModel order,
        BigDecimal paidAmount,
        BigDecimal expectedAmount,
        Map<String, String> parameters
    ) {
        return PaymentVerifyResultModel.builder()
            .status(PaymentVerifyStatus.FAILED)
            .build();
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public PaymentService clone(String newCode) {
        return newInstance(newCode);
    }
}
