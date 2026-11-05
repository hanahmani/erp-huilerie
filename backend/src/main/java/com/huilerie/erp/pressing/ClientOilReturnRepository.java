package com.huilerie.erp.pressing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientOilReturnRepository extends JpaRepository<ClientOilReturn, Long> {

    List<ClientOilReturn> findByClientIdOrderByIdAsc(Long clientId);
}
