package com.ecommerce.ext.web.payment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Minimal blocking GET with short timeouts, for provider poll APIs. */
final class KnHttp {

    private static final int MAX_BYTES = 1 << 20;

    private KnHttp() {}

    static String get(String url, String authorization) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Authorization", authorization);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() / 100 != 2) {
                throw new IllegalStateException(
                    "provider answered " + connection.getResponseCode()
                );
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                    if (out.size() > MAX_BYTES) {
                        throw new IllegalStateException("provider response too large");
                    }
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("provider unreachable", e);
        }
    }
}
