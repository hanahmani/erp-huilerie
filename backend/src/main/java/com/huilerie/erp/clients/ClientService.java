package com.huilerie.erp.clients;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.clients.dto.ClientDto;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.sales.SaleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clients;
    private final SaleRepository sales;
    private final DocumentNumberService numbers;
    private final AuditService audit;
    private com.huilerie.erp.salesdocs.SalesDocumentRepository invoices;

    @org.springframework.beans.factory.annotation.Autowired
    void setInvoices(com.huilerie.erp.salesdocs.SalesDocumentRepository invoices) {
        this.invoices = invoices;
    }

    private java.math.BigDecimal due(Long clientId) {
        return Money.ofAny(sales.outstandingByClient(clientId)).add(Money.ofAny(invoices.outstandingByClient(clientId)));
    }

    public ClientService(ClientRepository clients, SaleRepository sales, DocumentNumberService numbers,
                         AuditService audit) {
        this.clients = clients;
        this.sales = sales;
        this.numbers = numbers;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<ClientDto.Response> search(String q, ClientType type, Boolean actif, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("nom"));
        return PageResponse.of(clients.search(Text.search(q), type, actif, pr),
                c -> ClientDto.Response.from(c, due(c.getId())));
    }

    @PreAuthorize("hasAuthority('CLIENT_VIEW')")
    @Transactional(readOnly = true)
    public ClientDto.Response get(Long id) {
        Client c = client(id);
        return ClientDto.Response.from(c, due(id));
    }

    @PreAuthorize("hasAuthority('CLIENT_MANAGE')")
    @Transactional
    public ClientDto.Response save(Long id, ClientDto.Request r) {
        if (r.type() == ClientType.PROFESSIONNEL && Text.clean(r.matriculeFiscal()) == null) {
            throw new BusinessException("Le matricule fiscal est obligatoire pour un client professionnel.");
        }
        Client c;
        if (id == null) {
            c = new Client();
            c.setCode(numbers.nextCode("CLIENT", "CLI"));
        } else {
            c = client(id);
        }
        c.setType(r.type());
        c.setNom(r.nom().trim());
        c.setTelephone(Text.clean(r.telephone()));
        c.setAdresse(Text.clean(r.adresse()));
        c.setCin(Text.clean(r.cin()));
        c.setMatriculeFiscal(Text.clean(r.matriculeFiscal()));
        c.setPlafondCredit(Money.strict(r.plafondCredit(), "Plafond de crédit"));
        c.setEmail(Text.clean(r.email()));
        c.setConditionsPaiement(Text.clean(r.conditionsPaiement()));
        c = clients.save(c);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "Client", String.valueOf(c.getId()), c.getCode());
        return ClientDto.Response.from(c, due(c.getId()));
    }

    @PreAuthorize("hasAuthority('CLIENT_MANAGE')")
    @Transactional
    public ClientDto.Response setStatus(Long id, boolean actif) {
        Client c = client(id);
        c.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "ACTIVATE" : "DEACTIVATE", "Client", String.valueOf(id), null);
        return ClientDto.Response.from(c, due(id));
    }

    public Client client(Long id) {
        return clients.findById(id).orElseThrow(() -> new NotFoundException("Client introuvable."));
    }
}
