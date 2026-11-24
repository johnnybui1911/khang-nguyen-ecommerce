package com.ecommerce.ext.web.controller.api;

import com.ecommerce.ext.payment.KnBankTransferPaymentService;
import com.ecommerce.ext.payment.KnCodPaymentService;
import com.ecommerce.ext.web.payment.KnBankTransferInfo;
import com.ecommerce.ext.web.payment.KnBankTransferInfoService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.youngmonkeys.ecommerce.model.OrderModel;
import org.youngmonkeys.ecommerce.web.controller.api.WebApiOrderController;
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

    @EzyAutoBind
    private KnPaymentMailer paymentMailer;

    @EzyAutoBind
    private KnBankTransferInfoService bankInfoService;

    @EzyAutoBind
    private KnShopConfirmService shopConfirmService;

    @Override
    protected ResponseEntity createOrderPaymentUrl(
        RequestArguments arguments,
        OrderModel order,
        String paymentServiceCode,
        String message
    ) throws Exception {
        ResponseEntity response =
            super.createOrderPaymentUrl(arguments, order, paymentServiceCode, message);
        if (KnBankTransferPaymentService.CODE.equals(paymentServiceCode)
            || KnCodPaymentService.CODE.equals(paymentServiceCode)) {
            paymentMailer.sendOrderReceived(order);
            if (KnBankTransferPaymentService.CODE.equals(paymentServiceCode)) {
                mailShop(order);
            }
        }
        return response;
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
        OrderModel order = orderValidator.validateUserOrder(userId, orderId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (!KnBankTransferPaymentService.CODE.equals(order.getPaymentServiceCode())) {
            result.put("available", false);
            return result;
        }
        KnBankTransferInfo info = bankInfoService.build(order);
        result.put("available", true);
        result.put("configured", info.isConfigured());
        result.put("amount", info.getAmount().toBigInteger());
        result.put("content", info.getContent());
        if (info.isConfigured()) {
            result.put("bankName", info.getBankName());
            result.put("accountNumber", info.getAccountNumber());
            result.put("accountOwner", info.getAccountOwner());
            result.put("qrUrl", info.getQrUrl());
        }
        return result;
    }
}
