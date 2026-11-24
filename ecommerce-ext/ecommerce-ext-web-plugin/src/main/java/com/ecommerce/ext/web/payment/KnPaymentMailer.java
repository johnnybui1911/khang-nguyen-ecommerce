package com.ecommerce.ext.web.payment;

import com.ecommerce.ext.payment.KnBankTransferPaymentService;
import com.ecommerce.ext.payment.KnCodPaymentService;
import com.ecommerce.ext.web.guard.KnRateLimiter;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.util.EzyLoggable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;
import org.youngmonkeys.ezymail.entity.MailType;
import org.youngmonkeys.ezymail.model.MailModel;
import org.youngmonkeys.ezymail.web.mail.WebMailDeliver;
import org.youngmonkeys.ezyplatform.model.UserModel;
import org.youngmonkeys.ezyplatform.service.UserService;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.model.OrderRecipientModel;
import org.youngmonkeys.ecommerce.web.service.WebOrderRecipientService;

/**
 * Sends the two customer mails of the offline payment methods. Every failure is logged and
 * swallowed so a mail problem can never undo an order or a payment confirmation.
 *
 * <p>Anyone can type any delivery email at checkout, so the "order received" mail is limited per
 * address ({@code ecm.ratelimit.order-mail.max}, default 5, per
 * {@code ecm.ratelimit.order-mail.window.seconds}, default 3600). A "+tag" in the address counts
 * as the same mailbox. Over the limit the order is still placed and only the mail is skipped.
 */
@EzySingleton
public class KnPaymentMailer extends EzyLoggable {

    /**
     * Test-only switch: when it holds an email address, every mail goes there instead of to the
     * customer or the shop. Blank, "off" or anything that is not an email means no redirect, so a
     * live shop mails its real customers unless someone turns this on on purpose.
     */
    public static final String REDIRECT_SETTING = "kn_mail_redirect_to";

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final KnRateLimiter orderMailLimiter = new KnRateLimiter(
        intProperty("ecm.ratelimit.order-mail.max", 5),
        intProperty("ecm.ratelimit.order-mail.window.seconds", 3600) * 1000L
    );

    @EzyAutoBind
    private WebMailDeliver mailDeliver;

    @EzyAutoBind
    private UserService userService;

    @EzyAutoBind
    private KnPaymentSettings settings;

    @EzyAutoBind
    private KnBankTransferInfoService bankInfoService;

    @EzyAutoBind
    private WebOrderRecipientService orderRecipientService;

    @EzyAutoBind
    private KnOrderViewKeyService viewKeyService;

    public void sendOrderReceived(OrderModel order) {
        try {
            String method = order.getPaymentServiceCode();
            String body;
            if (KnBankTransferPaymentService.CODE.equals(method)) {
                body = receivedBankBody(order);
            } else if (KnCodPaymentService.CODE.equals(method)) {
                body = receivedCodBody(order);
            } else {
                return;
            }
            String to = recipient(order);
            if (to != null && orderMailLimiter.tryAcquire(
                mailboxKey(to), System.currentTimeMillis()) > 0) {
                logger.warn("order received mail of order {} skipped: too many to one mailbox",
                    order.getId());
                return;
            }
            send(order, "Khang Nguyễn đã nhận đơn hàng #"
                + order.getId(), body + orderLink(order));
        } catch (Exception e) {
            logger.warn("can not send the order received mail of order {}", order.getId(), e);
        }
    }

    public void sendPaymentReceived(OrderModel order) {
        try {
            String body = "<p>Chúng tôi đã nhận được "
                + "<b>" + money(order.getTotalAmountMoney()) + "</b> cho đơn hàng <b>#"
                + order.getId() + "</b>. Đơn đã được "
                + "chuyển sang trạng thái Đã thanh toán và "
                + "sẽ sớm được xử lý.</p>"
                + "<p>Cảm ơn bạn đã mua sắm.</p>";
            send(order, "Đã nhận thanh toán đơn hàng #"
                + order.getId(), body);
        } catch (Exception e) {
            logger.warn("can not send the payment received mail of order {}", order.getId(), e);
        }
    }

    /**
     * Asks the shop to confirm the money arrived; the button opens a page that changes nothing
     * until the shop presses the confirm button on it.
     */
    public void sendShopConfirmRequest(OrderModel order, String confirmUrl) {
        try {
            String to = shopRecipient();
            if (to == null) {
                logger.info("no shop email configured, skip confirm mail of order {}",
                    order.getId());
                return;
            }
            String body = "<p>Đơn <b>#" + order.getId() + "</b> chờ chuyển khoản "
                + "<b>" + money(order.getTotalAmountMoney()) + "</b>. Nội dung chuyển khoản: <b>"
                + escape(bankInfoService.build(order).getContent()) + "</b>.</p>"
                + "<p>Khi thấy tiền về tài khoản, bấm nút dưới đây:</p>"
                + "<p><a href=\"" + escape(confirmUrl) + "\" style=\"background:#1a7f37;"
                + "color:#fff;padding:10px 18px;text-decoration:none;border-radius:4px\">"
                + "Xác nhận đã nhận tiền</a></p>"
                + "<p>Liên kết dùng được một lần và hết hạn sau 72 giờ.</p>";
            deliver(to, "Đơn #" + order.getId() + " chờ xác nhận chuyển khoản", body);
        } catch (Exception e) {
            logger.warn("can not send the shop confirm mail of order {}", order.getId(), e);
        }
    }

