package com.ecommerce.ext.web.controller.view;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.view.View;
import org.youngmonkeys.ecommerce.web.controller.service.WebProductControllerService;
import org.youngmonkeys.ecommerce.web.service.WebEcommerceSettingService;
import org.youngmonkeys.ecommerce.web.service.WebProductCurrencyService;

import java.util.Collections;

/**
 * Storefront product page. The page is a template shell filled by script, so the product is looked
 * up here and an unknown code answers a real 404 instead of a 200 shell.
 */
@Controller
public class KnProductPageViewController {

    @EzyAutoBind
    private WebProductControllerService productControllerService;

    @EzyAutoBind
    private WebEcommerceSettingService ecommerceSettingService;

    @EzyAutoBind
    private WebProductCurrencyService currencyService;

    @DoGet("/products/{code}")
    public View product(@PathVariable String code) {
        Object found;
        try {
            found = productControllerService.getProductDetailsByCodeAndStatuses(
                code,
                ecommerceSettingService.getDisplayableProductStatuses(),
                "en",
                currencyService.getCurrencyByIdOrDefault(0L)
            );
        } catch (Exception e) {
            found = null;
        }
        if (found == null) {
            throw new HttpNotFoundException("product not found");
        }
        return View.builder()
            .template("product")
            .addVariable("pageTitle", "page.product")
            .addVariable("currentPage", "product")
            .addVariable("themeStyles", Collections.singletonList("/css/main.css"))
            .addVariable("pageParam", code)
            .build();
    }
}
