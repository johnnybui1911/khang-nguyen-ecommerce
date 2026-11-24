package com.ecommerce.ext.payment;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import java.math.BigDecimal;
import org.youngmonkeys.ecommerce.model.CreatePaymentUrlModel;
import org.youngmonkeys.ecommerce.model.PaymentServiceSettingModel;
import org.youngmonkeys.ecommerce.service.PaymentService;

/**
 * Cash on delivery. The order stays waiting for payment and the shop confirms it
 * after delivery; orders above {@link #MAX_AMOUNT} are refused at checkout.
 */
@EzySingleton
public class KnCodPaymentService extends KnOfflinePaymentService {

    public static final String CODE = "KN_COD";
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("5000000");
    public static final String TOO_LARGE_MESSAGE =
        "Đơn hàng trên 5.000.000₫ không áp dụng thanh toán khi nhận hàng. "
            + "Vui lòng chọn chuyển khoản ngân hàng.";

    public KnCodPaymentService() {
        this(CODE);
    }

    private KnCodPaymentService(String code) {
        super(code);
    }

    @Override
    public String createPaymentUrl(
        PaymentServiceSettingModel setting,
        String secretKey,
        CreatePaymentUrlModel model
    ) {
        if (model.getAmount() == null || model.getAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new HttpBadRequestException(TOO_LARGE_MESSAGE);
        }
        return orderPageUrl(model.getOrderId());
    }

    @Override
    protected String orderPageMode() {
        return "cod";
    }

    @Override
    protected PaymentService newInstance(String newCode) {
        return new KnCodPaymentService(newCode);
    }
}
