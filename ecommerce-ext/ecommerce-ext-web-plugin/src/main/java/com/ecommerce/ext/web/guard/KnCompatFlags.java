package com.ecommerce.ext.web.guard;

import java.util.Locale;

/**
 * Feature flags {@code ecm.compat.<name>.enabled} for the compatibility controllers. Read from
 * the JVM system property of that name, then from the environment variable
 * {@code ECM_COMPAT_<NAME>_ENABLED}; enabled when neither is set.
 */
public final class KnCompatFlags {

    private KnCompatFlags() {}

    public static boolean isEnabled(String name) {
        String key = "ecm.compat." + name + ".enabled";
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(
                "ECM_COMPAT_" + name.toUpperCase(Locale.ROOT).replace('-', '_') + "_ENABLED"
            );
        }
        return value == null || !"false".equalsIgnoreCase(value.trim());
    }
}
