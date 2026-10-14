package com.huilerie.erp.sales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientPaymentRepository extends JpaRepository<ClientPayment, Long> {
    List<ClientPayment> findBySaleIdOrderByIdAsc(Long saleId);
}
