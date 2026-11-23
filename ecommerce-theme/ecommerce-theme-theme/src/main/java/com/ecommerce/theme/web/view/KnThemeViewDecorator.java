package com.ecommerce.theme.web.view;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ezyplatform.web.view.WebViewDecorator;

@EzySingleton
public class KnThemeViewDecorator extends WebViewDecorator {

    @Override
    protected boolean includeWebLanguages() {
        return false;
    }
}
