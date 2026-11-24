package com.ecommerce.ext.web.controller;

import com.tvd12.ezyhttp.core.constant.HttpMethod;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.annotation.ExceptionHandler;
import com.tvd12.ezyhttp.server.core.handler.UnhandledErrorHandler;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import com.tvd12.ezyhttp.server.core.view.View;
import org.youngmonkeys.ezyplatform.web.controller.WebGlobalExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * Maps token and routing errors to 401 JSON for APIs and a login redirect for pages.
 * A not-found page is also served with a real 404 status.
 *
 * <p>It is also the server's unhandled-error handler: a request that matches no route at all
 * (for example {@code /abc-xyz}) gets the same storefront 404 page for a browser page request,
 * or a JSON error under {@code /api/}. Every other status keeps the server default.
 */
@ExceptionHandler
public class KnGlobalExceptionHandler
    extends WebGlobalExceptionHandler
    implements UnhandledErrorHandler {

    private static final String API_PREFIX = "/api/";

    @Override
    public Object processError(
        HttpMethod method,
        HttpServletRequest request,
        HttpServletResponse response,
        int errorStatusCode,
        Exception exception
    ) {
        if (errorStatusCode != 404) {
            return null;
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith(API_PREFIX)) {
            response.setStatus(404);
            return ResponseEntity.status(404)
                .body(Collections.singletonMap("error", "not found"))
                .build();
        }
        if (method != HttpMethod.GET || isAsset(uri)) {
            return null;
        }
        response.setStatus(404);
        return newNotFoundViewBuilder().build();
    }

    /** A missing file such as /js/x.js or /images/y.png keeps the plain default answer. */
    private static boolean isAsset(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.substring(uri.lastIndexOf('/') + 1).indexOf('.') >= 0;
    }

    @Override
    public Object handle(RequestArguments arguments, HttpNotFoundException e) {
        arguments.getResponse().setStatus(404);
        return super.handle(arguments, e);
    }

    /** The not-found page uses the storefront frame (header, footer, styles). */
    @Override
    protected String getNotFoundViewTemplate() {
        return "notfound";
    }

    @Override
    protected View.Builder newNotFoundViewBuilder() {
        return super.newNotFoundViewBuilder()
            .addVariable("pageTitle", "page.notfound")
            .addVariable("currentPage", "notfound")
            .addVariable("themeStyles", Collections.singletonList("/css/main.css"));
    }
}
