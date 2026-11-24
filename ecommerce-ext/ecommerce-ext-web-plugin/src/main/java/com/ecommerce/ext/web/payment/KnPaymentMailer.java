package com.ecommerce.ext.web.payment;

import com.ecommerce.ext.payment.KnBankTransferPaymentService;
import com.ecommerce.ext.payment.KnCodPaymentService;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.util.EzyLoggable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.youngmonkeys.ezymail.entity.MailType;
import org.youngmonkeys.ezymail.model.MailModel;
import org.youngmonkeys.ezymail.web.mail.WebMailDeliver;
import org.youngmonkeys.ezyplatform.model.UserModel;
import org.youngmonkeys.ezyplatform.service.UserService;
import org.youngmonkeys.ecommerce.model.OrderModel;

/**
 * Sends the two customer mails of the offline payment methods. Every failure is logged and
 * swallowed so a mail problem can never undo an order or a payment confirmation.
 */
@EzySingleton
public class KnPaymentMailer extends EzyLoggable {

    public static final String REDIRECT_SETTING = "kn_mail_redirect_to";
    public static final String DEFAULT_REDIRECT = "khangnguyen.shop.hn@gmail.com";
    private static final String REDIRECT_OFF = "off";

    @EzyAutoBind
    private WebMailDeliver mailDeliver;

    @EzyAutoBind
    private UserService userService;

    @EzyAutoBind
    private KnPaymentSettings settings;

    @EzyAutoBind
    private KnBankTransferInfoService bankInfoService;

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
            send(order, "Khang Nguyễn đã nhận đơn hàng #"
                + order.getId(), body);
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

    private String shopRecipient() {
        String redirect = settings.get(REDIRECT_SETTING);
        if (redirect.isEmpty()) {
            redirect = DEFAULT_REDIRECT;
        }
        if (!REDIRECT_OFF.equalsIgnoreCase(redirect)) {
            return redirect;
        }
        String shop = settings.get(KnPaymentSettings.SHOP_NOTIFY_EMAIL);
        return shop.isEmpty() ? null : shop;
    }

    private String recipient(OrderModel order) {
        String redirect = settings.get(REDIRECT_SETTING);
        if (redirect.isEmpty()) {
            redirect = DEFAULT_REDIRECT;
        }
        if (!REDIRECT_OFF.equalsIgnoreCase(redirect)) {
            return redirect;
        }
        UserModel user = userService.getUserById(order.getCreatorUserId());
        String email = user == null ? null : user.getEmail();
        return email == null || email.trim().isEmpty() ? null : email.trim();
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
