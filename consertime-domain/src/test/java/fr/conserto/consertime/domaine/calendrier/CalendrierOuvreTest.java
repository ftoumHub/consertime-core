package fr.conserto.consertime.domaine.calendrier;

import fr.conserto.consertime.domaine.regle.CodeRegle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.YearMonth;

import static fr.conserto.consertime.domaine.Fixtures.CALENDRIER;
import static fr.conserto.consertime.domaine.Fixtures.assertRegle;
import static fr.conserto.consertime.domaine.Fixtures.d;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Jours ouvrés, jours ouvrables et périodes")
class CalendrierOuvreTest {

    @ParameterizedTest(name = "{0} : {1} jours ouvrés, {2} jours ouvrables")
    @CsvSource({
            "2025-04, 21, 25",
            "2025-05, 19, 24",
            "2025-06, 20, 24",
            "2025-12, 22, 26",
            "2026-02, 20, 24"
    })
    void joursParMois(YearMonth mois, long ouvres, long ouvrables) {
        Periode periode = Periode.mois(mois);
        assertThat(CALENDRIER.nombreJoursOuvres(periode)).isEqualTo(ouvres);
        assertThat(CALENDRIER.nombreJoursOuvrables(periode)).isEqualTo(ouvrables);
    }

    @ParameterizedTest(name = "{0} est {1}")
    @CsvSource({
            "2025-05-14, OUVRE",
            "2025-05-17, WEEK_END",
            "2025-05-18, WEEK_END",
            "2025-05-29, FERIE",
            "2026-08-15, FERIE"
    })
    void statutDuJour(LocalDate jour, StatutJour attendu) {
        assertThat(CALENDRIER.statut(jour)).isEqualTo(attendu);
    }

    @Test
    void samediEstOuvrableMaisPasOuvre() {
        assertThat(CALENDRIER.estOuvre(d("2025-05-17"))).isFalse();
        assertThat(CALENDRIER.estOuvrable(d("2025-05-17"))).isTrue();
        assertThat(CALENDRIER.estOuvrable(d("2025-05-18"))).isFalse();
    }

    @ParameterizedTest(name = "semaine ISO de {0} = {1}")
    @CsvSource({
            "2024-12-30, 1",
            "2025-01-01, 1",
            "2025-05-14, 20",
            "2026-12-31, 53",
            "2027-01-03, 53"
    })
    void numeroDeSemaine(LocalDate jour, int semaine) {
        assertThat(CalendrierOuvre.numeroSemaine(jour)).isEqualTo(semaine);
    }

    @Test
    @DisplayName("Une période inclut ses deux bornes")
    void periodeBornesIncluses() {
        Periode periode = Periode.du(d("2025-05-12"), d("2025-05-16"));
        assertThat(periode.nombreDeJours()).isEqualTo(5);
        assertThat(CALENDRIER.joursOuvres(periode)).hasSize(5);
        assertThat(periode.contient(d("2025-05-16"))).isTrue();
    }

    @Test
    void intersectionDePeriodes() {
        Periode mai = Periode.mois(YearMonth.of(2025, 5));

        assertThat(mai.intersection(Periode.du(d("2025-04-20"), d("2025-05-03"))))
                .contains(Periode.du(d("2025-05-01"), d("2025-05-03")));

        assertThat(mai.intersection(Periode.jour(d("2025-06-01")))).isEmpty();
    }

    @Test
    void finAvantDebutInterdite() {
        assertRegle(CodeRegle.CAL_PERIODE_INVALIDE, () -> Periode.du(d("2025-05-16"), d("2025-05-12")));
    }
}
