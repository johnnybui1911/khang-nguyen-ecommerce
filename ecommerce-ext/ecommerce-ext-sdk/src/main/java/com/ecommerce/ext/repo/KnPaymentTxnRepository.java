package com.ecommerce.ext.repo;

import com.ecommerce.ext.entity.KnPaymentTxn;
import com.tvd12.ezydata.database.EzyDatabaseRepository;
import com.tvd12.ezyfox.database.annotation.EzyQuery;
import com.tvd12.ezyfox.database.annotation.EzyRepository;

@EzyRepository
public interface KnPaymentTxnRepository extends EzyDatabaseRepository<Long, KnPaymentTxn> {

    @EzyQuery("SELECT e FROM KnPaymentTxn e WHERE e.provider = ?0 AND e.txnId = ?1")
    KnPaymentTxn findByProviderAndTxnId(String provider, String txnId);
}
