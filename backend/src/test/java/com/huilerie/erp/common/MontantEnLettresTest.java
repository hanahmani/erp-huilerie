package com.huilerie.erp.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MontantEnLettresTest {

    private static String tnd(String v) {
        return MontantEnLettres.enLettres(new BigDecimal(v), Devise.TND);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0          | zéro",
            "1          | un",
            "16         | seize",
            "17         | dix-sept",
            "21         | vingt et un",
            "22         | vingt-deux",
            "70         | soixante-dix",
            "71         | soixante et onze",
            "72         | soixante-douze",
            "80         | quatre-vingts",
            "81         | quatre-vingt-un",
            "90         | quatre-vingt-dix",
            "91         | quatre-vingt-onze",
            "99         | quatre-vingt-dix-neuf",
            "100        | cent",
            "101        | cent un",
            "200        | deux cents",
            "201        | deux cent un",
            "280        | deux cent quatre-vingts",
            "1000       | mille",
            "1001       | mille un",
            "2000       | deux mille",
            "21000      | vingt et un mille",
            "80000      | quatre-vingt mille",
            "200000     | deux cent mille",
            "1000000    | un million",
            "2000000    | deux millions",
            "80000000   | quatre-vingts millions",
            "300000000  | trois cents millions",
            "1000000000 | un milliard",
            "1234567    | un million deux cent trente-quatre mille cinq cent soixante-sept",
    })
    void nombres(long n, String attendu) {
        assertThat(MontantEnLettres.nombre(n)).isEqualTo(attendu);
    }

    @Test
    void dinarsEtMillimes() {
        assertThat(tnd("1200.500")).isEqualTo("mille deux cents dinars et cinq cents millimes");
        assertThat(tnd("1")).isEqualTo("un dinar");
        assertThat(tnd("0")).isEqualTo("zéro dinar");
        assertThat(tnd("0.500")).isEqualTo("cinq cents millimes");
        assertThat(tnd("0.001")).isEqualTo("un millime");
        assertThat(tnd("2.080")).isEqualTo("deux dinars et quatre-vingts millimes");
        assertThat(tnd("1234567.891"))
                .isEqualTo("un million deux cent trente-quatre mille cinq cent soixante-sept dinars et huit cent quatre-vingt-onze millimes");
    }

    @Test
    void deDevantLaDeviseApresUnNombreRondDeMillions() {
        assertThat(tnd("2000000")).isEqualTo("deux millions de dinars");
        assertThat(tnd("1000000.250")).isEqualTo("un million de dinars et deux cent cinquante millimes");
        assertThat(tnd("2000100")).isEqualTo("deux millions cent dinars");
    }

    @Test
    void eurosEtCentimes() {
        assertThat(MontantEnLettres.enLettres(new BigDecimal("10"), Devise.EUR)).isEqualTo("dix euros");
        assertThat(MontantEnLettres.enLettres(new BigDecimal("10.50"), Devise.EUR)).isEqualTo("dix euros et cinquante centimes");
        assertThat(MontantEnLettres.enLettres(new BigDecimal("1.01"), Devise.EUR)).isEqualTo("un euro et un centime");
        // arrondi à 2 décimales
        assertThat(MontantEnLettres.enLettres(new BigDecimal("1.005"), Devise.EUR)).isEqualTo("un euro et un centime");
    }

    @Test
    void dollarsEtCents() {
        assertThat(MontantEnLettres.enLettres(new BigDecimal("5.25"), Devise.USD)).isEqualTo("cinq dollars et vingt-cinq cents");
        assertThat(MontantEnLettres.enLettres(new BigDecimal("99.999"), Devise.USD)).isEqualTo("cent dollars");
    }

    @Test
    void refusLesMontantsInvalides() {
        assertThatThrownBy(() -> tnd("-1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tnd("1000000000000")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MontantEnLettres.enLettres(null, Devise.TND)).isInstanceOf(IllegalArgumentException.class);
    }
}
