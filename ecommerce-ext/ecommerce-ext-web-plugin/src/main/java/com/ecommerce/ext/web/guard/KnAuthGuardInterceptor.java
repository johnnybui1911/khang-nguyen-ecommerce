package com.ecommerce.ext.web.guard;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.annotation.Interceptor;
import com.tvd12.ezyhttp.core.exception.HttpTooManyRequestsException;
import com.tvd12.ezyhttp.server.core.interceptor.RequestInterceptor;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import com.tvd12.ezyhttp.server.core.request.SimpleRequestArguments;
import org.youngmonkeys.ezylogin.web.service.WebEzyLoginUserAccessTokenService;
import org.youngmonkeys.ezyplatform.model.UserAccessTokenModel;
import org.youngmonkeys.ezyplatform.web.service.WebUserService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;

/**
 * Hardens the ezylogin authentication endpoints:
 * <ul>
 *   <li>login, register, forgot-password and reset-password attempts are rate limited per
 *   client address (429 with Retry-After);</li>
 *   <li>the platform's own logout also deletes the access token, so a copied token
 *   stops working.</li>
 *   <li>the accessToken cookie is marked HttpOnly and SameSite=Lax.</li>
 * </ul>
 * Limits come from the system properties {@code ecm.ratelimit.login.max},
 * {@code ecm.ratelimit.register.max}, {@code ecm.ratelimit.password-reset.max} and
 * {@code ecm.ratelimit.window.seconds}.
 */
@Interceptor(priority = 2)
public class KnAuthGuardInterceptor implements RequestInterceptor {

    private static final String AUTH_API = "WebApiAuthenticationController";
    private static final String AUTH_VIEW = "WebAuthenticationController";
    private static final String SAME_SITE_LAX_COMMENT = "__SAME_SITE_LAX__";

    private final KnRateLimiter loginLimiter = new KnRateLimiter(
        intProperty("ecm.ratelimit.login.max", 5),
        intProperty("ecm.ratelimit.window.seconds", 60) * 1000L
    );
    private final KnRateLimiter registerLimiter = new KnRateLimiter(
        intProperty("ecm.ratelimit.register.max", 10),
        intProperty("ecm.ratelimit.window.seconds", 60) * 1000L
    );

    private final KnRateLimiter passwordResetLimiter = new KnRateLimiter(
        intProperty("ecm.ratelimit.password-reset.max", 5),
        intProperty("ecm.ratelimit.window.seconds", 60) * 1000L
    );

    @EzyAutoBind
    private WebUserService userService;

    @EzyAutoBind
    private WebEzyLoginUserAccessTokenService loginAccessTokenService;

    @Override
    public boolean preHandle(RequestArguments arguments, Method handler) {
        String owner = handler.getDeclaringClass().getSimpleName();
        String name = handler.getName();
        if ((AUTH_API.equals(owner) || AUTH_VIEW.equals(owner))
            && arguments instanceof SimpleRequestArguments) {
            ((SimpleRequestArguments) arguments).setResponse(
                hardenResponse(arguments.getResponse())
            );
        }
        if (AUTH_API.equals(owner)) {
            KnRateLimiter limiter = "loginPost".equals(name) ? loginLimiter
                : ("registerPost".equals(name) || "openRegisterPost".equals(name))
                ? registerLimiter
                : ("forgotPasswordPost".equals(name) || "resetPasswordPost".equals(name))
                ? passwordResetLimiter : null;
            if (limiter != null) {
                return allow(limiter, name, arguments);
            }
        } else if (AUTH_VIEW.equals(owner) && "logoutPost".equals(name)) {
            revokeCurrentToken(arguments.getRequest());
        }
        return true;
    }

    @Override
    public void postHandle(RequestArguments arguments, Method handler) {}

    private boolean allow(KnRateLimiter limiter, String name, RequestArguments arguments) {
        HttpServletRequest request = arguments.getRequest();
        long retryAfter = limiter.tryAcquire(
            name + "|" + request.getRemoteAddr(),
            System.currentTimeMillis()
        );
        if (retryAfter == 0) {
            return true;
        }
        arguments.getResponse().setHeader("Retry-After", String.valueOf(retryAfter));
        throw new HttpTooManyRequestsException(
            "too many attempts, retry after " + retryAfter + " seconds"
        );
    }

    private void revokeCurrentToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        for (Cookie cookie : cookies) {
            if (!"accessToken".equals(cookie.getName())) {
                continue;
            }
            try {
                UserAccessTokenModel token =
                    userService.getUserAccessTokenOrThrowByAccessToken(cookie.getValue());
                loginAccessTokenService.deleteAccessTokenByIdAndUserId(
                    token.getId(),
                    token.getUserId()
                );
            } catch (Exception e) {
                // an unknown or already expired token needs no revocation
            }
        }
    }

    /**
     * Wraps the response so the accessToken cookie leaves with HttpOnly and SameSite=Lax:
     * page scripts cannot read it and browsers do not send it on cross-site requests.
     * A dynamic proxy is used on purpose: an extra class in the plugin jar changes how the
     * platform resolves its own controllers on the container.
     */
    private static HttpServletResponse hardenResponse(HttpServletResponse response) {
        return (HttpServletResponse) Proxy.newProxyInstance(
            HttpServletResponse.class.getClassLoader(),
            new Class<?>[] {HttpServletResponse.class},
            (proxy, method, args) -> {
                Object[] params = args;
                if ("addCookie".equals(method.getName()) && args[0] instanceof Cookie) {
                    Cookie cookie = (Cookie) args[0];
                    if ("accessToken".equals(cookie.getName())) {
                        cookie.setHttpOnly(true);
                        cookie.setComment(SAME_SITE_LAX_COMMENT);
                    }
                } else if (("addHeader".equals(method.getName())
                    || "setHeader".equals(method.getName()))
                    && args.length == 2
                    && "Set-Cookie".equalsIgnoreCase(String.valueOf(args[0]))
                    && args[1] != null) {
                    params = new Object[] {args[0], hardenCookie(String.valueOf(args[1]))};
                }
                try {
                    return method.invoke(response, params);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        );
    }

    static String hardenCookie(String cookie) {
        if (!cookie.startsWith("accessToken=")) {
            return cookie;
        }
        String result = cookie;
        String lower = cookie.toLowerCase(Locale.ROOT);
        if (!lower.contains("; httponly")) {
            result += "; HttpOnly";
        }
        if (!lower.contains("; samesite=")) {
            result += "; SameSite=Lax";
        }
        return result;
    }

    private static int intProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
