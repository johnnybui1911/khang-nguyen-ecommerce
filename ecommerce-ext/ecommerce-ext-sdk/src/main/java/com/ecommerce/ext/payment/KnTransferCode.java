package com.ecommerce.ext.payment;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Transfer content that ties a bank transaction to an order: {@code KN} + 5-digit order id. */
public final class KnTransferCode {

    private static final Pattern PATTERN = Pattern.compile("(?<![A-Za-z0-9])KN(\\d{5,})(?!\\d)");

    private KnTransferCode() {}

    public static String of(long orderId) {
        return String.format("KN%05d", orderId);
    }

    /**
     * Finds the order id inside free-form transfer content. Banks may add text
     * around the code or change its case, so the match is case-insensitive.
     */
    public static Optional<Long> extractOrderId(String content) {
        if (content == null) {
            return Optional.empty();
        }
        Matcher matcher = PATTERN.matcher(content.toUpperCase());
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(matcher.group(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
