package com.ecommerce.ext.web.payment;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Looks a confirming provider up by its webhook name. */
@EzySingleton
public class KnBankTxnProviders {

    @EzyAutoBind
    private KnSepayProvider sepay;

    @EzyAutoBind
    private KnPayosProvider payos;

    @EzyAutoBind
    private KnCassoProvider casso;

    public Optional<KnBankTxnProvider> find(String name) {
        return all().stream().filter(p -> p.getName().equals(name)).findFirst();
    }

    public List<KnBankTxnProvider> all() {
        return Arrays.asList(sepay, payos, casso);
    }
}
