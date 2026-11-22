package com.ecommerce.ext.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import com.ecommerce.ext.payment.EcmDemoPaymentService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;
import org.youngmonkeys.ecommerce.model.CreatePaymentUrlModel;

public class EcmDemoPaymentServiceTest {

    private final EcmDemoPaymentService service = new EcmDemoPaymentService();

    private Map<String, String> parametersOf(String url) {
        Map<String, String> parameters = new HashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] kv = pair.split("=");
            parameters.put(kv[0], kv[1]);
        }
        return parameters;
    }

    private String urlFor(String secret) throws Exception {
        return service.createPaymentUrl(
            null,
            secret,
            CreatePaymentUrlModel.builder()
                .orderId(7)
                .amount(new BigDecimal("129.00"))
                .build()
        );
    }

    @Test
    public void signedUrlVerifies() throws Exception {
        Map<String, String> parameters = parametersOf(urlFor("k"));
        assertNull(service.verifyPaymentRequestParameters(null, parameters, "k"));
        assertEquals(service.extractPaymentOrder(parameters).getOrderId(), 7L);
    }

    @Test
    public void tamperedAmountIsRejected() throws Exception {
        Map<String, String> parameters = parametersOf(urlFor("k"));
        parameters.put("amount", "1");
        assertNotNull(service.verifyPaymentRequestParameters(null, parameters, "k"));
    }

    @Test
    public void wrongSecretIsRejected() throws Exception {
        Map<String, String> parameters = parametersOf(urlFor("k"));
        assertNotNull(service.verifyPaymentRequestParameters(null, parameters, "other"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void blankSecretIsRefused() throws Exception {
        urlFor("");
    }

    @Test
    public void paidAmountMustEqualExpectedAmount() {
        org.youngmonkeys.ecommerce.constant.PaymentVerifyStatus ok = service.verifyPaymentResult(
            null, new BigDecimal("129.00"), new BigDecimal("129"), new HashMap<>()
        ).getStatus();
        org.youngmonkeys.ecommerce.constant.PaymentVerifyStatus bad = service.verifyPaymentResult(
            null, new BigDecimal("1"), new BigDecimal("129"), new HashMap<>()
        ).getStatus();
        assertEquals(ok, org.youngmonkeys.ecommerce.constant.PaymentVerifyStatus.SUCCESS);
        assertEquals(bad, org.youngmonkeys.ecommerce.constant.PaymentVerifyStatus.FAILED);
    }
}
