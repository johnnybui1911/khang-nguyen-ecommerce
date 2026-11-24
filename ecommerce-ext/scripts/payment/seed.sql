-- Payment methods for the storefront: cash on delivery (KN_COD) and bank transfer (KN_BANK),
-- plus the shop bank account shown to customers. Safe to run more than once.
-- The account number is a placeholder until the shop gives the real one; the order page
-- shows no QR code and no transfer details while it is still PLACEHOLDER.
-- Undo with rollback.sql.

SET NAMES utf8mb4;

INSERT INTO ecommerce_banks (bank_code, bin_code, bank_name, address, url, logo_image_id, banner_image_id, status, created_at, updated_at)
SELECT v.bank_code, v.bin_code, v.bank_name, '', '', 0, 0, 'ACTIVATED', NOW(), NOW()
FROM (
  SELECT 'MB' AS bank_code, '970422' AS bin_code, 'Ngân hàng TMCP Quân đội (MB Bank)' AS bank_name
  UNION ALL SELECT 'VCB', '970436', 'Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank)'
  UNION ALL SELECT 'TCB', '970407', 'Ngân hàng TMCP Kỹ thương Việt Nam (Techcombank)'
  UNION ALL SELECT 'ACB', '970416', 'Ngân hàng TMCP Á Châu (ACB)'
  UNION ALL SELECT 'BIDV', '970418', 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam (BIDV)'
  UNION ALL SELECT 'CTG', '970415', 'Ngân hàng TMCP Công thương Việt Nam (VietinBank)'
  UNION ALL SELECT 'VPB', '970432', 'Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)'
  UNION ALL SELECT 'TPB', '970423', 'Ngân hàng TMCP Tiên Phong (TPBank)'
) v
WHERE NOT EXISTS (SELECT 1 FROM ecommerce_banks b WHERE b.bank_code = v.bank_code);

INSERT INTO ecommerce_shop_bank_accounts (shop_id, bank_id, bank_branch_of_account, account_number, account_owner_name, account_qr_code_image_id, display_order, status, created_at, updated_at)
SELECT 1, (SELECT id FROM ecommerce_banks WHERE bank_code = 'MB'), '', 'PLACEHOLDER', 'KHANG NGUYEN', 0, 1, 'ACTIVATED', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ecommerce_shop_bank_accounts WHERE shop_id = 1);

INSERT INTO ecommerce_payment_service_settings (payment_service_code, payment_service_name, payment_service_version, logo_image_id, banner_image_id, bank_id, currency_id, priority, handling_service, status, created_at, updated_at)
SELECT 'KN_BANK', 'Chuyển khoản ngân hàng', '1', 0, 0, 0, 6, 2, 'KN_BANK', 'ACTIVATED', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ecommerce_payment_service_settings WHERE payment_service_code = 'KN_BANK');

INSERT INTO ecommerce_payment_service_settings (payment_service_code, payment_service_name, payment_service_version, logo_image_id, banner_image_id, bank_id, currency_id, priority, handling_service, status, created_at, updated_at)
SELECT 'KN_COD', 'Thanh toán khi nhận hàng (COD)', '1', 0, 0, 0, 6, 3, 'KN_COD', 'ACTIVATED', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ecommerce_payment_service_settings WHERE payment_service_code = 'KN_COD');
