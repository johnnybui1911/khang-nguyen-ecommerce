package com.ecommerce.ext.web.controller.api;

import com.ecommerce.ext.entity.KnNewsletterSubscriber;
import com.ecommerce.ext.repo.KnNewsletterRepository;
import com.ecommerce.ext.web.guard.KnRateLimiter;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.core.exception.HttpTooManyRequestsException;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoPost;
import com.tvd12.ezyhttp.server.core.annotation.RequestBody;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import org.youngmonkeys.ezymail.entity.MailType;
import org.youngmonkeys.ezymail.model.MailModel;
import org.youngmonkeys.ezymail.web.mail.WebMailDeliver;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Newsletter sign-up with double opt-in. A sign-up mails a confirmation link; only following it
 * confirms the address, and every mail carries a link that unsubscribes. The sign-up answers the
 * same whether or not the address is already known, so the form cannot be used to find out who
 * subscribes. Confirm and unsubscribe are POSTs, so a mail scanner that opens a link does not
 * change anything; the links open storefront pages that perform the POST.
 *
 * <p>Configuration (system properties): {@code ecm.base.url} (origin used in mailed links),
 * {@code ecm.ratelimit.newsletter.max} and {@code ecm.ratelimit.window.seconds}.
 */
@Api
@Controller("/api/v1/ecm")
public class KnNewsletterApiController {

    private static final Pattern EMAIL =
        Pattern.compile("^[A-Za-z0-9._%+'-]{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,24}$");
    private static final Pattern TOKEN = Pattern.compile("^[0-9a-f]{64}$");
    private static final long RESEND_AFTER_SECONDS = 300L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KnRateLimiter limiter = new KnRateLimiter(
        intProperty("ecm.ratelimit.newsletter.max", 10),
        intProperty("ecm.ratelimit.window.seconds", 60) * 1000L
    );

    @EzyAutoBind
    private KnNewsletterRepository ecmNewsletterRepository;

    @EzyAutoBind
    private WebMailDeliver mailDeliver;

    @DoPost("/newsletter")
    public Map<String, Object> subscribePost(
        RequestArguments arguments,
        @RequestBody SubscribeRequest request
    ) {
        throttle(arguments);
        final String email = request.getEmail() == null
            ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || email.length() > 254 || !EMAIL.matcher(email).matches()) {
            Map<String, String> errors = new HashMap<>();
            errors.put("email", email.isEmpty() ? "required" : "invalid");
            throw new HttpBadRequestException(errors);
        }
        if (request.getWebsite() != null && !request.getWebsite().trim().isEmpty()) {
            return accepted();
        }
        KnNewsletterSubscriber row = ecmNewsletterRepository.findByEmailAddress(email);
        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new KnNewsletterSubscriber();
            row.setEmail(email);
            row.setStatus(KnNewsletterSubscriber.PENDING);
            row.setConfirmToken(newToken());
            row.setUnsubscribeToken(newToken());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            try {
                ecmNewsletterRepository.save(row);
            } catch (RuntimeException e) {
                row = ecmNewsletterRepository.findByEmailAddress(email);
                if (row == null) {
                    throw e;
                }
                return accepted();
            }
            mailConfirmation(row);
        } else if (KnNewsletterSubscriber.UNSUBSCRIBED.equals(row.getStatus())) {
            row.setStatus(KnNewsletterSubscriber.PENDING);
            row.setConfirmToken(newToken());
            row.setUpdatedAt(now);
            ecmNewsletterRepository.save(row);
            mailConfirmation(row);
        } else if (KnNewsletterSubscriber.PENDING.equals(row.getStatus())
            && row.getUpdatedAt().isBefore(now.minusSeconds(RESEND_AFTER_SECONDS))) {
            row.setUpdatedAt(now);
            ecmNewsletterRepository.save(row);
            mailConfirmation(row);
        }
        return accepted();
    }

    @DoPost("/newsletter/confirm")
    public Map<String, Object> confirmPost(
        RequestArguments arguments,
        @RequestBody TokenRequest request
    ) {
        throttle(arguments);
        KnNewsletterSubscriber row = isToken(request.getToken())
            ? ecmNewsletterRepository.findByConfirmTokenValue(request.getToken()) : null;
        if (row == null || KnNewsletterSubscriber.UNSUBSCRIBED.equals(row.getStatus())) {
            throw new HttpNotFoundException("link not valid");
        }
        if (KnNewsletterSubscriber.PENDING.equals(row.getStatus())) {
            row.setStatus(KnNewsletterSubscriber.CONFIRMED);
            row.setUpdatedAt(LocalDateTime.now());
            ecmNewsletterRepository.save(row);
        }
        return status(row);
    }

    @DoPost("/newsletter/unsubscribe")
    public Map<String, Object> unsubscribePost(
        RequestArguments arguments,
        @RequestBody TokenRequest request
    ) {
        throttle(arguments);
        KnNewsletterSubscriber row = isToken(request.getToken())
            ? ecmNewsletterRepository.findByUnsubscribeTokenValue(request.getToken()) : null;
        if (row == null) {
            throw new HttpNotFoundException("link not valid");
        }
        if (!KnNewsletterSubscriber.UNSUBSCRIBED.equals(row.getStatus())) {
            row.setStatus(KnNewsletterSubscriber.UNSUBSCRIBED);
            row.setUpdatedAt(LocalDateTime.now());
            ecmNewsletterRepository.save(row);
        }
        return status(row);
    }

    private void mailConfirmation(KnNewsletterSubscriber row) {
        String base = System.getProperty("ecm.base.url", "http://localhost:8080");
        String body = "Please confirm your Khang Nguyen newsletter subscription:\n\n"
            + base + "/newsletter/confirm?token=" + row.getConfirmToken() + "\n\n"
            + "If you did not ask for this, ignore this message or unsubscribe here:\n"
            + base + "/newsletter/unsubscribe?token=" + row.getUnsubscribeToken() + "\n";
        mailDeliver.send(
            MailModel.builder()
                .type(MailType.USER)
                .to(row.getEmail())
                .title("Confirm your newsletter subscription")
                .content(body)
                .textContentType("text/plain; charset=utf-8")
                .build()
        );
    }

    private void throttle(RequestArguments arguments) {
        long retryAfter = limiter.tryAcquire(
            "newsletter|" + arguments.getRequest().getRemoteAddr(),
            System.currentTimeMillis()
        );
        if (retryAfter > 0) {
            arguments.getResponse().setHeader("Retry-After", String.valueOf(retryAfter));
            throw new HttpTooManyRequestsException(
                "too many requests, retry after " + retryAfter + " seconds"
            );
        }
    }

    private static boolean isToken(String token) {
        return token != null && TOKEN.matcher(token).matches();
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(64);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static Map<String, Object> accepted() {
        return Collections.singletonMap("accepted", Boolean.TRUE);
    }

    private static Map<String, Object> status(KnNewsletterSubscriber row) {
        return Collections.singletonMap("status", row.getStatus());
    }

    private static int intProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static class SubscribeRequest {
        private String email;
        private String website;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }
    }

    public static class TokenRequest {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
