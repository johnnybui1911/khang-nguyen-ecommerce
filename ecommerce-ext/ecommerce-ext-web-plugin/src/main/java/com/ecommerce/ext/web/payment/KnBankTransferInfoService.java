package com.ecommerce.ext.web.payment;

import com.ecommerce.ext.payment.KnTransferCode;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.util.EzyLoggable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import org.youngmonkeys.ecommerce.model.BankModel;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.model.ShopBankAccountModel;
import org.youngmonkeys.ecommerce.web.service.WebBankService;
import org.youngmonkeys.ecommerce.web.service.WebShopBankAccountService;

/**
 * Builds the transfer instructions for an order. The amount always comes from the order the
 * server stored, never from the client. While the shop account is still the placeholder the
 * result is flagged as not configured and carries no account details or QR.
 */
@EzySingleton
public class KnBankTransferInfoService extends EzyLoggable {

    public static final String PLACEHOLDER_ACCOUNT = "PLACEHOLDER";

    private static final long SHOP_ID = 1L;
    private static final String QR_URL = "https://img.vietqr.io/image/%s-%s-compact2.png"
        + "?amount=%s&addInfo=%s&accountName=%s";

    @EzyAutoBind
    private WebShopBankAccountService shopBankAccountService;

    @EzyAutoBind
    private WebBankService bankService;

    public KnBankTransferInfo build(OrderModel order) {
        BigDecimal amount = order.getTotalAmountMoney();
        String content = KnTransferCode.of(order.getId());
        ShopBankAccountModel account = null;
        try {
            account = shopBankAccountService.getTopActivatedBankAccountByShopId(SHOP_ID);
        } catch (Exception e) {
            logger.warn("can not load the shop bank account", e);
        }
        if (account == null || isPlaceholder(account.getAccountNumber())) {
            return new KnBankTransferInfo(false, null, null, null, amount, content, null);
        }
        BankModel bank = bankService.getBankById(account.getBankId());
        String bankName = bank == null ? "" : bank.getName();
        String qr = bank == null || bank.getBinCode() == null || bank.getBinCode().isEmpty()
            ? null
            : String.format(
                QR_URL,
                bank.getBinCode(),
                encode(account.getAccountNumber()),
                amount.setScale(0, java.math.RoundingMode.CEILING).toPlainString(),
                encode(content),
                encode(account.getAccountOwnerName())
            );
        return new KnBankTransferInfo(
            true,
            bankName,
            account.getAccountNumber(),
            account.getAccountOwnerName(),
            amount,
            content,
            qr
        );
    }

    private static boolean isPlaceholder(String accountNumber) {
        return accountNumber == null
            || accountNumber.trim().isEmpty()
            || PLACEHOLDER_ACCOUNT.equalsIgnoreCase(accountNumber.trim());
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
