package com.ecommerce.theme.web.controller.view;

import com.ecommerce.theme.web.view.KnThemeContentHelper;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.view.View;
import lombok.Setter;
import org.youngmonkeys.ezyarticle.web.controller.view.BlogController;
import org.youngmonkeys.ezyarticle.web.response.WebPostDetailsResponse;

import javax.servlet.http.HttpServletRequest;

import static com.ecommerce.theme.web.view.KnThemeContentHelper.SITE_NAME;

@Setter
@Controller
public class KnThemeBlogController extends BlogController {

    @EzyAutoBind
    private KnThemeContentHelper contentHelper;

    @Override
    protected void decorateBlogView(HttpServletRequest request, View view) {
        contentHelper.decorate(
            request,
            view,
            "Cẩm nang",
            "Cẩm nang về ngọc phong thủy: cách chọn, cách đeo và cách giữ gìn từ "
                + SITE_NAME + ".",
            null
        );
    }

    @Override
    protected void decorateBlogDetailsView(HttpServletRequest request, View view) {
        WebPostDetailsResponse blog = view.getVariable("blog");
        if (blog == null) {
            return;
        }
        contentHelper.decorate(
            request,
            view,
            blog.getTitle(),
            contentHelper.excerpt(blog.getSummary(), blog.getContent()),
            blog.getFeaturedImage()
        );
    }
}
