package com.ecommerce.ext.admin.view;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.server.core.view.View;
import com.tvd12.ezyhttp.server.core.view.ViewDecorator;

import javax.servlet.http.HttpServletRequest;

/**
 * Adds the Khang Nguyen brand fragment (sidebar logo and name) to every admin page.
 */
@EzySingleton
public class KnAdminViewDecorator implements ViewDecorator {

    private static final String BRAND_FRAGMENT = "knadmin/fragments/brand";

    @Override
    public void decorate(HttpServletRequest request, View view) {
        view.appendValueToVariable("additionalFragments", BRAND_FRAGMENT);
    }
}
