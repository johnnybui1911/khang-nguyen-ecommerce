package com.ecommerce.ext.web.guard;

import com.ecommerce.ext.web.payment.KnOrderViewKeyService;
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
 *
 * <p>The anonymous user of a guest checkout may not change an account: a state-changing call to
 * the ezyaccount API, the platform account API (profile, password, avatar) or the two-factor
 * settings answers 403 for it, so it can not set an email or password and pass for a member.
 * Order pages, order APIs and the shop's money-received page answer with
 * {@code Referrer-Policy: no-referrer} and {@code Cache-Control: no-store}, so the secret
 * {@code ?key=} of a guest order link and the {@code ?token=} of a shop-confirm link are neither
 * sent to other sites nor kept in a cache.
 */
@Interceptor(priority = 1)
public class KnCompatGuardInterceptor implements RequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnCompatGuardInterceptor.class);
    private static final String ACCOUNT_OWNER = "AccountController";
    /** The theme also serves the /account page shell; the platform lets any session reach it. */
    private static final String THEME_ACCOUNT_OWNER = "KnThemeHomeController";
    private static final String REQUIRED_HEADER_VALUE = "ecm";
    private static final Pattern LIST_SPLIT = Pattern.compile("\\s*,\\s*");
    private static final Pattern ORDER_PATH =
        Pattern.compile("^(/orders/|/api/v1/ecm/orders/|/api/v1/orders/|/payment/shop-confirm).*");
    private static final String EZYACCOUNT_API = "org.youngmonkeys.ezyaccount.web.controller.api.";
    private static final String PLATFORM_ACCOUNT_API =
        "org.youngmonkeys.ezyplatform.web.controller.api.WebApiAccountController";
    private static final String TWO_FACTOR_API =
        "org.youngmonkeys.ezylogin.web.controller.api.WebApiTwoFactorController";

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
        FLAGS.put("KnWishlistApiController", "wishlist");
        FLAGS.put("KnNewsletterApiController", "newsletter");
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
            "KnWishlistApiController",
            "KnNewsletterApiController"
        ));
    }

    @EzyAutoBind
    private KnOrderViewKeyService viewKeyService;

    @EzyAutoBind
    private WebUserService userService;

    @EzyAutoBind
    private RequestURIManager requestUriManager;

    @Override
    public boolean preHandle(RequestArguments arguments, Method handler) {
        String owner = handler.getDeclaringClass().getSimpleName();
        protectOrderPage(arguments);
        HttpMethod method = arguments.getMethod();
        boolean changesState = method != HttpMethod.GET && method != HttpMethod.HEAD;
        if (changesState && isAccountWrite(handler)) {
            forbidGuest(arguments);
        }
        if (THEME_ACCOUNT_OWNER.equals(owner) && "account".equals(handler.getName())) {
            rejectGuestToken(arguments);
            return true;
        }
        String flag = FLAGS.get(owner);
        if (flag == null) {
            return true;
        }
        if (!KnCompatFlags.isEnabled(flag)) {
            throw new HttpNotFoundException("ecm compat disabled: " + flag);
        }
        if (ACCOUNT_OWNER.equals(owner)) {
            requireSignedIn(arguments);
        }
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
     * into the redirect to the login page, and a valid token supplies the user id. The anonymous
     * user a guest checkout gets is not an account either, so it is sent to the login page too.
     */
    private void requireSignedIn(RequestArguments arguments) {
        Long userId = (Long) arguments.getArgument(UserId.class);
        if (userId != null && userId != 0L) {
            rejectGuest(userId);
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
        rejectGuest(validated);
        arguments.setArgument(UserId.class, validated);
        arguments.getRequest().setAttribute(UserId.class.getName(), validated);
    }

    /** A guest checkout session opening /account goes to the login page, as without a session. */
    private void rejectGuestToken(RequestArguments arguments) {
        Long userId = (Long) arguments.getArgument(UserId.class);
        if (userId == null || userId == 0L) {
            String accessToken = arguments.getRequestValueAnyway("accessToken");
            if (EzyStrings.isBlank(accessToken)) {
                accessToken = AccessTokens.extractBearerToken(
                    arguments.getHeader("Authorization")
                );
            }
            if (EzyStrings.isBlank(accessToken)) {
                return;
            }
            userId = userService.validateUserAccessToken(accessToken);
        }
        rejectGuest(userId);
    }

    /** True for the routes that let a signed-in user change its own account or login. */
    static boolean isAccountWrite(Method handler) {
        String type = handler.getDeclaringClass().getName();
        return type.startsWith(EZYACCOUNT_API)
            || PLATFORM_ACCOUNT_API.equals(type)
            || (TWO_FACTOR_API.equals(type) && handler.getName().startsWith("usersMe2Fa"));
    }

    /**
     * 403 for the anonymous user of a guest checkout. A request without a session or with an
     * unknown token goes on, and the platform answers it as it always does (401).
     */
    private void forbidGuest(RequestArguments arguments) {
        Long userId = (Long) arguments.getArgument(UserId.class);
        if (userId == null || userId == 0L) {
            String accessToken = arguments.getRequestValueAnyway("accessToken");
            if (EzyStrings.isBlank(accessToken)) {
                accessToken = AccessTokens.extractBearerToken(
                    arguments.getHeader("Authorization")
                );
            }
            if (EzyStrings.isBlank(accessToken)) {
                return;
            }
            try {
                userId = userService.validateUserAccessToken(accessToken);
            } catch (Exception e) {
                return;
            }
        }
        if (userId != null && userId > 0L && viewKeyService.isGuest(userId)) {
            throw new com.tvd12.ezyhttp.core.exception.HttpForbiddenException(
                "a guest checkout session can not change an account"
            );
        }
    }

    private static void protectOrderPage(RequestArguments arguments) {
        String uri = arguments.getRequest().getRequestURI();
        if (uri != null && ORDER_PATH.matcher(uri).matches()) {
            arguments.getResponse().setHeader("Referrer-Policy", "no-referrer");
            arguments.getResponse().setHeader("Cache-Control", "no-store");
        }
    }

    private void rejectGuest(Long userId) {
        if (userId != null && viewKeyService.isGuest(userId)) {
            throw new UserInvalidAccessTokenException("");
        }
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
