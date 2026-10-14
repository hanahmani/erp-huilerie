package com.huilerie.erp.suppliers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    long countByActifTrue();

    @Query("""
            select s from Supplier s
            where (:q is null or lower(s.nom) like concat('%', :q, '%')
                   or lower(s.code) like concat('%', :q, '%')
                   or s.telephone like concat('%', :q, '%')
                   or lower(s.cin) like concat('%', :q, '%')
                   or lower(s.matriculeFiscal) like concat('%', :q, '%'))
              and (:type is null or s.type = :type)
              and (:actif is null or s.actif = :actif)
            """)
    Page<Supplier> search(@Param("q") String q, @Param("type") SupplierType type, @Param("actif") Boolean actif,
                          Pageable pageable);
}
