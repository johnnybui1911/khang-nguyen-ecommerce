package com.ecommerce.ext.web.payment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import com.ecommerce.ext.entity.KnPaymentTxn;
import com.ecommerce.ext.repo.KnPaymentTxnRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.web.service.WebOrderService;

public class KnShopConfirmServiceTest {

    private static final long NOW = 1_800_000_000L;
    private static final long TTL = 72 * 3600L;

    private KnPaymentSettings settings;
    private WebOrderService orderService;
    private KnPaymentConfirmService confirmService;
    private KnPaymentTxnRepository repository;
    private KnShopConfirmService service;
    private long now;

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        now = NOW;
        settings = mock(KnPaymentSettings.class);
        when(settings.get(KnPaymentSettings.SHOP_CONFIRM_KEY)).thenReturn("test-key-1");
        orderService = mock(WebOrderService.class);
        confirmService = mock(KnPaymentConfirmService.class);
        repository = mock(KnPaymentTxnRepository.class);
        Map<String, KnPaymentTxn> saved = new HashMap<>();
        doAnswer(call -> {
            KnPaymentTxn txn = (KnPaymentTxn) call.getArguments()[0];
            saved.put(txn.getProvider() + "/" + txn.getTxnId(), txn);
            return txn;
        }).when(repository).save(any(KnPaymentTxn.class));
        when(repository.findByProviderAndTxnId(anyString(), anyString())).thenAnswer(
            call -> saved.get(call.getArguments()[0] + "/" + call.getArguments()[1])
        );
        service = new KnShopConfirmService();
        inject(service, "settings", settings);
        inject(service, "orderService", orderService);
        inject(service, "confirmService", confirmService);
        inject(service, "txnRepository", repository);
        inject(service, "clock", (LongSupplier) () -> now);
    }

    private void orderWithTotal(long id, String total) {
        OrderModel order = mock(OrderModel.class);
        when(order.getTotalAmountMoney()).thenReturn(new BigDecimal(total));
        when(orderService.getOrderById(id)).thenReturn(order);
        when(confirmService.markPaid(id, new BigDecimal(total), KnShopConfirmService.SOURCE))
            .thenReturn(KnTxnResult.PAID);
    }

    @Test
    public void aValidTokenMarksTheOrderPaid() {
        orderWithTotal(12L, "250000");
        String token = service.issueToken(12L);
        assertEquals(service.confirm(token), KnTxnResult.PAID);
        verify(confirmService).markPaid(12L, new BigDecimal("250000"), KnShopConfirmService.SOURCE);
    }

    @Test
    public void aTokenWorksOnlyOnce() {
        orderWithTotal(12L, "250000");
        String token = service.issueToken(12L);
        assertEquals(service.confirm(token), KnTxnResult.PAID);
        assertEquals(service.confirm(token), KnTxnResult.TOKEN_USED);
        verify(confirmService, org.mockito.Mockito.times(1))
            .markPaid(12L, new BigDecimal("250000"), KnShopConfirmService.SOURCE);
    }

    @Test
    public void anExpiredTokenIsRejected() {
        orderWithTotal(12L, "250000");
        String token = service.issueToken(12L);
        now = NOW + TTL + 1;
        assertEquals(service.confirm(token), KnTxnResult.EXPIRED_TOKEN);
        verify(confirmService, never()).markPaid(12L, new BigDecimal("250000"),
            KnShopConfirmService.SOURCE);
    }

    @Test
    public void aTokenStillWorksJustBeforeItExpires() {
        orderWithTotal(12L, "250000");
        String token = service.issueToken(12L);
        now = NOW + TTL;
        assertEquals(service.confirm(token), KnTxnResult.PAID);
    }

    @Test
    public void aTamperedOrForeignSignatureIsRejected() throws Exception {
        orderWithTotal(12L, "250000");
        orderWithTotal(13L, "10");
        String token = service.issueToken(12L);
        String[] parts = token.split("\\.");
        String forged = "13." + parts[1] + "." + parts[2] + "." + parts[3];
        assertEquals(service.confirm(forged), KnTxnResult.INVALID_TOKEN);
        assertEquals(service.confirm(token + "0"), KnTxnResult.INVALID_TOKEN);
        assertEquals(service.confirm("garbage"), KnTxnResult.INVALID_TOKEN);
        assertEquals(service.confirm(""), KnTxnResult.INVALID_TOKEN);
        assertEquals(service.confirm(null), KnTxnResult.INVALID_TOKEN);

        KnShopConfirmService other = new KnShopConfirmService();
        KnPaymentSettings otherSettings = mock(KnPaymentSettings.class);
        when(otherSettings.get(KnPaymentSettings.SHOP_CONFIRM_KEY)).thenReturn("another-key");
        inject(other, "settings", otherSettings);
        inject(other, "clock", (LongSupplier) () -> now);
        assertEquals(service.confirm(other.issueToken(12L)), KnTxnResult.INVALID_TOKEN);
        verify(confirmService, never()).markPaid(12L, new BigDecimal("250000"),
            KnShopConfirmService.SOURCE);
    }

    @Test
    public void withoutAKeyNothingIsIssuedOrAccepted() {
        orderWithTotal(12L, "250000");
        String token = service.issueToken(12L);
        when(settings.get(KnPaymentSettings.SHOP_CONFIRM_KEY)).thenReturn("");
        assertNull(service.issueToken(12L));
        assertEquals(service.confirm(token), KnTxnResult.INVALID_TOKEN);
    }

    @Test
    public void lookingAtATokenDoesNotChangeAnything() {
        orderWithTotal(12L, "250000");
        String token = service.issueToken(12L);
        KnShopConfirmService.Preview preview = service.preview(token);
        assertNull(preview.getProblem());
        assertEquals(preview.getOrderId(), 12L);
        assertEquals(service.preview(token).getOrderId(), 12L);
        verify(confirmService, never()).markPaid(12L, new BigDecimal("250000"),
            KnShopConfirmService.SOURCE);
        verify(repository, never()).save(any(KnPaymentTxn.class));
        assertEquals(service.confirm(token), KnTxnResult.PAID);
        assertEquals(service.preview(token).getProblem(), KnTxnResult.TOKEN_USED);
    }

    @Test
    public void aCodOrderIsRefusedAndTheTokenStaysUsable() {
        OrderModel order = mock(OrderModel.class);
        when(order.getTotalAmountMoney()).thenReturn(new BigDecimal("10"));
        when(orderService.getOrderById(12L)).thenReturn(order);
        when(confirmService.markPaid(12L, new BigDecimal("10"), KnShopConfirmService.SOURCE))
            .thenReturn(KnTxnResult.WRONG_METHOD);
        String token = service.issueToken(12L);
        assertEquals(service.confirm(token), KnTxnResult.WRONG_METHOD);
        assertNotNull(token);
        assertNull(service.preview(token).getProblem());
    }

    @Test
    public void aMissingOrderIsReported() {
        String token = service.issueToken(99L);
        assertEquals(service.confirm(token), KnTxnResult.ORDER_NOT_FOUND);
        assertTrue(token.startsWith("99."));
    }
}
