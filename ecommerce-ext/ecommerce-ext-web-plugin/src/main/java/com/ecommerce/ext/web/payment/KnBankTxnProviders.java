package com.ecommerce.ext.web.payment;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.util.List;
import java.util.Optional;

/** Looks a confirming provider up by its webhook name. */
@EzySingleton
public class KnBankTxnProviders {

    @EzyAutoBind
    private List<KnBankTxnProvider> providers;

    public Optional<KnBankTxnProvider> find(String name) {
        return providers.stream().filter(p -> p.getName().equals(name)).findFirst();
    }

    public List<KnBankTxnProvider> all() {
        return providers;
    }
}
