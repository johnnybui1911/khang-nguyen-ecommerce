package com.ecommerce.ext.web.controller.api;

import com.ecommerce.ext.payment.KnBankTransferPaymentService;
import com.ecommerce.ext.payment.KnCodPaymentService;
import com.ecommerce.ext.web.payment.KnBankTransferInfoService;
import com.ecommerce.ext.web.payment.KnOrderViewKeyService;
import com.ecommerce.ext.web.payment.KnPaymentMailer;
import com.ecommerce.ext.web.payment.KnShopConfirmService;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.youngmonkeys.ecommerce.entity.OrderRecipientType;
import org.youngmonkeys.ecommerce.entity.OrderStatus;
import org.youngmonkeys.ecommerce.entity.ShoppingCartStatus;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.request.SaveOrderRequest;
import org.youngmonkeys.ecommerce.web.controller.api.WebApiOrderController;
import org.youngmonkeys.ecommerce.web.repo.WebShoppingCartRepository;
import org.youngmonkeys.ezyplatform.web.annotation.UserId;

/**
 * Activates the inherited order routes, mails the customer once an offline-paid order is
 * placed, and exposes the transfer instructions of an order to its owner.
 */
@Api
@Authenticated
@Controller("/api/v1")
public class KnOrderApiController extends WebApiOrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnOrderApiController.class);
    /** Request attribute naming the order this request created, rather than reused. */
    private static final String NEW_ORDER_ID =
        KnOrderApiController.class.getName() + ".newOrderId";

    @EzyAutoBind
    private KnPaymentMailer paymentMailer;

    @EzyAutoBind
    private KnBankTransferInfoService bankInfoService;

    @EzyAutoBind
    private KnShopConfirmService shopConfirmService;

    @EzyAutoBind
    private KnOrderViewKeyService viewKeyService;

    @EzyAutoBind
    private WebShoppingCartRepository shoppingCartRepository;

    @Override
    protected OrderModel createOrderIfNeed(
        RequestArguments arguments,
        long userId,
        String paymentServiceCode,
        OrderStatus orderStatus,
        OrderRecipientType orderRecipientType,
        ShoppingCartStatus shoppingCartStatus,
        SaveOrderRequest requestBody
    ) {
        boolean existed = orderService.getWaitingForPaymentOrderByShoppingCartId(
            requestBody.getFromShoppingCartId()) != null;
        OrderModel order = super.createOrderIfNeed(arguments, userId, paymentServiceCode,
            orderStatus, orderRecipientType, shoppingCartStatus, requestBody);
        if (!existed) {
            arguments.getRequest().setAttribute(NEW_ORDER_ID, order.getId());
        }
        return order;
    }

    @Override
    protected ResponseEntity createOrderPaymentUrl(
        RequestArguments arguments,
        OrderModel order,
        String paymentServiceCode,
        String message
    ) throws Exception {
        boolean offline = KnBankTransferPaymentService.CODE.equals(paymentServiceCode)
            || KnCodPaymentService.CODE.equals(paymentServiceCode);
        if (!offline) {
            return super.createOrderPaymentUrl(arguments, order, paymentServiceCode, message);
        }
        ResponseEntity response;
        try {
            response = super.createOrderPaymentUrl(arguments, order, paymentServiceCode, message);
        } catch (Exception e) {
            dropUnplacedOrder(arguments, order);
            throw e;
        }
        closeCart(order);
        paymentMailer.sendOrderReceived(order);
        if (KnBankTransferPaymentService.CODE.equals(paymentServiceCode)) {
            mailShop(order);
        }
        return withGuestKey(response, order);
    }

    /**
     * A placed offline order leaves its cart. The platform keeps the cart of an unpaid order
     * open and reuses that order for the next checkout from the cart, so without this a second
     * purchase would land on the first order. PAYING is what the platform itself sets once an
     * order waits for the shop; paying or rejecting the order still moves the cart on.
     */
    private void closeCart(OrderModel order) {
        long cartId = order.getFromShoppingCartId();
        if (cartId > 0L) {
            shoppingCartRepository.updateShoppingCartStatus(
                cartId, ShoppingCartStatus.PAYING.toString());
        }
    }

    /**
     * An order this very request created and could not place (for one, cash on delivery above
     * the limit) is invalidated, so retrying with another payment method makes a fresh order. A
     * reused order is left as it is.
     */
    private void dropUnplacedOrder(RequestArguments arguments, OrderModel order) {
        Object created = arguments.getRequest().getAttribute(NEW_ORDER_ID);
        if (created instanceof Long && (Long) created == order.getId()) {
            orderService.invalidateWaitingForPaymentOrderByShoppingCartIdIfExists(
                0L, order.getCreatorUserId(), order.getFromShoppingCartId());
        }
    }

    /**
     * A guest is signed in only in the browser that checked out, so its order page link carries
     * the order's view key; the page then still opens after the session is gone.
     */
    private ResponseEntity withGuestKey(ResponseEntity response, OrderModel order) {
        Object body = response.getBody();
        String key = body instanceof String && viewKeyService.isGuest(order.getCreatorUserId())
            ? viewKeyService.keyOf(order.getId()) : null;
        if (key == null) {
            return response;
        }
        String url = (String) body;
        return ResponseEntity.builder()
            .contentType("text/plain")
            .body(url + (url.contains("?") ? "&" : "?") + "key=" + key)
            .build();
    }

    private void mailShop(OrderModel order) {
        try {
            String token = shopConfirmService.issueToken(order.getId());
            if (token != null) {
                paymentMailer.sendShopConfirmRequest(order, shopConfirmService.confirmUrl(token));
            }
        } catch (Exception e) {
            LOGGER.warn("can not mail the shop about order {}", order.getId(), e);
        }
    }

    @DoGet("/orders/{id}/bank-transfer")
    public Map<String, Object> ordersIdBankTransferGet(
        @UserId long userId,
        @PathVariable("id") long orderId
    ) {
        return bankInfoService.describe(orderValidator.validateUserOrder(userId, orderId));
    }
}
