package com.huilerie.erp.hr;

import com.huilerie.erp.TestUsers;
import com.huilerie.erp.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "rh", authorities = {"HR_VIEW", "HR_MANAGE", "LEAVE_VALIDATE", "LEAVE_REQUEST"})
class HrServiceTest {

    @Autowired HrService service;
    @Autowired TestUsers testUsers;

    // lundi 2 novembre 2026 → vendredi 6 novembre 2026
    static final LocalDate MON = LocalDate.of(2026, 11, 2);

    HrDto.EmployeeResponse emp;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        emp = service.saveEmployee(null, new HrDto.EmployeeRequest("Trabelsi", "Sami", null, null, null, null, null, null, null,
                LocalDate.of(2026, 1, 1), "CDI", null, "ACTIF", new BigDecimal("10"), null));
    }

    @Test
    void joursOuvresHorsWeekEndEtFeries() {
        assertThat(service.workingDays(MON, MON.plusDays(6))).isEqualByComparingTo("5");
        service.addHoliday(new HrDto.HolidayRequest(MON.plusDays(1), "Jour férié"));
        assertThat(service.workingDays(MON, MON.plusDays(6))).isEqualByComparingTo("4");
    }

    @Test
    void demandeValideeDebiteLeSolde() {
        HrDto.Leave l = service.requestLeave(new HrDto.LeaveRequestBody(emp.id(), "CONGE_ANNUEL", MON, MON.plusDays(4), null));
        assertThat(l.jours()).isEqualByComparingTo("5");
        HrDto.Leave ok = service.decide(l.id(), new HrDto.DecisionRequest(true, null));
        assertThat(ok.statut()).isEqualTo("VALIDEE");
        assertThat(ok.soldeActuel()).isEqualByComparingTo("5");
    }

    @Test
    void chevauchementEtSoldeInsuffisantRefuses() {
        service.requestLeave(new HrDto.LeaveRequestBody(emp.id(), "CONGE_ANNUEL", MON, MON.plusDays(1), null));
        assertThatThrownBy(() -> service.requestLeave(new HrDto.LeaveRequestBody(emp.id(), "MALADIE", MON.plusDays(1), MON.plusDays(2), null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("chevauche");
        assertThatThrownBy(() -> service.requestLeave(new HrDto.LeaveRequestBody(emp.id(), "CONGE_ANNUEL", MON.plusDays(7), MON.plusDays(25), null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Solde");
    }

    @Test
    void refusExigeUnMotif() {
        HrDto.Leave l = service.requestLeave(new HrDto.LeaveRequestBody(emp.id(), "SANS_SOLDE", MON, MON, null));
        assertThatThrownBy(() -> service.decide(l.id(), new HrDto.DecisionRequest(false, null))).isInstanceOf(BusinessException.class);
        assertThat(service.decide(l.id(), new HrDto.DecisionRequest(false, "Pic de campagne")).statut()).isEqualTo("REFUSEE");
    }

    @Test
    void cddSansDateDeFinRefuse() {
        assertThatThrownBy(() -> service.saveEmployee(null, new HrDto.EmployeeRequest("A", "B", null, null, null, null, null, null, null,
                LocalDate.now(), "SAISONNIER", null, "ACTIF", BigDecimal.ZERO, null))).isInstanceOf(BusinessException.class);
    }
}
