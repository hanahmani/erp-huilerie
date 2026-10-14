package com.huilerie.erp.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUser> findAllByOrderByLastNameAscFirstNameAsc();

    @org.springframework.data.jpa.repository.Query(
            "select count(u) from AppUser u join u.roles r where r.code = :code and u.active = true")
    long countActiveWithRole(@org.springframework.data.repository.query.Param("code") String code);

    @org.springframework.data.jpa.repository.Query("select count(u) from AppUser u join u.roles r where r.id = :roleId")
    long countWithRole(@org.springframework.data.repository.query.Param("roleId") Long roleId);
}
