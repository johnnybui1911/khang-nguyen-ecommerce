package com.ecommerce.ext.web.payment;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import org.youngmonkeys.ezyplatform.web.service.WebSettingService;

/** Reads the payment keys kept in the platform settings; blank means "not configured". */
@EzySingleton
public class KnPaymentSettings {

    public static final String SEPAY_WEBHOOK_KEY = "kn_sepay_webhook_key";
    public static final String SEPAY_API_TOKEN = "kn_sepay_api_token";
    public static final String SEPAY_API_BASE_URL = "kn_sepay_api_base_url";
    public static final String PAYOS_CHECKSUM_KEY = "kn_payos_checksum_key";
    public static final String CASSO_SECURE_TOKEN = "kn_casso_secure_token";

    @EzyAutoBind
    private WebSettingService settingService;

    public String get(String name) {
        return settingService.getSettingValue(name).map(String::trim).orElse("");
    }
}
