package com.ecommerce.ext.web.guard;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.io.EzyStrings;
import com.tvd12.ezyhttp.core.annotation.Interceptor;
import com.tvd12.ezyhttp.core.constant.HttpMethod;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.server.core.interceptor.RequestInterceptor;
import com.tvd12.ezyhttp.server.core.manager.RequestURIManager;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.youngmonkeys.ezyplatform.exception.UserInvalidAccessTokenException;
import org.youngmonkeys.ezyplatform.util.AccessTokens;
import org.youngmonkeys.ezyplatform.web.annotation.UserId;
import org.youngmonkeys.ezyplatform.web.service.WebUserService;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Guards the routes activated by the compatibility controllers.
 *
 * <p>A disabled {@code ecm.compat.<name>.enabled} flag answers 404 as if the route were absent.
 * A state-changing request needs an {@code Origin} matching this site (or listed in
 * {@code ecm.allowed.origins}) plus the header {@code X-Requested-With: ecm}, otherwise 403.
 * The loopback names 127.0.0.1 and localhost count as one site on the same port.
 * Payment-gateway callbacks are not guarded because they come from servers, not browsers.
 */
@Interceptor(priority = 1)
public class EcmCompatGuardInterceptor implements RequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EcmCompatGuardInterceptor.class);
    private static final String ACCOUNT_OWNER = "AccountController";
    private static final String REQUIRED_HEADER_VALUE = "ecm";
    private static final Pattern LIST_SPLIT = Pattern.compile("\\s*,\\s*");

    /** Maps the original controller simple name to its flag name. */
    private static final Map<String, String> FLAGS = new HashMap<>();
    /** Original controllers whose state-changing routes are browser-facing. */
    private static final Set<String> GUARDED_CLASSES;

    static {
        FLAGS.put("WebApiShoppingCartController", "cart");
        FLAGS.put("WebApiCheckoutController", "checkout");
        FLAGS.put("WebApiProductCheckoutController", "checkout");
        FLAGS.put("WebCheckoutController", "checkout");
        FLAGS.put("WebProductCheckoutController", "checkout");
        FLAGS.put("WebApiOrderController", "order");
        FLAGS.put("WebApiProductOrderController", "order");
        FLAGS.put("WebOrderController", "order");
        FLAGS.put("WebPaymentCallbackController", "payment");
        FLAGS.put("WebApiProductController", "catalog");
        FLAGS.put("WebApiCategoryController", "catalog");
        FLAGS.put("WebApiCollectionController", "catalog");
        FLAGS.put("WebApiProductPriceController", "catalog");
        FLAGS.put("WebApiProductCurrencyController", "catalog");
        FLAGS.put("WebApiCountryController", "catalog");
        FLAGS.put("AccountController", "account");
        FLAGS.put("WebApiAccountController", "account");
        FLAGS.put("EcmWishlistApiController", "wishlist");
        FLAGS.put("EcmNewsletterApiController", "newsletter");
        GUARDED_CLASSES = new HashSet<>(Arrays.asList(
            "WebApiShoppingCartController",
            "WebApiCheckoutController",
            "WebApiProductCheckoutController",
            "WebApiOrderController",
            "WebApiProductOrderController",
            "WebApiProductController",
            "WebApiCategoryController",
            "WebApiCollectionController",
            "WebApiProductPriceController",
            "WebApiProductCurrencyController",
            "WebApiCountryController",
            "WebApiAccountController",
            "EcmWishlistApiController",
            "EcmNewsletterApiController"
        ));
    }

    @EzyAutoBind
    private WebUserService userService;

    @EzyAutoBind
    private RequestURIManager requestUriManager;

    @Override
    public boolean preHandle(RequestArguments arguments, Method handler) {
        String owner = handler.getDeclaringClass().getSimpleName();
        String flag = FLAGS.get(owner);
        if (flag == null) {
            return true;
        }
        if (!EcmCompatFlags.isEnabled(flag)) {
            throw new HttpNotFoundException("ecm compat disabled: " + flag);
        }
        if (ACCOUNT_OWNER.equals(owner)) {
            requireSignedIn(arguments);
        }
        HttpMethod method = arguments.getMethod();
        boolean changesState = method != HttpMethod.GET && method != HttpMethod.HEAD;
        if (changesState && GUARDED_CLASSES.contains(owner)) {
            return isTrustedBrowserRequest(arguments.getRequest());
        }
        return true;
    }

    @Override
    public void postHandle(RequestArguments arguments, Method handler) {}

    /**
     * The inherited account pages read the user id as a primitive, so a request that reaches them
     * without one fails with a NullPointerException (HTTP 500). The platform interceptor normally
     * rejects a visitor without a valid access token first; this makes the page enforce the same
     * rule itself: no token or an unknown token raises the exception that the global handler turns
     * into the redirect to the login page, and a valid token supplies the user id.
     */
    private void requireSignedIn(RequestArguments arguments) {
        Long userId = (Long) arguments.getArgument(UserId.class);
        if (userId != null && userId != 0L) {
            return;
        }
        String accessToken = arguments.getRequestValueAnyway("accessToken");
        if (EzyStrings.isBlank(accessToken)) {
            accessToken = AccessTokens.extractBearerToken(arguments.getHeader("Authorization"));
        }
        if (EzyStrings.isBlank(accessToken)) {
            String template = arguments.getUriTemplate();
            LOGGER.info(
                "account route without a user id and token: uriTemplate={}, authenticatedUri={}",
                template,
                requestUriManager.isAuthenticatedURI(arguments.getMethod(), template)
            );
            throw new UserInvalidAccessTokenException(accessToken);
        }
        Long validated = userService.validateUserAccessToken(accessToken);
        arguments.setArgument(UserId.class, validated);
        arguments.getRequest().setAttribute(UserId.class.getName(), validated);
    }

    private static boolean isTrustedBrowserRequest(HttpServletRequest request) {
        if (!REQUIRED_HEADER_VALUE.equals(request.getHeader("X-Requested-With"))) {
            throw new com.tvd12.ezyhttp.core.exception.HttpForbiddenException(
                "missing X-Requested-With header"
            );
        }
        String origin = request.getHeader("Origin");
        if (origin == null || !isAllowedOrigin(origin, request)) {
            throw new com.tvd12.ezyhttp.core.exception.HttpForbiddenException(
                "origin not allowed"
            );
        }
        return true;
    }

    private static boolean isAllowedOrigin(String origin, HttpServletRequest request) {
        String allowed = System.getProperty(
            "ecm.allowed.origins",
            System.getenv("ECM_ALLOWED_ORIGINS")
        );
        if (allowed != null) {
            for (String item : LIST_SPLIT.split(allowed.trim())) {
                if (!item.isEmpty() && item.equalsIgnoreCase(origin)) {
                    return true;
                }
            }
        }
        try {
            URI uri = new URI(origin);
            String host = request.getHeader("Host");
            if (host == null || uri.getHost() == null) {
                return false;
            }
            String authority = uri.getPort() < 0
                ? uri.getHost()
                : uri.getHost() + ":" + uri.getPort();
            boolean schemeOk = uri.getScheme() != null
                && (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
            return schemeOk
                && (authority.equalsIgnoreCase(host) || sameLoopbackSite(authority, host));
        } catch (Exception e) {
            return false;
        }
    }

    /** 127.0.0.1:P and localhost:P are the same local site; a browser may use either name. */
    private static boolean sameLoopbackSite(String authority, String host) {
        String port = loopbackPort(authority);
        return port != null && port.equals(loopbackPort(host));
    }

    private static String loopbackPort(String authority) {
        String a = authority.toLowerCase(java.util.Locale.ROOT);
        for (String name : new String[] {"127.0.0.1", "localhost"}) {
            if (a.equals(name)) {
                return "";
            }
            if (a.startsWith(name + ":")) {
                return a.substring(name.length() + 1);
            }
        }
        return null;
    }
}
