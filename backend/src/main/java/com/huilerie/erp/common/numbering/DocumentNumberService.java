package com.huilerie.erp.common.numbering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.ZoneId;

/**
 * Numéros séquentiels sans trou. Doit être appelé DANS la transaction du document :
 * si le document échoue, le compteur est annulé avec lui.
 */
@Service
public class DocumentNumberService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final DocumentSequenceRepository repository;

    public DocumentNumberService(DocumentSequenceRepository repository) {
        this.repository = repository;
    }

    /** Numéro annuel, ex. TCK-2026-000042. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String nextYearly(String type, String prefix) {
        int year = Year.now(TUNIS).getValue();
        return String.format("%s-%d-%06d", prefix, year, take(type, year));
    }

    /** Code permanent, ex. CLI-000042. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String nextCode(String type, String prefix) {
        return String.format("%s-%06d", prefix, take(type, 0));
    }

    private long take(String type, int year) {
        DocumentSequence seq = repository.findByTypeAndAnnee(type, year).orElse(null);
        if (seq == null) {
            repository.saveAndFlush(new DocumentSequence(type, year));
            seq = repository.findByTypeAndAnnee(type, year).orElseThrow();
        }
        return seq.take();
    }
}
