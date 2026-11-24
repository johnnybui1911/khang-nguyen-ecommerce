package com.ecommerce.ext.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import com.ecommerce.ext.payment.KnBankTransferPaymentService;
import com.ecommerce.ext.payment.KnCodPaymentService;
import com.ecommerce.ext.payment.KnTransferCode;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import java.math.BigDecimal;
import java.util.HashMap;
import org.testng.annotations.Test;
import org.youngmonkeys.ecommerce.constant.PaymentVerifyStatus;
import org.youngmonkeys.ecommerce.model.CreatePaymentUrlModel;

public class KnOfflinePaymentServiceTest {

    private final KnCodPaymentService cod = new KnCodPaymentService();
    private final KnBankTransferPaymentService bank = new KnBankTransferPaymentService();

    private static CreatePaymentUrlModel model(long orderId, String amount) {
        return CreatePaymentUrlModel.builder()
            .orderId(orderId)
            .amount(new BigDecimal(amount))
            .build();
    }

    @Test
    public void codAcceptsUpToTheLimit() {
        assertEquals(
            cod.createPaymentUrl(null, null, model(12, "5000000")),
            "/orders/12?pay=cod"
        );
    }

    @Test
    public void codRejectsAboveTheLimit() {
        HttpBadRequestException e = expectThrows(
            HttpBadRequestException.class,
            () -> cod.createPaymentUrl(null, null, model(12, "5000001"))
        );
        assertNotNull(e.getData());
    }

    @Test
    public void bankHasNoAmountLimit() {
        assertEquals(
            bank.createPaymentUrl(null, null, model(3, "90000000")),
            "/orders/3?pay=bank"
        );
    }

    @Test
    public void callbackNeverConfirmsAnOfflineOrder() {
        assertNotNull(bank.verifyPaymentRequestParameters(null, new HashMap<>(), null));
        assertNotNull(cod.verifyPaymentRequestParameters(null, new HashMap<>(), null));
        assertEquals(
            bank.verifyPaymentResult(null, BigDecimal.TEN, BigDecimal.TEN, new HashMap<>())
                .getStatus(),
            PaymentVerifyStatus.FAILED
        );
    }

    @Test
    public void cloneKeepsTheBehaviourUnderANewCode() {
        assertEquals(cod.clone("X").getCode(), "X");
        expectThrows(
            HttpBadRequestException.class,
            () -> cod.clone("X").createPaymentUrl(null, null, model(1, "6000000"))
        );
    }

    @Test
    public void transferCodeRoundTrips() {
        assertEquals(KnTransferCode.of(12), "KN00012");
        assertEquals(KnTransferCode.extractOrderId("KN00012").get(), Long.valueOf(12));
        assertEquals(
            KnTransferCode.extractOrderId("MBVCB.99.kn00012.CT tu A").get(),
            Long.valueOf(12)
        );
        assertEquals(KnTransferCode.extractOrderId("KN1234567").get(), Long.valueOf(1234567));
    }

    @Test
    public void transferCodeIgnoresLookalikes() {
        assertFalse(KnTransferCode.extractOrderId(null).isPresent());
        assertFalse(KnTransferCode.extractOrderId("").isPresent());
        assertFalse(KnTransferCode.extractOrderId("KN123").isPresent());
        assertFalse(KnTransferCode.extractOrderId("XKN00012").isPresent());
        assertTrue(KnTransferCode.extractOrderId("thanh toan KN00012 cam on").isPresent());
    }
}
