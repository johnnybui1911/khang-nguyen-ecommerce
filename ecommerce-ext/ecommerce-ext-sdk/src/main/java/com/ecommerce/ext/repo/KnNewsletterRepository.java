package com.ecommerce.ext.repo;

import com.ecommerce.ext.entity.KnNewsletterSubscriber;
import com.tvd12.ezydata.database.EzyDatabaseRepository;
import com.tvd12.ezyfox.database.annotation.EzyQuery;
import com.tvd12.ezyfox.database.annotation.EzyRepository;

@EzyRepository
public interface KnNewsletterRepository
    extends EzyDatabaseRepository<Long, KnNewsletterSubscriber> {

    @EzyQuery("SELECT e FROM KnNewsletterSubscriber e WHERE e.email = ?0")
    KnNewsletterSubscriber findByEmailAddress(String email);

    @EzyQuery("SELECT e FROM KnNewsletterSubscriber e WHERE e.confirmToken = ?0")
    KnNewsletterSubscriber findByConfirmTokenValue(String token);

    @EzyQuery("SELECT e FROM KnNewsletterSubscriber e WHERE e.unsubscribeToken = ?0")
    KnNewsletterSubscriber findByUnsubscribeTokenValue(String token);
}
