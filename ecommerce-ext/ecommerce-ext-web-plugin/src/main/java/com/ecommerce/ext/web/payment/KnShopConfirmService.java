package com.ecommerce.ext.web.payment;

import com.ecommerce.ext.entity.KnPaymentTxn;
import com.ecommerce.ext.repo.KnPaymentTxnRepository;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.util.EzyLoggable;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.function.LongSupplier;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.web.service.WebOrderService;

/**
 * One-tap "money received" confirmation for bank-transfer orders. The shop gets a signed link
 * per order; opening it only previews, and the confirmation itself goes through the same
 * {@link KnPaymentConfirmService#markPaid} path as a provider webhook. A link works once and
 * for {@link #TTL_SECONDS}; its nonce is recorded in {@code ecm_payment_txn}, whose unique
 * (provider, txn_id) key is what makes it single-use.
 */
@EzySingleton
public class KnShopConfirmService extends EzyLoggable {

    public static final String SOURCE = "shop-confirm";
    public static final long TTL_SECONDS = 72 * 3600L;
    private static final int NONCE_BYTES = 12;
    private static final int PARTS = 4;

    @EzyAutoBind
    private KnPaymentSettings settings;

    @EzyAutoBind
    private WebOrderService orderService;

    @EzyAutoBind
    private KnPaymentConfirmService confirmService;

    @EzyAutoBind
    private KnPaymentTxnRepository txnRepository;

    private final SecureRandom random = new SecureRandom();
    private final Object lock = new Object();
    private LongSupplier clock = () -> System.currentTimeMillis() / 1000;

    /** What a confirmation link points at, or why it can not be used. */
    public static final class Preview {
        private final KnTxnResult problem;
        private final long orderId;

        Preview(KnTxnResult problem, long orderId) {
            this.problem = problem;
            this.orderId = orderId;
        }

        /** The reason the link is unusable, or null when it can be confirmed. */
        public KnTxnResult getProblem() {
            return problem;
        }

        public long getOrderId() {
            return orderId;
        }
    }

    /** Returns a signed token for the order, or null while no signing key is configured. */
    public String issueToken(long orderId) {
        String key = settings.get(KnPaymentSettings.SHOP_CONFIRM_KEY);
        if (key.isEmpty()) {
            return null;
        }
        byte[] bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);
        StringBuilder nonce = new StringBuilder();
        for (byte b : bytes) {
            nonce.append(String.format("%02x", b));
        }
        String body = orderId + "." + (clock.getAsLong() + TTL_SECONDS) + "." + nonce;
        return body + "." + KnSecrets.hmacSha256Hex(key, body);
    }

    /** Checks the token without changing anything. */
    public Preview preview(String token) {
        Parsed parsed = parse(token);
        if (parsed.problem != null) {
            return new Preview(parsed.problem, parsed.orderId);
        }
        if (isUsed(parsed.nonce)) {
            return new Preview(KnTxnResult.TOKEN_USED, parsed.orderId);
        }
        return new Preview(null, parsed.orderId);
    }

    /**
     * Marks the order paid for the total the server holds, because the shop is the one saying
     * the money arrived. The token is consumed only when the order becomes paid.
     */
    public KnTxnResult confirm(String token) {
        Parsed parsed = parse(token);
        if (parsed.problem != null) {
            return parsed.problem;
        }
        synchronized (lock) {
            if (isUsed(parsed.nonce)) {
                return KnTxnResult.TOKEN_USED;
            }
            OrderModel order = orderService.getOrderById(parsed.orderId);
            if (order == null) {
                return KnTxnResult.ORDER_NOT_FOUND;
            }
            BigDecimal total = order.getTotalAmountMoney();
            KnTxnResult result = confirmService.markPaid(parsed.orderId, total, SOURCE);
            if (result == KnTxnResult.PAID) {
                consume(parsed, total);
            }
            return result;
        }
    }

    private boolean isUsed(String nonce) {
        return txnRepository.findByProviderAndTxnId(SOURCE, nonce) != null;
    }

    private void consume(Parsed parsed, BigDecimal total) {
        KnPaymentTxn entity = new KnPaymentTxn();
        entity.setProvider(SOURCE);
        entity.setTxnId(parsed.nonce);
        entity.setOrderId(parsed.orderId);
        entity.setAmount(total.longValue());
        entity.setContent("shop confirmed the transfer");
        entity.setResult(KnTxnResult.PAID.name());
        entity.setCreatedAt(LocalDateTime.now());
        txnRepository.save(entity);
    }

    private Parsed parse(String token) {
        String key = settings.get(KnPaymentSettings.SHOP_CONFIRM_KEY);
        if (key.isEmpty() || token == null) {
            return Parsed.problem(KnTxnResult.INVALID_TOKEN);
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != PARTS) {
            return Parsed.problem(KnTxnResult.INVALID_TOKEN);
        }
        String body = parts[0] + "." + parts[1] + "." + parts[2];
        if (!KnSecrets.sameSecret(KnSecrets.hmacSha256Hex(key, body), parts[3])) {
            return Parsed.problem(KnTxnResult.INVALID_TOKEN);
        }
        try {
            long orderId = Long.parseLong(parts[0]);
            long expiresAt = Long.parseLong(parts[1]);
            if (clock.getAsLong() > expiresAt) {
                return new Parsed(KnTxnResult.EXPIRED_TOKEN, orderId, parts[2]);
            }
            return new Parsed(null, orderId, parts[2]);
        } catch (NumberFormatException e) {
            return Parsed.problem(KnTxnResult.INVALID_TOKEN);
        }
    }

    private static final class Parsed {
        private final KnTxnResult problem;
        private final long orderId;
        private final String nonce;

        Parsed(KnTxnResult problem, long orderId, String nonce) {
            this.problem = problem;
            this.orderId = orderId;
            this.nonce = nonce;
        }

        static Parsed problem(KnTxnResult problem) {
            return new Parsed(problem, 0, "");
        }
    }
}
