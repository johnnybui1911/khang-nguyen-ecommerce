package com.ecommerce.ext.web.controller.api;

import com.ecommerce.ext.entity.KnWishlistItem;
import com.ecommerce.ext.repo.KnWishlistRepository;
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.util.Next;
import com.tvd12.ezyhttp.core.exception.HttpNotFoundException;
import com.tvd12.ezyhttp.core.response.ResponseEntity;
import com.tvd12.ezyhttp.server.core.annotation.Api;
import com.tvd12.ezyhttp.server.core.annotation.Authenticated;
import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoDelete;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.DoPut;
import com.tvd12.ezyhttp.server.core.annotation.PathVariable;
import com.tvd12.ezyhttp.server.core.annotation.RequestParam;
import org.youngmonkeys.ecommerce.model.ProductCurrencyModel;
import org.youngmonkeys.ecommerce.web.controller.service.WebProductControllerService;
import org.youngmonkeys.ecommerce.web.response.WebProductResponse;
import org.youngmonkeys.ecommerce.web.service.WebEcommerceSettingService;
import org.youngmonkeys.ecommerce.web.service.WebProductCurrencyService;
import org.youngmonkeys.ezyplatform.web.annotation.UserId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The signed-in customer's favourite products. The owner is always the authenticated user; no
 * route takes a user id, so one customer can neither read nor change another's list. Adding and
 * removing are idempotent and answer 204.
 */
@Api
@Authenticated
@Controller("/api/v1/ecm")
public class KnWishlistApiController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_ITEMS = 500;

    @EzyAutoBind
    private KnWishlistRepository ecmWishlistRepository;

    @EzyAutoBind
    private WebProductControllerService productControllerService;

    @EzyAutoBind
    private WebEcommerceSettingService ecommerceSettingService;

    @EzyAutoBind
    private WebProductCurrencyService currencyService;

    @DoGet("/wishlist")
    public Map<String, Object> wishlistGet(
        @UserId long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    ) {
        int pageSize = size <= 0 ? 24 : Math.min(size, MAX_PAGE_SIZE);
        int pageIndex = Math.max(page, 1);
        List<KnWishlistItem> rows = ecmWishlistRepository.findByUser(
            userId,
            Next.fromSkipLimit((long) (pageIndex - 1) * pageSize, pageSize)
        );
        List<Long> ids = new ArrayList<>();
        for (KnWishlistItem row : rows) {
            ids.add(row.getProductId());
        }
        Map<Long, WebProductResponse> byId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (WebProductResponse product : productControllerService.getProducts(
                ids,
                "en",
                currency()
            )) {
                byId.put(product.getId(), product);
            }
        }
        List<Object> items = new ArrayList<>();
        for (Long id : ids) {
            WebProductResponse product = byId.get(id);
            if (product != null) {
                items.add(product);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("productIds", ids);
        body.put("total", ecmWishlistRepository.countByUser(userId));
        body.put("page", pageIndex);
        body.put("size", pageSize);
        return body;
    }

    /** Every favourite product id of the caller, newest first, so a page can mark its hearts. */
    @DoGet("/wishlist/ids")
    public Map<String, Object> wishlistIdsGet(@UserId long userId) {
        List<Long> ids = new ArrayList<>();
        List<KnWishlistItem> rows =
            ecmWishlistRepository.findByUser(userId, Next.limit(MAX_ITEMS));
        for (KnWishlistItem row : rows) {
            ids.add(row.getProductId());
        }
        return Collections.singletonMap("ids", ids);
    }

    @DoPut("/wishlist/{productId}")
    public ResponseEntity wishlistPut(
        @UserId long userId,
        @PathVariable("productId") long productId
    ) {
        if (productId <= 0 || !productExists(productId)) {
            throw new HttpNotFoundException("product not found");
        }
        if (ecmWishlistRepository.findByUserAndProduct(userId, productId) == null
            && ecmWishlistRepository.countByUser(userId) < MAX_ITEMS) {
            KnWishlistItem item = new KnWishlistItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setCreatedAt(LocalDateTime.now());
            try {
                ecmWishlistRepository.save(item);
            } catch (RuntimeException e) {
                // A concurrent identical request won the unique key; the outcome is the same.
                if (ecmWishlistRepository.findByUserAndProduct(userId, productId) == null) {
                    throw e;
                }
            }
        }
        return ResponseEntity.noContent();
    }

    @DoDelete("/wishlist/{productId}")
    public ResponseEntity wishlistDelete(
        @UserId long userId,
        @PathVariable("productId") long productId
    ) {
        KnWishlistItem item = ecmWishlistRepository.findByUserAndProduct(userId, productId);
        if (item != null) {
            ecmWishlistRepository.delete(item.getId());
        }
        return ResponseEntity.noContent();
    }

    private boolean productExists(long productId) {
        try {
            return productControllerService.getProductDetailsByIdAndStatuses(
                productId,
                ecommerceSettingService.getDisplayableProductStatuses(),
                "en",
                currency()
            ) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private ProductCurrencyModel currency() {
        return currencyService.getCurrencyByIdOrDefault(0L);
    }
}
