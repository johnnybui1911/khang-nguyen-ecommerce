package com.ecommerce.ext.web.controller.api;

import com.ecommerce.ext.web.guard.EcmRateLimiter;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyhttp.core.exception.HttpBadRequestException;
import com.tvd12.ezyhttp.core.exception.HttpTooManyRequestsException;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoPost;
import com.tvd12.ezyhttp.server.core.annotation.RequestBody;
import com.tvd12.ezyhttp.server.core.request.RequestArguments;
import org.youngmonkeys.ezymail.entity.MailType;
import org.youngmonkeys.ezymail.model.MailModel;
import org.youngmonkeys.ezymail.web.mail.WebMailDeliver;
import org.youngmonkeys.ezyplatform.web.service.WebSettingService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Customer contact and store appointment requests, mailed to the shop. Nothing is stored: the
 * request becomes one mail through the platform mail service, so the delivery channel (sandbox or
 * a real SMTP server) is whatever the mail plugin is configured for.
 *
 * <p>Configuration (system properties):
 * <ul>
 *   <li>{@code ecm.shop.mail} - recipient of both kinds of request, used only when the platform
 *   setting {@code ecm_shop_mail} is empty;</li>
 *   <li>{@code ecm.stores} - comma separated store ids an appointment may name;</li>
 *   <li>{@code ecm.ratelimit.contact.max} and {@code ecm.ratelimit.window.seconds} - requests
 *   allowed per client address (the window is shared with the other limiters).</li>
 * </ul>
 * Every field is validated and none of the visitor's text goes into a mail header except the
 * validated reply address, so the mail cannot be turned into a header injection.
 */
@Api
@Controller("/api/v1/ecm")
public class EcmStoreContactApiController {

    private static final Pattern EMAIL =
        Pattern.compile("^[A-Za-z0-9._%+'-]{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,24}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final Pattern TIME = Pattern.compile("^(1[0-9]|2[01]):[03]0$");
    private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Singapore");
    private static final int MAX_DAYS_AHEAD = 90;
    private static final List<String> TOPICS = Arrays.asList(
        "order", "product", "repair", "gift-card", "other"
    );
    private static final Map<String, String> STORE_NAMES = storeNames();
    private static final String DEFAULT_STORES =
        "ion-orchard,marina-bay-sands,vivocity,suntec-city,parkway-parade";

    private final EcmRateLimiter limiter = new EcmRateLimiter(
        intProperty("ecm.ratelimit.contact.max", 5),
        intProperty("ecm.ratelimit.window.seconds", 60) * 1000L
    );

    @EzyAutoBind
    private WebMailDeliver mailDeliver;

    @EzyAutoBind
    private WebSettingService settingService;

    @DoPost("/contact")
    public Map<String, Object> contactPost(
        RequestArguments arguments,
        @RequestBody ContactRequest request
    ) {
        throttle(arguments, "contact");
        Map<String, String> errors = new LinkedHashMap<>();
        final String name = text(request.getName(), "name", 100, true, false, errors);
        final String email = email(request.getEmail(), errors);
        final String phone = phone(request.getPhone(), false, errors);
        final String message = text(request.getMessage(), "message", 2000, true, true, errors);
        String topic = request.getTopic() == null ? "other" : request.getTopic().trim();
        if (!TOPICS.contains(topic)) {
            errors.put("topic", "invalid");
        }
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        if (isBot(request.getWebsite())) {
            return accepted();
        }
        StringBuilder body = new StringBuilder();
        body.append("Name: ").append(name).append('\n')
            .append("Email: ").append(email).append('\n');
        if (!phone.isEmpty()) {
            body.append("Phone: ").append(phone).append('\n');
        }
        body.append("Topic: ").append(topic).append("\n\n").append(message).append('\n');
        deliver(MailType.CONTACT_US, "[Contact] " + topic, email, body.toString());
        return accepted();
    }

    @DoPost("/appointments")
    public Map<String, Object> appointmentPost(
        RequestArguments arguments,
        @RequestBody AppointmentRequest request
    ) {
        throttle(arguments, "appointment");
        Map<String, String> errors = new LinkedHashMap<>();
        String store = request.getStoreId() == null ? "" : request.getStoreId().trim();
        if (!allowedStores().contains(store)) {
            errors.put("storeId", "invalid");
        }
        String date = request.getDate() == null ? "" : request.getDate().trim();
        if (!validDate(date)) {
            errors.put("date", "invalid");
        }
        String time = request.getTime() == null ? "" : request.getTime().trim();
        if (!TIME.matcher(time).matches()) {
            errors.put("time", "invalid");
        }
        String name = text(request.getName(), "name", 100, true, false, errors);
        String phone = phone(request.getPhone(), true, errors);
        String email = email(request.getEmail(), errors);
        final String note = text(request.getNote(), "note", 500, false, true, errors);
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        if (isBot(request.getWebsite())) {
            return accepted();
        }
        StringBuilder body = new StringBuilder();
        String storeLabel = storeLabel(store);
        body.append("Store: ").append(storeLabel).append('\n')
            .append("Date: ").append(date).append('\n')
            .append("Time: ").append(time).append('\n')
            .append("Name: ").append(name).append('\n')
            .append("Phone: ").append(phone).append('\n')
            .append("Email: ").append(email).append('\n');
        if (!note.isEmpty()) {
            body.append("\n").append(note).append('\n');
        }
        deliver(
            MailType.REGISTER_CONSULTATION,
            "[Appointment] " + storeLabel + " " + date + " " + time,
            email,
            body.toString()
        );
        return accepted();
    }

    private void deliver(MailType type, String title, String replyTo, String body) {
        mailDeliver.send(
            MailModel.builder()
                .type(type)
                .to(shopMail())
                .title(title)
                .content(body)
                .textContentType("text/plain; charset=utf-8")
                .header("Reply-To", replyTo)
                .build()
        );
    }

    private String shopMail() {
        String configured = settingService.getTextValue("ecm_shop_mail", "");
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }
        return System.getProperty("ecm.shop.mail", "stores@ecommerce.local");
    }

