-- Khang Nguyen storefront tables: wishlist and newsletter subscribers.
-- Only creates; safe to run more than once. Undo with ecommerce-ext/scripts/drop-ecm-tables.sql (applied by hand).

CREATE TABLE IF NOT EXISTS `ecm_wishlist` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `product_id` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_ecm_wishlist_user_product` (`user_id`,`product_id`),
  KEY `index_ecm_wishlist_user_created` (`user_id`,`created_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

CREATE TABLE IF NOT EXISTS `ecm_newsletter` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `email` varchar(254) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `confirm_token` varchar(64) NOT NULL,
  `unsubscribe_token` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_ecm_newsletter_email` (`email`),
  UNIQUE KEY `uniq_ecm_newsletter_confirm_token` (`confirm_token`),
  UNIQUE KEY `uniq_ecm_newsletter_unsubscribe_token` (`unsubscribe_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

CREATE TABLE IF NOT EXISTS `ecm_payment_txn` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `provider` varchar(30) NOT NULL,
  `txn_id` varchar(120) NOT NULL,
  `order_id` bigint unsigned NOT NULL DEFAULT 0,
  `amount` bigint unsigned NOT NULL DEFAULT 0,
  `content` varchar(500) DEFAULT NULL,
  `result` varchar(40) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_ecm_payment_txn_provider_txn` (`provider`,`txn_id`),
  KEY `index_ecm_payment_txn_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
