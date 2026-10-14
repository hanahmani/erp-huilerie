package com.huilerie.erp.finance;

import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.finance.dto.CashDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class FinanceService {

    private final CashMovementRepository cash;

    public FinanceService(CashMovementRepository cash) {
        this.cash = cash;
    }

    /** Appelé par les documents (vente, annulation…) dans leur propre transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(CashMovementType type, PaymentMode mode, BigDecimal montant, String libelle,
                       String referenceType, Long referenceId) {
        cash.save(new CashMovement(type, mode, montant, libelle, referenceType, referenceId, SecurityUtils.username()));
    }

    @PreAuthorize("hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public CashDto.Journal journal(Instant from, Instant to, CashMovementType type, PaymentMode mode, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        BigDecimal in = Money.of(cash.total(CashMovementType.ENCAISSEMENT, from, to, mode));
        BigDecimal out = Money.of(cash.total(CashMovementType.DECAISSEMENT, from, to, mode));
        BigDecimal cashIn = Money.of(cash.total(CashMovementType.ENCAISSEMENT, from, to, PaymentMode.ESPECES));
        BigDecimal cashOut = Money.of(cash.total(CashMovementType.DECAISSEMENT, from, to, PaymentMode.ESPECES));
        return new CashDto.Journal(PageResponse.of(cash.search(from, to, type, mode, pr), CashDto.Line::from),
                in, out, in.subtract(out), cashIn.subtract(cashOut));
    }
}
