package com.ecommerce.ext.web.payment;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzyPostInit;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.util.EzyLoggable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Safety net for lost webhooks: every minute, providers that can be polled are asked for
 * their recent transactions. Already processed transactions are skipped by
 * {@link KnPaymentConfirmService}, so polling is always safe to repeat.
 */
@EzySingleton
public class KnPaymentPollJob extends EzyLoggable {

    static final long INTERVAL_SECONDS = 60;

    @EzyAutoBind
    private KnBankTxnProviders providers;

    @EzyAutoBind
    private KnPaymentConfirmService confirmService;

    @EzyPostInit
    public void start() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "kn-payment-poll");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
            this::pollOnce,
            INTERVAL_SECONDS,
            INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    void pollOnce() {
        for (KnBankTxnProvider provider : providers.all()) {
            if (!provider.isPollable()) {
                continue;
            }
            try {
                for (KnBankTxn txn : provider.poll()) {
                    confirmService.handle(txn);
                }
            } catch (Exception e) {
                logger.warn("poll {} failed: {}", provider.getName(), e.getMessage());
            }
        }
    }
}
