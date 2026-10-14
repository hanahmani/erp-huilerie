package com.huilerie.erp;

import com.huilerie.erp.audit.AuditLogRepository;
import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.Role;
import com.huilerie.erp.users.RoleRepository;
import com.huilerie.erp.users.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Comptes créés uniquement pendant les tests, dans une base H2 en mémoire. */
@Component
public class TestUsers {

    public static final String PASSWORD = "MotDePasse2026";

    private final UserRepository users;
    private final RoleRepository roles;
    private final AuditLogRepository audit;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    public TestUsers(UserRepository users, RoleRepository roles, AuditLogRepository audit, PasswordEncoder encoder,
                     JdbcTemplate jdbc) {
        this.users = users;
        this.roles = roles;
        this.audit = audit;
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    /** Vide les tables métier (H2 uniquement : sous MySQL les journaux sont protégés par trigger). */
    public void reset() {
        for (String table : new String[]{"company_settings", "pressing_payments", "pressing_returns", "pressings",
                "production_byproducts", "production_inputs", "production_orders", "packaging_consumptions",
                "packaging_orders", "goods_receipt_lines", "goods_receipts", "supplier_payments",
                "purchase_lines", "purchases", "client_payments", "inventory_lines", "inventories", "expenses",
                "expense_categories", "cash_sessions", "taxes", "sale_payments", "sale_lines", "cash_movements", "sales",
                "stock_movements", "stock_balances", "stock_lots", "articles", "categories", "units",
                "clients", "suppliers", "document_sequences"}) {
            jdbc.update("DELETE FROM " + table);
        }
        users.deleteAll();
        audit.deleteAll();
    }

    public AppUser create(String username, boolean mustChangePassword, String... roleCodes) {
        Set<Role> r = new HashSet<>(roles.findByCodeIn(List.of(roleCodes)));
        return users.save(new AppUser(username, "Prénom", "Nom", encoder.encode(PASSWORD), r, mustChangePassword));
    }
}
