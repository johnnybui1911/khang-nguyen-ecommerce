package com.ecommerce.ext.web.payment;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.util.regex.Pattern;
import org.youngmonkeys.ezyplatform.model.UserModel;
import org.youngmonkeys.ezyplatform.service.UserService;

/**
 * The secret link of a guest order. A guest checks out under an anonymous user that the
 * platform creates for one browser, so the order page needs another way in from a second device
 * or the mail: {@code /orders/{id}?key=...}. The key is an HMAC of the order number under the
 * shop signing key, so it cannot be derived for another order, needs no storage and stays the
 * same for the life of the order. Without a signing key there is no link.
 */
@EzySingleton
public class KnOrderViewKeyService {

    /** Keeps the order-view MAC apart from the shop-confirm MAC made with the same key. */
    private static final String PURPOSE = "order-view:";
    private static final int KEY_CHARS = 32;
    /** ezylogin names anonymous users "anonymous" + 32 random letters/digits and gives no email. */
    private static final Pattern ANONYMOUS_NAME = Pattern.compile("^anonymous[A-Za-z0-9]{32}$");

    @EzyAutoBind
    private KnPaymentSettings settings;

    @EzyAutoBind
    private UserService userService;

    /** The view key of the order, or null while no signing key is configured. */
    public String keyOf(long orderId) {
        String secret = settings.get(KnPaymentSettings.SHOP_CONFIRM_KEY);
        if (secret.isEmpty() || orderId <= 0) {
            return null;
        }
        return KnSecrets.hmacSha256Hex(secret, PURPOSE + orderId).substring(0, KEY_CHARS);
    }

    public boolean matches(long orderId, String key) {
        String expected = keyOf(orderId);
        return expected != null && KnSecrets.sameSecret(expected, key == null ? "" : key.trim());
    }

    /** True when the user is an anonymous checkout user rather than a registered customer. */
    public boolean isGuest(long userId) {
        UserModel user = userId <= 0 ? null : userService.getUserById(userId);
        return user != null
            && user.getUsername() != null
            && ANONYMOUS_NAME.matcher(user.getUsername()).matches()
            && (user.getEmail() == null || user.getEmail().trim().isEmpty());
    }

    /** The order page path for this order; a guest order carries its view key. */
    public String orderPath(long orderId, long creatorUserId) {
        String path = "/orders/" + orderId;
        String key = isGuest(creatorUserId) ? keyOf(orderId) : null;
        return key == null ? path : path + "?key=" + key;
    }

    /** {@link #orderPath} as an absolute link for mails. */
    public String orderUrl(long orderId, long creatorUserId) {
        String base = settings.get(KnPaymentSettings.PUBLIC_BASE_URL);
        if (base.isEmpty()) {
            base = System.getProperty("ecm.base.url", "http://localhost:8080");
        }
        return base.replaceAll("/+$", "") + orderPath(orderId, creatorUserId);
    }
}
