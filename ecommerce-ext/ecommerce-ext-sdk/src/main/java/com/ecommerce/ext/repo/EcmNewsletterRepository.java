package com.ecommerce.ext.repo;

import com.ecommerce.ext.entity.EcmNewsletterSubscriber;
import com.tvd12.ezydata.database.EzyDatabaseRepository;
import com.tvd12.ezyfox.database.annotation.EzyQuery;
import com.tvd12.ezyfox.database.annotation.EzyRepository;

@EzyRepository
public interface EcmNewsletterRepository
    extends EzyDatabaseRepository<Long, EcmNewsletterSubscriber> {

    @EzyQuery("SELECT e FROM EcmNewsletterSubscriber e WHERE e.email = ?0")
    EcmNewsletterSubscriber findByEmailAddress(String email);

    @EzyQuery("SELECT e FROM EcmNewsletterSubscriber e WHERE e.confirmToken = ?0")
    EcmNewsletterSubscriber findByConfirmTokenValue(String token);

    @EzyQuery("SELECT e FROM EcmNewsletterSubscriber e WHERE e.unsubscribeToken = ?0")
    EcmNewsletterSubscriber findByUnsubscribeTokenValue(String token);
}
