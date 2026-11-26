-- Removes exactly the storefront tables created by the create script in the admin plugin resources.
-- Destroys their rows. Take a dump first. The platform runs every script found in the plugin resources, so this file lives here and is applied by hand only.

DROP TABLE IF EXISTS `ecm_wishlist`;
DROP TABLE IF EXISTS `ecm_newsletter`;
DROP TABLE IF EXISTS `ecm_payment_txn`;
