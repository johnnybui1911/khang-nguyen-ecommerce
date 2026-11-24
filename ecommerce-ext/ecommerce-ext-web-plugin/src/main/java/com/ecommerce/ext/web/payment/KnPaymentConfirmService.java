package com.ecommerce.ext.web.payment;

import com.ecommerce.ext.entity.KnPaymentTxn;
import com.ecommerce.ext.payment.KnBankTransferPaymentService;
import com.ecommerce.ext.payment.KnTransferCode;
import com.ecommerce.ext.repo.KnPaymentTxnRepository;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.util.EzyLoggable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.youngmonkeys.ecommerce.constant.OrderAction;
import org.youngmonkeys.ecommerce.entity.OrderStatus;
import org.youngmonkeys.ecommerce.event.OrderActionEvent;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.web.service.WebOrderService;
import org.youngmonkeys.ezyplatform.web.event.WebEventHandlerManager;

/**
 * The single place where a bank transfer turns an order into "paid". Every provider (webhook
 * or poll) ends up in {@link #handle}; the admin order screen remains the manual fallback.
 */
@EzySingleton
public class KnPaymentConfirmService extends EzyLoggable {

    private static final int MAX_CONTENT = 500;

    @EzyAutoBind
    private WebOrderService orderService;

    @EzyAutoBind
    private KnPaymentTxnRepository txnRepository;

    @EzyAutoBind
    private WebEventHandlerManager eventHandlerManager;

    @EzyAutoBind
    private KnPaymentMailer mailer;

    private final Object lock = new Object();

    /**
     * Records an incoming bank transaction exactly once and marks the matching order paid
     * when the transfer code, amount and order state all agree.
     */
    public KnTxnResult handle(KnBankTxn txn) {
        synchronized (lock) {
            if (txnRepository.findByProviderAndTxnId(txn.getProvider(), txn.getTxnId()) != null) {
                return KnTxnResult.DUPLICATE;
            }
            Optional<Long> orderId = KnTransferCode.extractOrderId(txn.getContent());
            KnTxnResult result = orderId.isPresent()
                ? markPaid(orderId.get(), txn.getAmount(), txn.getProvider())
                : KnTxnResult.NO_CODE;
            record(txn, orderId.orElse(0L), result);
            return result;
        }
    }

    /**
     * Marks the order paid when it was placed with bank transfer, is still waiting and the
     * amount covers the total the server computed for it.
     *
     * @param source who reported the money, for the log (provider name)
     */
    public KnTxnResult markPaid(long orderId, BigDecimal amount, String source) {
        synchronized (lock) {
            OrderModel order = orderService.getOrderById(orderId);
            if (order == null) {
                return KnTxnResult.ORDER_NOT_FOUND;
            }
            if (!KnBankTransferPaymentService.CODE.equals(order.getPaymentServiceCode())) {
                return KnTxnResult.WRONG_METHOD;
            }
            if (!OrderStatus.isPayableOrderStatus(order.getStatus())) {
                return KnTxnResult.NOT_WAITING;
            }
            if (amount == null || amount.compareTo(order.getTotalAmountMoney()) < 0) {
                return KnTxnResult.UNDERPAID;
            }
            orderService.approveOrder(0, 0, orderId);
            eventHandlerManager.handleEvent(new OrderActionEvent(orderId, OrderAction.APPROVED));
            logger.info("order {} marked paid by {}", orderId, source);
            mailer.sendPaymentReceived(order);
            return KnTxnResult.PAID;
        }
    }

    private void record(KnBankTxn txn, long orderId, KnTxnResult result) {
        try {
            KnPaymentTxn entity = new KnPaymentTxn();
            entity.setProvider(txn.getProvider());
            entity.setTxnId(txn.getTxnId());
            entity.setOrderId(orderId);
            entity.setAmount(txn.getAmount().longValue());
            String content = txn.getContent() == null ? "" : txn.getContent();
            entity.setContent(content.length() > MAX_CONTENT
                ? content.substring(0, MAX_CONTENT) : content);
            entity.setResult(result.name());
            entity.setCreatedAt(LocalDateTime.now());
            txnRepository.save(entity);
        } catch (Exception e) {
            logger.warn("can not record payment txn {}/{}", txn.getProvider(), txn.getTxnId(), e);
        }
    }
}