    private void throttle(RequestArguments arguments, String key) {
        long retryAfter = limiter.tryAcquire(
            key + "|" + arguments.getRequest().getRemoteAddr(),
            System.currentTimeMillis()
        );
        if (retryAfter > 0) {
            arguments.getResponse().setHeader("Retry-After", String.valueOf(retryAfter));
            throw new HttpTooManyRequestsException(
                "too many requests, retry after " + retryAfter + " seconds"
            );
        }
    }

    private static boolean isBot(String honeypot) {
        return honeypot != null && !honeypot.trim().isEmpty();
    }

    private static Map<String, Object> accepted() {
        return Collections.singletonMap("accepted", Boolean.TRUE);
    }

    private static HttpBadRequestException invalid(Map<String, String> errors) {
        return new HttpBadRequestException(new HashMap<String, String>(errors));
    }

    /** A single-line value, or when {@code multiline} a value that may hold line breaks. */
    private static String text(
        String value,
        String field,
        int max,
        boolean required,
        boolean multiline,
        Map<String, String> errors
    ) {
        String v = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (v.isEmpty()) {
            if (required) {
                errors.put(field, "required");
            }
            return "";
        }
        if (v.length() > max) {
            errors.put(field, "too-long");
            return "";
        }
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            boolean control = Character.isISOControl(c) && !(multiline && c == '\n');
            if (control) {
                errors.put(field, "invalid");
                return "";
            }
        }
        return v;
    }

    private static String email(String value, Map<String, String> errors) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            errors.put("email", "required");
        } else if (v.length() > 254 || !EMAIL.matcher(v).matches()) {
            errors.put("email", "invalid");
        }
        return v;
    }

    private static String phone(String value, boolean required, Map<String, String> errors) {
        String v = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (v.isEmpty()) {
            if (required) {
                errors.put("phone", "required");
            }
            return "";
        }
        if (!PHONE.matcher(v).matches()) {
            errors.put("phone", "invalid");
        }
        return v;
    }

    private static boolean validDate(String date) {
        try {
            LocalDate day = LocalDate.parse(date);
            LocalDate today = LocalDate.now(SHOP_ZONE);
            return !day.isBefore(today) && !day.isAfter(today.plusDays(MAX_DAYS_AHEAD));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static Map<String, String> storeNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("ion-orchard", "Khang Nguyen ION Orchard");
        names.put("marina-bay-sands", "Khang Nguyen Marina Bay Sands");
        names.put("vivocity", "Khang Nguyen VivoCity");
        names.put("suntec-city", "Khang Nguyen Suntec City");
        names.put("parkway-parade", "Khang Nguyen Parkway Parade");
        return names;
    }

    /** The store's display name when known, with its id so the shop can still look it up. */
    private static String storeLabel(String storeId) {
        String name = STORE_NAMES.get(storeId);
        return name == null ? storeId : name + " (" + storeId + ")";
    }

    private static List<String> allowedStores() {
        return Arrays.asList(
            System.getProperty("ecm.stores", DEFAULT_STORES)
                .toLowerCase(Locale.ROOT)
                .split("\\s*,\\s*")
        );
    }

    private static int intProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static class ContactRequest {
        private String name;
        private String email;
        private String phone;
        private String topic;
        private String message;
        private String website;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }
    }

    public static class AppointmentRequest {
        private String storeId;
        private String date;
        private String time;
        private String name;
        private String phone;
        private String email;
        private String note;
        private String website;

        public String getStoreId() {
            return storeId;
        }

        public void setStoreId(String storeId) {
            this.storeId = storeId;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }
    }
}
