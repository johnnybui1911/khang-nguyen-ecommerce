package com.ecommerce.ext.web.payment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import com.ecommerce.ext.entity.KnPaymentTxn;
import com.ecommerce.ext.repo.KnPaymentTxnRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.web.service.WebOrderService;
import org.youngmonkeys.ezyplatform.web.event.WebEventHandlerManager;

public class KnPaymentConfirmServiceTest {

    private WebOrderService orderService;
    private KnPaymentTxnRepository repository;
    private KnPaymentConfirmService service;
    private KnPaymentMailer mailer;

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        orderService = mock(WebOrderService.class);
        repository = mock(KnPaymentTxnRepository.class);
        service = new KnPaymentConfirmService();
        inject(service, "orderService", orderService);
        inject(service, "txnRepository", repository);
        inject(service, "eventHandlerManager", mock(WebEventHandlerManager.class));
        mailer = mock(KnPaymentMailer.class);
        inject(service, "mailer", mailer);
    }

    private OrderModel order(String method, String status, String total) {
        OrderModel order = mock(OrderModel.class);
        when(order.getPaymentServiceCode()).thenReturn(method);
        when(order.getStatus()).thenReturn(status);
        when(order.getTotalAmountMoney()).thenReturn(new BigDecimal(total));
        when(orderService.getOrderById(12L)).thenReturn(order);
        return order;
    }

    private KnTxnResult pay(String content, String amount) {
        return service.handle(new KnBankTxn("sepay", "t1", new BigDecimal(amount), content));
    }

    @Test
    public void matchingCodeAndAmountMarksTheOrderPaid() {
        order("KN_BANK", "WAITING_FOR_PAYMENT", "250000");
        assertEquals(pay("chuyen khoan KN00012", "250000"), KnTxnResult.PAID);
        verify(orderService).approveOrder(0L, 0L, 12L);
        verify(repository).save(any(KnPaymentTxn.class));
        verify(mailer).sendPaymentReceived(any(OrderModel.class));
    }

    @Test
    public void overpaymentStillCounts() {
        order("KN_BANK", "WAITING_FOR_PAYMENT", "250000");
        assertEquals(pay("KN00012", "300000"), KnTxnResult.PAID);
    }

    @Test
    public void underpaymentLeavesTheOrderAlone() {
        order("KN_BANK", "WAITING_FOR_PAYMENT", "250000");
        assertEquals(pay("KN00012", "249999"), KnTxnResult.UNDERPAID);
        verify(orderService, never()).approveOrder(anyLong(), anyLong(), anyLong());
    }

    @Test
    public void unknownCodeAndUnknownOrderAreNotPaid() {
        assertEquals(pay("hello", "250000"), KnTxnResult.NO_CODE);
        assertEquals(pay("KN00099", "250000"), KnTxnResult.ORDER_NOT_FOUND);
        verify(orderService, never()).approveOrder(anyLong(), anyLong(), anyLong());
    }

    @Test
    public void ordersOfOtherMethodsOrStatesAreNotPaid() {
        order("KN_COD", "WAITING_FOR_PAYMENT", "10");
        assertEquals(pay("KN00012", "10"), KnTxnResult.WRONG_METHOD);
        order("KN_BANK", "PAID", "10");
        assertEquals(pay("KN00012", "10"), KnTxnResult.NOT_WAITING);
        order("KN_BANK", "REJECTED", "10");
        assertEquals(pay("KN00012", "10"), KnTxnResult.NOT_WAITING);
        verify(orderService, never()).approveOrder(anyLong(), anyLong(), anyLong());
    }

    @Test
    public void aTransactionAlreadySeenIsNotProcessedAgain() {
        order("KN_BANK", "WAITING_FOR_PAYMENT", "250000");
        when(repository.findByProviderAndTxnId("sepay", "t1")).thenReturn(new KnPaymentTxn());
        assertEquals(pay("KN00012", "250000"), KnTxnResult.DUPLICATE);
        verify(orderService, never()).approveOrder(anyLong(), anyLong(), anyLong());
    }
}
