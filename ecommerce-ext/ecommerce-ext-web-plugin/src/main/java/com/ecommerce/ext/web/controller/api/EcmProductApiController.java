package com.ecommerce.ext.web.controller.api;

import com.ecommerce.ext.web.guard.EcmRateLimiter;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.core.exception.HttpTooManyRequestsException;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.RequestParam;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.model.OrderRecipientModel;
import org.youngmonkeys.ecommerce.response.OrderProductResponse;
import org.youngmonkeys.ecommerce.web.controller.decorator.WebOrderModelDecorator;
import org.youngmonkeys.ecommerce.web.response.WebOrderDetailsResponse;
import org.youngmonkeys.ecommerce.web.service.WebOrderRecipientService;
import org.youngmonkeys.ecommerce.web.service.WebOrderService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.youngmonkeys.ecommerce.web.controller.api.WebApiProductController;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Controller;

/**
 * Activates the inherited routes of its base controller and adds the guest order status lookup
 * {@code GET /api/v1/ecm/order-status}. The lookup lives here, not in a class of its own, because
 * an extra class in the plugin jar changes how the platform resolves its own controllers on the
 * container.
 *
 * <p>It answers the number, status, dates, product lines (name, quantity, unit price) and total of
 * one order, given its number and delivery email. It never returns the recipient's name, address,
 * phone or email. Every case that does not match (unknown order, wrong email, draft or deleted
 * order) answers the same 404, so a caller cannot tell which order numbers exist. Calls are
 * limited per client address.
 */
@Api
@Controller("/api/v1")
public class EcmProductApiController extends WebApiProductController {

    private static final int MAX_CALLS = 10;
    private static final long WINDOW_MILLIS = 60_000L;

    private final EcmRateLimiter limiter = new EcmRateLimiter(MAX_CALLS, WINDOW_MILLIS);

    @EzyAutoBind
    private WebOrderService ecmOrderService;

    @EzyAutoBind
    private WebOrderRecipientService ecmOrderRecipientService;

    @EzyAutoBind
    private WebOrderModelDecorator ecmOrderDecorator;

    @DoGet("/ecm/order-status")
    public Map<String, Object> orderStatusGet(
        RequestArguments arguments,
        @RequestParam("orderId") String orderIdText,
        @RequestParam("email") String email
    ) {
        long retryAfter = limiter.tryAcquire(
            arguments.getRequest().getRemoteAddr(),
            System.currentTimeMillis()
        );
        if (retryAfter > 0) {
            arguments.getResponse().setHeader("Retry-After", String.valueOf(retryAfter));
            throw new HttpTooManyRequestsException(
                "too many lookups, retry after " + retryAfter + " seconds"
            );
        }
        OrderModel order = findMatchingOrder(orderIdText, email);
        if (order == null) {
            throw new HttpNotFoundException("order not found");
        }
        Map<String, Object> answer = new HashMap<>();
        answer.put("orderId", order.getId());
        answer.put("status", order.getStatus());
        answer.put("updatedAt", order.getUpdatedAt());
        addSummary(answer, order);
        return answer;
    }

    /** Adds the product lines and total; a failed lookup leaves the basic answer unchanged. */
    private void addSummary(Map<String, Object> answer, OrderModel order) {
        try {
            WebOrderDetailsResponse details = ecmOrderDecorator.decorate(order);
            List<Map<String, Object>> items = new ArrayList<>();
            if (details.getProducts() != null) {
                for (OrderProductResponse line : details.getProducts()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", line.getProduct() == null
                        ? "" : line.getProduct().getProductName());
                    item.put("quantity", line.getAmount());
                    item.put("unitPrice", line.getFormattedPriceIncludeSymbol());
                    item.put("lineTotal", line.getFormattedTotalAmountMoneyIncludeSymbol());
                    items.add(item);
                }
            }
            answer.put("items", items);
            answer.put("total", details.getFormattedTotalAmountMoneyIncludeSymbol());
            answer.put("createdAt", details.getCreatedAt());
        } catch (RuntimeException e) {
            answer.remove("items");
        }
    }

    private OrderModel findMatchingOrder(String orderIdText, String email) {
        long orderId;
        try {
            orderId = Long.parseLong(orderIdText == null ? "" : orderIdText.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        String wanted = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (orderId <= 0 || wanted.isEmpty()) {
            return null;
        }
        OrderModel order = ecmOrderService.getOrderById(orderId);
        if (order == null
            || "DRAFT".equals(order.getStatus())
            || "DELETED".equals(order.getStatus())) {
            return null;
        }
        OrderRecipientModel recipient = ecmOrderRecipientService.getOrderRecipientById(orderId);
        String actual = recipient == null || recipient.getEmail() == null
            ? "" : recipient.getEmail().trim().toLowerCase(Locale.ROOT);
        return !actual.isEmpty() && actual.equals(wanted) ? order : null;
    }
}
