package com.ecommerce.theme.web.controller.view;

import com.ecommerce.theme.web.view.KnThemeContentHelper;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.view.View;
import lombok.Setter;
import org.youngmonkeys.ezyarticle.web.controller.view.PageController;
import org.youngmonkeys.ezyarticle.web.response.WebPostDetailsResponse;

import javax.servlet.http.HttpServletRequest;

@Setter
@Controller
public class KnThemePageController extends PageController {

    @EzyAutoBind
    private KnThemeContentHelper contentHelper;

    @Override
    protected void decoratePageDetailsView(HttpServletRequest request, View view) {
        WebPostDetailsResponse page = view.getVariable("page");
        if (page == null) {
            return;
        }
        contentHelper.decorate(
            request,
            view,
            page.getTitle(),
            contentHelper.excerpt(page.getSummary(), page.getContent()),
            page.getFeaturedImage()
        );
    }
}
