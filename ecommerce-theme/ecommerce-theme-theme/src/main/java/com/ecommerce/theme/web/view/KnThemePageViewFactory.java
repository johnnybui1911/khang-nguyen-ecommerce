package com.ecommerce.theme.web.view;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.server.core.view.View;

import java.util.Collections;
import java.util.List;

import static org.youngmonkeys.ezyplatform.constant.CommonConstants.VIEW_VARIABLE_PAGE_TITLE;

@EzySingleton
public class KnThemePageViewFactory {

    public static final String VIEW_VARIABLE_CURRENT_PAGE = "currentPage";
    public static final String VIEW_VARIABLE_BODY_CLASS = "bodyClass";
    public static final String VIEW_VARIABLE_THEME_STYLES = "themeStyles";
    public static final String VIEW_VARIABLE_PAGE_PARAM = "pageParam";
    public static final String STYLE_MAIN = "/css/main.css";

    /**
     * Builds a storefront view. The page renders a template shell and the
     * page script fills it from the same-origin storefront API.
     */
    public View newView(
        String page,
        String template,
        String titleKey,
        String param
    ) {
        List<String> styles = Collections.singletonList(STYLE_MAIN);
        View.Builder builder = View.builder()
            .template(template)
            .addVariable(VIEW_VARIABLE_PAGE_TITLE, titleKey)
            .addVariable(VIEW_VARIABLE_CURRENT_PAGE, page)
            .addVariable(VIEW_VARIABLE_THEME_STYLES, styles);
        if (param != null) {
            builder.addVariable(VIEW_VARIABLE_PAGE_PARAM, param);
        }
        return builder.build();
    }

    public View newView(String page, String template, String titleKey) {
        return newView(page, template, titleKey, null);
    }
}
