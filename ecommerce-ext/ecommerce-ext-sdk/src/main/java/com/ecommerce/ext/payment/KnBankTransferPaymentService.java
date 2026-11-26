package com.ecommerce.ext.payment;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ecommerce.model.CreatePaymentUrlModel;
import org.youngmonkeys.ecommerce.model.PaymentServiceSettingModel;
import org.youngmonkeys.ecommerce.service.PaymentService;

/**
 * Bank transfer. The customer pays with the transfer content from
 * {@link KnTransferCode}; the order is marked paid when the bank reports the
 * matching transaction.
 */
@EzySingleton
public class KnBankTransferPaymentService extends KnOfflinePaymentService {

    public static final String CODE = "KN_BANK";

    public KnBankTransferPaymentService() {
        this(CODE);
    }

    private KnBankTransferPaymentService(String code) {
        super(code);
    }

    @Override
    public String createPaymentUrl(
        PaymentServiceSettingModel setting,
        String secretKey,
        CreatePaymentUrlModel model
    ) {
        return orderPageUrl(model.getOrderId());
    }

    @Override
    protected String orderPageMode() {
        return "bank";
    }

    @Override
    protected PaymentService newInstance(String newCode) {
        return new KnBankTransferPaymentService(newCode);
    }
}
