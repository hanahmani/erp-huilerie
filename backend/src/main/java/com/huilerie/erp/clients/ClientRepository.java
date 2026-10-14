package com.huilerie.erp.clients;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {

    long countByActifTrue();

    @Query("""
            select c from Client c
            where (:q is null or lower(c.nom) like concat('%', :q, '%')
                   or lower(c.code) like concat('%', :q, '%')
                   or c.telephone like concat('%', :q, '%')
                   or lower(c.matriculeFiscal) like concat('%', :q, '%'))
              and (:type is null or c.type = :type)
              and (:actif is null or c.actif = :actif)
            """)
    Page<Client> search(@Param("q") String q, @Param("type") ClientType type, @Param("actif") Boolean actif,
                        Pageable pageable);
}
