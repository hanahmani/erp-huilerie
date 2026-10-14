package com.huilerie.erp.purchases;

import com.huilerie.erp.inventory.StockLot;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @Column(name = "date_recep", nullable = false)
    private Instant date;

    @Column(nullable = false, length = 64)
    private String username;

    private String notes;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Line> lignes = new ArrayList<>();

    protected GoodsReceipt() {
    }

    GoodsReceipt(String numero, Purchase purchase, String username, String notes) {
        this.numero = numero;
        this.purchase = purchase;
        this.username = username;
        this.notes = notes;
        this.date = Instant.now();
    }

    void addLine(PurchaseLine line, StockLot lot, BigDecimal q) {
        lignes.add(new Line(this, line, lot, q));
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Instant getDate() { return date; }
    public String getUsername() { return username; }
    public String getNotes() { return notes; }
    public List<Line> getLignes() { return lignes; }

    @Entity(name = "GoodsReceiptLine")
    @Table(name = "goods_receipt_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "receipt_id", nullable = false)
        private GoodsReceipt receipt;

        @ManyToOne(optional = false)
        @JoinColumn(name = "purchase_line_id", nullable = false)
        private PurchaseLine purchaseLine;

        @ManyToOne
        @JoinColumn(name = "lot_id")
        private StockLot lot;

        @Column(nullable = false, precision = 15, scale = 3)
        private BigDecimal quantite;

        protected Line() {
        }

        Line(GoodsReceipt receipt, PurchaseLine purchaseLine, StockLot lot, BigDecimal quantite) {
            this.receipt = receipt;
            this.purchaseLine = purchaseLine;
            this.lot = lot;
            this.quantite = quantite;
        }

        public PurchaseLine getPurchaseLine() { return purchaseLine; }
        public StockLot getLot() { return lot; }
        public BigDecimal getQuantite() { return quantite; }
    }
}
