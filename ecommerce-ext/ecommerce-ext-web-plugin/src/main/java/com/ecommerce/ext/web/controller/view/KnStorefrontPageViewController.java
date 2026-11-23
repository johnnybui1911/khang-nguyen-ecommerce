package com.ecommerce.ext.web.controller.view;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.view.View;
import org.youngmonkeys.ecommerce.model.ProductCategoryModel;
import org.youngmonkeys.ecommerce.web.service.WebProductCategoryService;

import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Category and order pages. They are template shells filled by script, so the path is checked
 * here and an unknown category or a malformed order number answers a real 404 instead of a 200
 * shell that says "not found".
 */
@Controller
public class KnStorefrontPageViewController {

    private static final Pattern ORDER_ID = Pattern.compile("^[0-9]{1,18}$");

    @EzyAutoBind
    private WebProductCategoryService categoryService;

    @DoGet("/categories/{slug}")
    public View category(@PathVariable String slug) {
        ProductCategoryModel found;
        try {
            found = categoryService.getCategoryByName(slug);
        } catch (Exception e) {
            found = null;
        }
        if (found == null) {
            throw new HttpNotFoundException("category not found");
        }
        return shell("products", "products", "page.category", slug);
    }

    @DoGet("/orders/{id}")
    public View order(@PathVariable String id) {
        if (!ORDER_ID.matcher(id).matches()) {
            throw new HttpNotFoundException("order not found");
        }
        return shell("order", "order", "page.order", id);
    }

    private static View shell(String template, String page, String titleKey, String param) {
        return View.builder()
            .template(template)
            .addVariable("pageTitle", titleKey)
            .addVariable("currentPage", page)
            .addVariable("themeStyles", Collections.singletonList("/css/main.css"))
            .addVariable("pageParam", param)
            .build();
    }
}
