package com.huilerie.erp;

import com.huilerie.erp.catalog.*;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.clients.ClientRepository;
import com.huilerie.erp.clients.ClientType;
import com.huilerie.erp.inventory.*;
import com.huilerie.erp.suppliers.Supplier;
import com.huilerie.erp.suppliers.SupplierRepository;
import com.huilerie.erp.suppliers.SupplierType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/** Données créées uniquement dans la base H2 des tests. */
@Component
public class SalesFixtures {

    private final UnitRepository units;
    private final ArticleRepository articles;
    private final ClientRepository clients;
    private final StockLotRepository lots;
    private final StockService stock;
    private final TransactionTemplate tx;
    private SupplierRepository suppliers;

    @org.springframework.beans.factory.annotation.Autowired
    void setSuppliers(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    public Supplier supplier(String nom) {
        Supplier s = new Supplier();
        s.setCode("FRN-T-" + nom);
        s.setType(SupplierType.ENTREPRISE);
        s.setNom(nom);
        s.setMatriculeFiscal("MF-" + nom);
        return suppliers.save(s);
    }

    public SalesFixtures(UnitRepository units, ArticleRepository articles, ClientRepository clients,
                         StockLotRepository lots, StockService stock, TransactionTemplate tx) {
        this.units = units;
        this.articles = articles;
        this.clients = clients;
        this.lots = lots;
        this.stock = stock;
        this.tx = tx;
    }

    public Article article(String ref, String price, boolean lotTracked) {
        Unit u = units.findAll().stream().filter(x -> x.getCode().equals("L")).findFirst()
                .orElseGet(() -> units.save(new Unit("L", "Litre")));
        Article a = new Article();
        a.setReference(ref);
        a.setNom("Huile " + ref);
        a.setType(ArticleType.HUILE_VRAC);
        a.setUnite(u);
        a.setPrixVente(new BigDecimal(price));
        a.setVendable(true);
        a.setSuiviLot(lotTracked);
        return articles.save(a);
    }

    /** Article de type quelconque (olives, emballage, bouteille…), créé uniquement pour les tests. */
    public Article articleOf(String ref, ArticleType type, String unitCode, boolean lotTracked, String contenance) {
        Unit u = units.findAll().stream().filter(x -> x.getCode().equals(unitCode)).findFirst()
                .orElseGet(() -> units.save(new Unit(unitCode, unitCode)));
        Article a = new Article();
        a.setReference(ref);
        a.setNom("Article " + ref);
        a.setType(type);
        a.setUnite(u);
        a.setSuiviLot(lotTracked);
        a.setContenance(contenance == null ? null : new BigDecimal(contenance));
        return articles.save(a);
    }

    public Client client(String nom, String plafond) {
        Client c = new Client();
        c.setCode("CLI-T-" + nom);
        c.setType(ClientType.PARTICULIER);
        c.setNom(nom);
        c.setPlafondCredit(plafond == null ? null : new BigDecimal(plafond));
        return clients.save(c);
    }

    public StockLot lot(Article a, String numero, LotOwner owner, Client client) {
        return lots.save(new StockLot(a, numero, owner, client));
    }

    /** Stock initial via le vrai service (même chemin que l'application). */
    public void receive(Article a, StockLot lot, String qty) {
        tx.executeWithoutResult(s -> stock.apply(a, lot, MovementType.STOCK_INITIAL, new BigDecimal(qty),
                "test", "TEST", null));
    }
}
