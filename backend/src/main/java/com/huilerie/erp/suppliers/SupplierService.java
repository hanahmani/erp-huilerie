package com.huilerie.erp.suppliers;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.suppliers.dto.SupplierDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository suppliers;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public SupplierService(SupplierRepository suppliers, DocumentNumberService numbers, AuditService audit) {
        this.suppliers = suppliers;
        this.numbers = numbers;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<SupplierDto.Response> search(String q, SupplierType type, Boolean actif, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("nom"));
        return PageResponse.of(suppliers.search(Text.search(q), type, actif, pr), SupplierDto.Response::from);
    }

    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    @Transactional(readOnly = true)
    public SupplierDto.Response get(Long id) {
        return SupplierDto.Response.from(supplier(id));
    }

    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    @Transactional
    public SupplierDto.Response save(Long id, SupplierDto.Request r) {
        String cin = Text.clean(r.cin());
        String mf = Text.clean(r.matriculeFiscal());
        if (r.type() == SupplierType.AGRICULTEUR && cin == null && mf == null) {
            throw new BusinessException("Indiquez le CIN ou le matricule fiscal de l'agriculteur.");
        }
        if (r.type() == SupplierType.ENTREPRISE && mf == null) {
            throw new BusinessException("Le matricule fiscal est obligatoire pour une entreprise.");
        }
        Supplier s;
        if (id == null) {
            s = new Supplier();
            s.setCode(numbers.nextCode("FOURNISSEUR", "FRN"));
        } else {
            s = supplier(id);
        }
        s.setType(r.type());
        s.setNom(r.nom().trim());
        s.setTelephone(Text.clean(r.telephone()));
        s.setAdresse(Text.clean(r.adresse()));
        s.setCin(cin);
        s.setMatriculeFiscal(mf);
        s.setEmail(Text.clean(r.email()));
        s.setConditionsPaiement(Text.clean(r.conditionsPaiement()));
        s.setBanque(Text.clean(r.banque()));
        s.setRib(Text.clean(r.rib()) == null ? null : r.rib().replace(" ", ""));
        s = suppliers.save(s);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "Supplier", String.valueOf(s.getId()), s.getCode());
        return SupplierDto.Response.from(s);
    }

    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE')")
    @Transactional
    public SupplierDto.Response setStatus(Long id, boolean actif) {
        Supplier s = supplier(id);
        s.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "ACTIVATE" : "DEACTIVATE", "Supplier", String.valueOf(id), null);
        return SupplierDto.Response.from(s);
    }

    private Supplier supplier(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Fournisseur introuvable."));
    }
}
