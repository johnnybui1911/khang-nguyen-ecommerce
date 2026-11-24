package com.ecommerce.ext.web.controller.api;

import com.ecommerce.ext.web.payment.KnBankTxn;
import com.ecommerce.ext.web.payment.KnBankTxnProvider;
import com.ecommerce.ext.web.payment.KnBankTxnProviders;
import com.ecommerce.ext.web.payment.KnPaymentConfirmService;
import com.ecommerce.ext.web.payment.KnWebhookRejectedException;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.core.exception.HttpUnauthorizedException;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoPost;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Entry point for bank-transaction webhooks, one path per provider. Authentication is the
 * provider's own (API key or signature); an unconfigured provider answers 401 to everything.
 */
@Api
@Controller("/api/v1/ecm/payment")
public class KnPaymentWebhookApiController {

    private static final int MAX_BODY_CHARS = 64 * 1024;

    @EzyAutoBind
    private KnBankTxnProviders providers;

    @EzyAutoBind
    private KnPaymentConfirmService confirmService;

    @DoPost("/webhook/{provider}")
    public Map<String, Object> webhookPost(
        @PathVariable("provider") String providerName,
        RequestArguments arguments
    ) throws IOException {
        KnBankTxnProvider provider = providers.find(providerName)
            .orElseThrow(() -> new HttpNotFoundException("unknown provider"));
        if (!provider.isEnabled()) {
            throw new HttpUnauthorizedException("provider disabled");
        }
        HttpServletRequest request = arguments.getRequest();
        try {
            for (KnBankTxn txn : provider.parseWebhook(
                headersOf(request),
                readBody(request)
            )) {
                confirmService.handle(txn);
            }
        } catch (KnWebhookRejectedException e) {
            if (e.isUnauthorized()) {
                throw new HttpUnauthorizedException(e.getMessage());
            }
            throw new HttpBadRequestException(e.getMessage());
        }
        Map<String, Object> ok = new HashMap<>();
        ok.put("success", true);
        ok.put("error", 0);
        return ok;
    }

    private static Map<String, String> headersOf(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name.toLowerCase(), request.getHeader(name));
        }
        return headers;
    }

    private static String readBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) > 0) {
                body.append(buffer, 0, read);
                if (body.length() > MAX_BODY_CHARS) {
                    throw new HttpBadRequestException("body too large");
                }
            }
        }
        return body.toString();
    }
}