    private String receivedBankBody(OrderModel order) {
        KnBankTransferInfo info = bankInfoService.build(order);
        StringBuilder html = new StringBuilder()
            .append("<p>Cảm ơn bạn đã đặt hàng. Đơn ")
            .append("<b>#").append(order.getId()).append("</b> đang chờ chuyển khoản ")
            .append("<b>").append(money(info.getAmount())).append("</b>.</p>");
        if (info.isConfigured()) {
            html.append("<table cellpadding=\"4\">")
                .append(row("Ngân hàng", info.getBankName()))
                .append(row("Số tài khoản", info.getAccountNumber()))
                .append(row("Chủ tài khoản", info.getAccountOwner()))
                .append(row("Số tiền", money(info.getAmount())))
                .append(row("Nội dung", info.getContent()))
                .append("</table>")
                .append("<p>Vui lòng ghi đúng nội dung chuyển khoản "
                    + "để đơn được xác nhận tự "
                    + "động.</p>");
        } else {
            html.append("<p>Thông tin tài khoản sẽ được "
                + "gửi sau. Nội dung chuyển khoản: <b>")
                .append(escape(info.getContent())).append("</b></p>");
        }
        return html.toString();
    }

    private String receivedCodBody(OrderModel order) {
        return "<p>Cảm ơn bạn đã đặt hàng. Đơn <b>#"
            + order.getId() + "</b> sẽ được thanh toán khi nhận "
            + "hàng (COD). Tổng tiền: <b>" + money(order.getTotalAmountMoney())
            + "</b>.</p>";
    }

    private void send(OrderModel order, String title, String body) {
        String to = recipient(order);
        if (to == null) {
            logger.info("order {} has no customer email, skip mail", order.getId());
            return;
        }
        deliver(to, title, body);
    }

    private void deliver(String to, String title, String body) {
        mailDeliver.send(
            MailModel.builder()
                .type(MailType.SYSTEM)
                .to(to)
                .title(title)
                .content("<div style=\"font-family:Arial,sans-serif\">" + body + "</div>")
                .build()
        );
    }

    /**
     * The mailbox an address lands in: lower case, without a "+tag" in the local part. Gmail
     * also ignores dots in the local part and treats googlemail.com as gmail.com.
     */
    static String mailboxKey(String email) {
        String lower = email.trim().toLowerCase(Locale.ROOT);
        int at = lower.lastIndexOf('@');
        if (at <= 0) {
            return lower;
        }
        String local = lower.substring(0, at);
        String domain = lower.substring(at + 1);
        int plus = local.indexOf('+');
        if (plus > 0) {
            local = local.substring(0, plus);
        }
        if ("gmail.com".equals(domain) || "googlemail.com".equals(domain)) {
            String undotted = local.replace(".", "");
            if (!undotted.isEmpty()) {
                return undotted + "@gmail.com";
            }
        }
        return local + "@" + domain;
    }

    private static int intProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String shopRecipient() {
        String redirect = redirectTarget(settings.get(REDIRECT_SETTING));
        if (redirect != null) {
            return redirect;
        }
        String shop = settings.get(KnPaymentSettings.SHOP_NOTIFY_EMAIL);
        return shop.isEmpty() ? null : shop;
    }

    /**
     * The customer address: the delivery email typed at checkout, which is the only address a guest
     * order has, else the email of the account that placed the order.
     */
    String recipient(OrderModel order) {
        String redirect = redirectTarget(settings.get(REDIRECT_SETTING));
        if (redirect != null) {
            return redirect;
        }
        OrderRecipientModel delivery = orderRecipientService.getOrderRecipientById(order.getId());
        String typed = delivery == null || delivery.getEmail() == null
            ? "" : delivery.getEmail().trim();
        if (EMAIL.matcher(typed).matches()) {
            return typed;
        }
        UserModel user = userService.getUserById(order.getCreatorUserId());
        String email = user == null ? null : user.getEmail();
        return email == null || email.trim().isEmpty() ? null : email.trim();
    }

    /** A button to the order page; a guest order's link carries its view key. */
    private String orderLink(OrderModel order) {
        String url = viewKeyService.orderUrl(order.getId(), order.getCreatorUserId());
        return "<p><a href=\"" + escape(url) + "\">Xem đơn hàng #" + order.getId() + "</a></p>";
    }

    /** The address to redirect every mail to, or null when the redirect is off. */
    static String redirectTarget(String value) {
        String v = value == null ? "" : value.trim();
        return EMAIL.matcher(v).matches() ? v : null;
    }

    private static String row(String label, String value) {
        return "<tr><td>" + label + "</td><td><b>" + escape(value) + "</b></td></tr>";
    }

    static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + "₫";
    }

    private static String escape(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
