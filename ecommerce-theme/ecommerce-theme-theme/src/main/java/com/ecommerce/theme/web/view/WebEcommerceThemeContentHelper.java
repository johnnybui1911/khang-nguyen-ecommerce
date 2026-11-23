package com.ecommerce.theme.web.view;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.server.core.view.View;
import org.youngmonkeys.ezyplatform.model.MediaNameModel;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@EzySingleton
public class WebEcommerceThemeContentHelper {

    public static final String VIEW_VARIABLE_HELPER = "knContent";
    public static final String VIEW_VARIABLE_META_TITLE = "knMetaTitle";
    public static final String VIEW_VARIABLE_META_HEADING = "knMetaHeading";
    public static final String VIEW_VARIABLE_META_DESCRIPTION = "knMetaDescription";
    public static final String VIEW_VARIABLE_META_IMAGE = "knMetaImage";
    public static final String VIEW_VARIABLE_META_URL = "knMetaUrl";
    public static final String SITE_NAME = "Khang Nguyên · Ngọc Bảo";
    private static final int DESCRIPTION_LENGTH = 160;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
        .ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public String date(long millis) {
        return millis > 0 ? DATE_FORMAT.format(Instant.ofEpochMilli(millis)) : "";
    }

    public String text(String html) {
        if (html == null) {
            return "";
        }
        return html
            .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
            .replaceAll("<[^>]+>", " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replaceAll("\\s+", " ")
            .trim();
    }

    public String excerpt(String summary, String html) {
        String source = text(summary);
        if (source.isEmpty()) {
            source = text(html);
        }
        if (source.length() <= DESCRIPTION_LENGTH) {
            return source;
        }
        String cut = source.substring(0, DESCRIPTION_LENGTH);
        int space = cut.lastIndexOf(' ');
        return (space > DESCRIPTION_LENGTH / 2 ? cut.substring(0, space) : cut) + "…";
    }

    public void decorate(
        HttpServletRequest request,
        View view,
        String title,
        String description,
        MediaNameModel image
    ) {
        String base = baseUrl(request);
        view.setVariable(VIEW_VARIABLE_HELPER, this);
        view.setVariable(VIEW_VARIABLE_META_HEADING, title);
        view.setVariable(VIEW_VARIABLE_META_TITLE, title + " | " + SITE_NAME);
        view.setVariable(VIEW_VARIABLE_META_DESCRIPTION, description);
        view.setVariable(VIEW_VARIABLE_META_URL, base + request.getRequestURI());
        if (image != null && image.getUrl() != null && !image.getUrl().isEmpty()) {
            String url = image.getUrl();
            view.setVariable(
                VIEW_VARIABLE_META_IMAGE,
                url.startsWith("http") ? url : base + url
            );
        }
    }

    private String baseUrl(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        String scheme = proto != null && !proto.isEmpty() ? proto : request.getScheme();
        String host = request.getHeader("Host");
        if (host == null || host.isEmpty()) {
            host = request.getServerName();
        }
        return scheme + "://" + host;
    }
}
