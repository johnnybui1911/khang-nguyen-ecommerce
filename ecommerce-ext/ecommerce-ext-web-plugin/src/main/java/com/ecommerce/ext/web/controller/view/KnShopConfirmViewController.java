package com.ecommerce.ext.web.controller.view;

import com.ecommerce.ext.web.payment.KnShopConfirmService;
import com.ecommerce.ext.web.payment.KnTxnResult;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.DoPost;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;

/**
 * The page behind the "money received" button in the shop mail. Opening it (GET) only shows
 * what would be confirmed; the order changes on the POST of its confirm button, so mail
 * scanners and link previewers that fetch the link cannot mark anything paid.
 */
@Controller("/payment/shop-confirm")
public class KnShopConfirmViewController {

    private static final String HTML = "text/html; charset=utf-8";

    @EzyAutoBind
    private KnShopConfirmService service;

    @DoGet
    public ResponseEntity confirmGet(RequestArguments arguments) {
        String token = tokenOf(arguments);
        KnShopConfirmService.Preview preview = service.preview(token);
        if (preview.getProblem() != null) {
            return page(400, message(preview.getProblem(), preview.getOrderId()));
        }
        return page(200, "<h2>Xác nhận đã nhận tiền</h2>"
            + "<p>Đơn hàng <b>#" + preview.getOrderId() + "</b> sẽ chuyển sang trạng thái "
            + "Đã thanh toán và khách nhận thư xác nhận.</p>"
            + "<form method=\"post\" action=\"/payment/shop-confirm\">"
            + "<input type=\"hidden\" name=\"token\" value=\"" + escape(token) + "\">"
            + "<button type=\"submit\" style=\"background:#1a7f37;color:#fff;border:0;"
            + "padding:12px 22px;border-radius:4px;font-size:16px;cursor:pointer\">"
            + "Xác nhận đã nhận tiền</button></form>");
    }

    @DoPost
    public ResponseEntity confirmPost(RequestArguments arguments) {
        String token = tokenOf(arguments);
        long orderId = service.preview(token).getOrderId();
        KnTxnResult result = service.confirm(token);
        return page(result == KnTxnResult.PAID ? 200 : 400, message(result, orderId));
    }

    private static String tokenOf(RequestArguments arguments) {
        String token = arguments.getRequest().getParameter("token");
        return token == null ? "" : token.trim();
    }

    private static String message(KnTxnResult result, long orderId) {
        switch (result) {
            case PAID:
                return "<h2>Đã xác nhận</h2><p>Đơn <b>#" + orderId
                    + "</b> đã chuyển sang Đã thanh toán.</p>";
            case TOKEN_USED:
                return "<h2>Liên kết đã được dùng</h2><p>Đơn này đã được xác nhận trước đó.</p>";
            case EXPIRED_TOKEN:
                return "<h2>Liên kết đã hết hạn</h2><p>Hãy xác nhận trong trang quản trị.</p>";
            case NOT_WAITING:
                return "<h2>Đơn không còn chờ thanh toán</h2><p>Đơn đã được thanh toán, "
                    + "huỷ hoặc hết hạn.</p>";
            case WRONG_METHOD:
                return "<h2>Đơn không phải chuyển khoản</h2><p>Đơn này không thanh toán "
                    + "bằng chuyển khoản nên không xác nhận ở đây.</p>";
            case ORDER_NOT_FOUND:
                return "<h2>Không tìm thấy đơn hàng</h2>";
            default:
                return "<h2>Liên kết không hợp lệ</h2>";
        }
    }

    private static ResponseEntity page(int status, String body) {
        String html = "<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<meta name=\"robots\" content=\"noindex\"><title>Xác nhận thanh toán</title>"
            + "</head><body style=\"font-family:Arial,sans-serif;max-width:480px;"
            + "margin:48px auto;padding:0 16px\">" + body + "</body></html>";
        return ResponseEntity.status(status)
            .header("Content-Type", HTML)
            .header("Cache-Control", "no-store")
            .body(html)
            .build();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
