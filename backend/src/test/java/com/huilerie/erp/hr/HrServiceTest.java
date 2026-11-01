package com.huilerie.erp.hr;

import com.huilerie.erp.TestUsers;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.CashMovementRepository;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.PaymentMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Employés, pointage Play / Stop et reste dû (journées × TJM − paiements). */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "rh", authorities = {"HR_VIEW", "HR_MANAGE"})
class HrServiceTest {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    @Autowired HrService service;
    @Autowired TestUsers testUsers;
    @Autowired CashMovementRepository cash;

    HrDto.EmployeeResponse emp;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        emp = service.saveEmployee(null, new HrDto.EmployeeRequest("Nour", "Jlassi", null, "98 000 111", null, new BigDecimal("100")));
    }

    /** Journée passée saisie à la main : il y a n jours, de 8 h à 16 h (heure de Tunis). */
    private HrDto.WorkDayLine journee(int joursAvant, boolean terminee) {
        LocalDate d = LocalDate.now(TUNIS).minusDays(joursAvant);
        Instant debut = d.atTime(LocalTime.of(8, 0)).atZone(TUNIS).toInstant();
        Instant fin = terminee ? d.atTime(LocalTime.of(16, 0)).atZone(TUNIS).toInstant() : null;
        return service.addWorkDay(new HrDto.WorkDayRequest(emp.id(), debut, fin));
    }

    private HrDto.EmployeeResponse payer(EmployeePayment.Type type, String montant) {
        return service.pay(emp.id(), new HrDto.PaymentRequest(type, PaymentMode.ESPECES, new BigDecimal(montant), null));
    }

    @Test
    void cinqJoursATjm100MoinsPaiementDe400DonneReste100() {
        assertThat(emp.matricule()).isEqualTo("EMP-000001");
        for (int i = 1; i <= 5; i++) {
            journee(i, true);
        }
        HrDto.EmployeeResponse r = payer(EmployeePayment.Type.TJM, "400");
        assertThat(r.jours()).isEqualTo(5);
        assertThat(r.gagne()).isEqualByComparingTo("500");
        assertThat(r.paye()).isEqualByComparingTo("400");
        assertThat(r.reste()).isEqualByComparingTo("100");
        assertThat(cash.total(CashMovementType.DECAISSEMENT, null, null, null)).isEqualByComparingTo("400");
    }

    @Test
    void playPuisStopAvecLHeureActuelle() {
        Instant avant = Instant.now().minus(1, ChronoUnit.SECONDS);
        HrDto.EmployeeResponse enCours = service.start(emp.id());
        assertThat(enCours.enCours()).isTrue();
        assertThat(enCours.aujourdhui().heureDebut()).isAfter(avant);
        assertThat(enCours.aujourdhui().heureFin()).isNull();
        assertThat(enCours.jours()).isEqualTo(1);
        assertThatThrownBy(() -> service.start(emp.id())).isInstanceOf(BusinessException.class);

        HrDto.EmployeeResponse fini = service.stop(emp.id());
        assertThat(fini.enCours()).isFalse();
        assertThat(fini.aujourdhui().heureFin()).isNotNull();
        assertThatThrownBy(() -> service.stop(emp.id())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.start(emp.id())).isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà pointé");
    }

    @Test
    void leTjmEstFigeSurChaqueJournee() {
        journee(2, true);
        service.saveEmployee(emp.id(), new HrDto.EmployeeRequest("Nour", "Jlassi", null, null, null, new BigDecimal("120")));
        HrDto.WorkDayLine j = journee(1, true);
        assertThat(j.tjm()).isEqualByComparingTo("120");
        assertThat(service.getEmployee(emp.id()).gagne()).isEqualByComparingTo("220");
    }

    @Test
    void correctionManuelleDesHeures() {
        HrDto.WorkDayLine j = journee(1, true);
        LocalDate d = j.jour();
        Instant debut = d.atTime(LocalTime.of(7, 30)).atZone(TUNIS).toInstant();
        Instant fin = d.atTime(LocalTime.of(17, 15)).atZone(TUNIS).toInstant();
        HrDto.WorkDayLine corrige = service.updateWorkDay(j.id(), new HrDto.WorkDayRequest(null, debut, fin));
        assertThat(corrige.minutes()).isEqualTo(585);
        assertThat(corrige.modifiePar()).isEqualTo("rh");

        assertThatThrownBy(() -> service.updateWorkDay(j.id(), new HrDto.WorkDayRequest(null, fin, debut)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("après");
        assertThatThrownBy(() -> service.updateWorkDay(j.id(), new HrDto.WorkDayRequest(null,
                Instant.now().plus(2, ChronoUnit.HOURS), null))).isInstanceOf(BusinessException.class).hasMessageContaining("futur");
        assertThatThrownBy(() -> journee(1, true)).isInstanceOf(BusinessException.class).hasMessageContaining("déjà une journée");
    }

    @Test
    void unPointageOublieBloqueLePlayEtLaDesactivation() {
        journee(1, false);
        assertThatThrownBy(() -> service.start(emp.id())).isInstanceOf(BusinessException.class).hasMessageContaining("n'est pas arrêté");
        assertThatThrownBy(() -> service.setStatus(emp.id(), false)).isInstanceOf(BusinessException.class);
    }

    @Test
    void employeInactifNePeutPasPointerMaisPeutEtrePaye() {
        service.setStatus(emp.id(), false);
        assertThatThrownBy(() -> service.start(emp.id())).isInstanceOf(BusinessException.class).hasMessageContaining("inactif");
        assertThat(payer(EmployeePayment.Type.SALAIRE, "50").paye()).isEqualByComparingTo("50");
    }

    @Test
    void avanceSuperieureAuGagneDonneUnResteNegatif() {
        journee(1, true);
        HrDto.EmployeeResponse r = payer(EmployeePayment.Type.AVANCE, "150");
        assertThat(r.reste()).isEqualByComparingTo("-50");
        assertThat(service.searchPayments(emp.id(), EmployeePayment.Type.AVANCE, null, null, 0, 10).content()).hasSize(1);
    }
}
