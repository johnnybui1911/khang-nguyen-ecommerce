-- Undo seed.sql. Order history that already used KN_COD / KN_BANK keeps its code.
DELETE FROM ecommerce_payment_service_settings WHERE payment_service_code IN ('KN_COD', 'KN_BANK');
DELETE FROM ecommerce_shop_bank_accounts WHERE shop_id = 1 AND account_number IN ('PLACEHOLDER', '19020221216026');
-- The seeded banks are left in place; delete them by hand only if nothing references them.
