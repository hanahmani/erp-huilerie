-- MySQL uniquement : les journaux ne peuvent être ni modifiés ni supprimés.
-- Une correction passe toujours par un nouveau mouvement compensatoire.

CREATE TRIGGER trg_stock_movements_no_update BEFORE UPDATE ON stock_movements
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Journal de stock non modifiable';

CREATE TRIGGER trg_stock_movements_no_delete BEFORE DELETE ON stock_movements
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Journal de stock non supprimable';

CREATE TRIGGER trg_cash_movements_no_update BEFORE UPDATE ON cash_movements
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Journal de caisse non modifiable';

CREATE TRIGGER trg_cash_movements_no_delete BEFORE DELETE ON cash_movements
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Journal de caisse non supprimable';

CREATE TRIGGER trg_audit_logs_no_update BEFORE UPDATE ON audit_logs
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Journal d''audit non modifiable';

CREATE TRIGGER trg_audit_logs_no_delete BEFORE DELETE ON audit_logs
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Journal d''audit non supprimable';

CREATE TRIGGER trg_sale_lines_no_update BEFORE UPDATE ON sale_lines
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ligne de vente validée non modifiable';
