package com.ecommerce.ext.repo;

import com.ecommerce.ext.entity.KnWishlistItem;
import com.tvd12.ezydata.database.EzyDatabaseRepository;
import com.tvd12.ezyfox.database.annotation.EzyQuery;
import com.tvd12.ezyfox.database.annotation.EzyRepository;
import com.tvd12.ezyfox.util.Next;

import java.util.List;

@EzyRepository
public interface KnWishlistRepository extends EzyDatabaseRepository<Long, KnWishlistItem> {

    @EzyQuery("SELECT e FROM KnWishlistItem e WHERE e.userId = ?0 AND e.productId = ?1")
    KnWishlistItem findByUserAndProduct(long userId, long productId);

    @EzyQuery(
        "SELECT e FROM KnWishlistItem e WHERE e.userId = ?0 "
            + "ORDER BY e.createdAt DESC, e.id DESC"
    )
    List<KnWishlistItem> findByUser(long userId, Next next);

    @EzyQuery("SELECT COUNT(e.id) FROM KnWishlistItem e WHERE e.userId = ?0")
    long countByUser(long userId);
}
