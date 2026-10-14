-- MySQL : les paiements enregistrés ne se modifient pas.
CREATE TRIGGER trg_supplier_payments_no_update BEFORE UPDATE ON supplier_payments
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Paiement fournisseur non modifiable';
CREATE TRIGGER trg_client_payments_no_update BEFORE UPDATE ON client_payments
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Règlement client non modifiable';
CREATE TRIGGER trg_goods_receipt_lines_no_update BEFORE UPDATE ON goods_receipt_lines
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ligne de réception non modifiable';
