package com.ecommerce.ext.payment;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.youngmonkeys.ecommerce.constant.PaymentVerifyStatus;
import org.youngmonkeys.ecommerce.model.CreatePaymentUrlModel;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.model.PaymentOrderModel;
import org.youngmonkeys.ecommerce.model.PaymentServiceSettingModel;
import org.youngmonkeys.ecommerce.model.PaymentVerifyResponseModel;
import org.youngmonkeys.ecommerce.model.PaymentVerifyResultModel;
import org.youngmonkeys.ecommerce.service.PaymentService;

/**
 * Demo payment method: the returned URL is signed with the service secret key, so only
 * the amount and order issued by checkout can be confirmed through the callback.
 */
@EzySingleton
public class KnDemoPaymentService implements PaymentService {

    public static final String CODE = "ECM_DEMO";

    private final String code;

    public KnDemoPaymentService() {
        this(CODE);
    }

    private KnDemoPaymentService(String code) {
        this.code = code;
    }

    @Override
    public String createPaymentUrl(
        PaymentServiceSettingModel setting,
        String secretKey,
        CreatePaymentUrlModel model
    ) {
        String amount = model.getAmount().toPlainString();
        String orderId = String.valueOf(model.getOrderId());
        return "/callback/" + code
            + "?orderId=" + orderId
            + "&amount=" + amount
            + "&sig=" + sign(secretKey, orderId, amount);
    }

    @Override
    public PaymentVerifyResponseModel verifyPaymentRequestParameters(
        Enumeration<String> parameterNames,
        Map<String, String> parameters,
        String secretKey
    ) {
        String expected = sign(
            secretKey,
            parameters.get("orderId"),
            parameters.get("amount")
        );
        String actual = parameters.getOrDefault("sig", "");
        boolean valid = MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8)
        );
        return valid ? null : PaymentVerifyResponseModel.builder()
            .code(400)
            .data("invalid signature")
            .build();
    }

    @Override
    public PaymentOrderModel extractPaymentOrder(Map<String, String> parameters) {
        return PaymentOrderModel.builder()
            .orderId(Long.parseLong(parameters.get("orderId")))
            .amount(new BigDecimal(parameters.get("amount")))
            .build();
    }

    @Override
    public PaymentVerifyResultModel verifyPaymentResult(
        OrderModel order,
        BigDecimal paidAmount,
        BigDecimal expectedAmount,
        Map<String, String> parameters
    ) {
        boolean amountMatches = paidAmount != null
            && expectedAmount != null
            && paidAmount.compareTo(expectedAmount) == 0;
        return PaymentVerifyResultModel.builder()
            .status(amountMatches ? PaymentVerifyStatus.SUCCESS : PaymentVerifyStatus.FAILED)
            .build();
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public PaymentService clone(String newCode) {
        return new KnDemoPaymentService(newCode);
    }

    private static String sign(String secretKey, String orderId, String amount) {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("payment service secret key is not configured");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            ));
            byte[] raw = mac.doFinal(
                (orderId + "|" + amount).getBytes(StandardCharsets.UTF_8)
            );
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
