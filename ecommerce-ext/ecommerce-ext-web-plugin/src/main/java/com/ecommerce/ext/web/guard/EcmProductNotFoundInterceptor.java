package com.ecommerce.ext.web.guard;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.annotation.Interceptor;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.server.core.interceptor.RequestInterceptor;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import org.youngmonkeys.ecommerce.web.controller.service.WebProductControllerService;
import org.youngmonkeys.ecommerce.web.service.WebEcommerceSettingService;
import org.youngmonkeys.ecommerce.web.service.WebProductCurrencyService;

import java.lang.reflect.Method;

/**
 * The platform product page answers 200 with a "Not found" body for an unknown product code. This
 * checks the code first, with the same lookup as the storefront product page, so an unknown or
 * hidden product is a real 404.
 */
@Interceptor(priority = 3)
public class EcmProductNotFoundInterceptor implements RequestInterceptor {

    private static final String OWNER = "WebProductController";
    private static final String HANDLER = "productCodeGet";
    private static final String PREFIX = "/ecommerce/products/";

    @EzyAutoBind
    private WebProductControllerService productControllerService;

    @EzyAutoBind
    private WebEcommerceSettingService ecommerceSettingService;

    @EzyAutoBind
    private WebProductCurrencyService currencyService;

    @Override
    public boolean preHandle(RequestArguments arguments, Method handler) {
        if (!OWNER.equals(handler.getDeclaringClass().getSimpleName())
            || !HANDLER.equals(handler.getName())) {
            return true;
        }
        String uri = arguments.getRequest().getRequestURI();
        int at = uri.indexOf(PREFIX);
        String code = at < 0 ? "" : uri.substring(at + PREFIX.length());
        if (code.isEmpty() || !exists(code)) {
            throw new HttpNotFoundException("product not found");
        }
        return true;
    }

    @Override
    public void postHandle(RequestArguments arguments, Method handler) {}

    private boolean exists(String code) {
        try {
            return productControllerService.getProductDetailsByCodeAndStatuses(
                code,
                ecommerceSettingService.getDisplayableProductStatuses(),
                "en",
                currencyService.getCurrencyByIdOrDefault(0L)
            ) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
