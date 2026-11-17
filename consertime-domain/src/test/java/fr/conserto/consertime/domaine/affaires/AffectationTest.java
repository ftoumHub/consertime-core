package fr.conserto.consertime.domaine.affaires;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static fr.conserto.consertime.domaine.Fixtures.ALICE;
import static fr.conserto.consertime.domaine.Fixtures.EXTRANET;
import static fr.conserto.consertime.domaine.Fixtures.affectation;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Budgets d'affectation")
class AffectationTest {

    @ParameterizedTest(name = "budget fixe de 5 j, {0} j consommés -> {1}")
    @CsvSource({
            "0,   DANS_LE_BUDGET",
            "4.5, DANS_LE_BUDGET",
            "5,   BUDGET_ATTEINT",
            "5.5, BUDGET_DEPASSE"
    })
    void budgetFixe(BigDecimal consomme, StatutBudget attendu) {
        Affectation a = affectation(ALICE.matricule(), EXTRANET, null, Budget.fixe("5"));
        assertThat(a.evaluer(consomme)).isEqualTo(attendu);
    }

    @ParameterizedTest(name = "budget flexible 10 j + 2 j, {0} j consommés -> {1}")
    @CsvSource({
            "9.5,  DANS_LE_BUDGET",
            "10,   DEPASSEMENT_EN_COURS",
            "11.5, DEPASSEMENT_EN_COURS",
            "12,   DEPASSEMENT_MAXIMAL_ATTEINT",
            "12.5, BUDGET_DEPASSE"
    })
    void budgetFlexible(BigDecimal consomme, StatutBudget attendu) {
        Affectation a = affectation(ALICE.matricule(), EXTRANET, null, Budget.flexible("10", "2"));
        assertThat(a.evaluer(consomme)).isEqualTo(attendu);
    }

    @Test
    void sansBudget() {
        assertThat(affectation(ALICE, EXTRANET).evaluer(new BigDecimal("400"))).isEqualTo(StatutBudget.DANS_LE_BUDGET);
    }

    @Test
    @DisplayName("Le chef de projet est prévenu quand le statut change et reste acceptable")
    void notificationChefDeProjet() {
        Affectation a = affectation(ALICE.matricule(), EXTRANET, null, Budget.fixe("5"));
        assertThat(a.doitPrevenirChefDeProjet(StatutBudget.DANS_LE_BUDGET, StatutBudget.BUDGET_ATTEINT)).isTrue();
        assertThat(a.doitPrevenirChefDeProjet(StatutBudget.DANS_LE_BUDGET, StatutBudget.DANS_LE_BUDGET)).isFalse();
        assertThat(a.doitPrevenirChefDeProjet(StatutBudget.BUDGET_ATTEINT, StatutBudget.BUDGET_DEPASSE)).isFalse();
        assertThat(StatutBudget.BUDGET_DEPASSE.estAcceptable()).isFalse();
    }
}
