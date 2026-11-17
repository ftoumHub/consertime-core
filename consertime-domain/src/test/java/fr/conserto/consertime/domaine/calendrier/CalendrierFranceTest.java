package fr.conserto.consertime.domaine.calendrier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.conserto.consertime.domaine.Fixtures.d;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Jours fériés français")
class CalendrierFranceTest {

    private final CalendrierFrance metropole = CalendrierFrance.metropole();

    @ParameterizedTest(name = "Pâques {0} = {1}")
    @CsvSource({
            "2000, 2000-04-23",
            "2008, 2008-03-23",
            "2011, 2011-04-24",
            "2019, 2019-04-21",
            "2024, 2024-03-31",
            "2025, 2025-04-20",
            "2026, 2026-04-05",
            "2027, 2027-03-28",
            "2038, 2038-04-25",
            "2285, 2285-03-22"
    })
    void dimancheDePaques(int annee, LocalDate attendu) {
        assertThat(CalendrierFrance.dimancheDePaques(annee)).isEqualTo(attendu);
    }

    @Test
    @DisplayName("2025 : les 11 jours fériés du legacy, dans l'ordre")
    void joursFeries2025() {
        assertThat(metropole.joursFeries(2025)).extracting(JourFerie::date)
                .containsExactly(
                        d("2025-01-01"), d("2025-04-21"), d("2025-05-01"), d("2025-05-08"), d("2025-05-29"),
                        d("2025-06-09"), d("2025-07-14"), d("2025-08-15"), d("2025-11-01"), d("2025-11-11"),
                        d("2025-12-25"));
    }

    @Test
    void libelles() {
        assertThat(metropole.jourFerie(d("2025-05-29"))).isPresent()
                .contains(new JourFerie(d("2025-05-29"), "Ascension"));

        assertThat(metropole.jourFerie(d("2025-06-09"))
                .map(JourFerie::libelle))
                .contains("Lundi de Pentecôte");

        assertThat(metropole.jourFerie(d("2025-05-30"))).isEmpty();
    }

    @Test
    @DisplayName("Ascension et Pentecôte se déduisent de Pâques sur 2000-2040")
    void feriesMobilesCoherentsAvecPaques() {
        IntStream.rangeClosed(2000, 2040).forEach(annee -> {
            LocalDate paques = CalendrierFrance.dimancheDePaques(annee);
            assertThat(paques.getDayOfWeek().getValue()).as("Pâques %d un dimanche", annee).isEqualTo(7);
            assertThat(metropole.estFerie(paques.plusDays(1))).isTrue();
            assertThat(metropole.estFerie(paques.plusDays(39))).isTrue();
            assertThat(metropole.estFerie(paques.plusDays(50))).isTrue();
            assertThat(metropole.joursFeries(annee)).hasSize(11);
        });
    }

    @Nested
    @DisplayName("Compléments au legacy")
    class Complements {

        @Test
        @DisplayName("Alsace-Moselle : Vendredi saint et Saint-Étienne en plus")
        void alsaceMoselle() {
            CalendrierFrance alsace = CalendrierFrance.alsaceMoselle();
            assertThat(alsace.joursFeries(2025)).hasSize(13);
            assertThat(alsace.jourFerie(d("2025-04-18")).map(JourFerie::libelle)).contains("Vendredi saint");
            assertThat(alsace.estFerie(d("2025-12-26"))).isTrue();
            assertThat(metropole.estFerie(d("2025-12-26"))).isFalse();
        }

        @Test
        @DisplayName("Lundi de Pentecôte travaillé (journée de solidarité)")
        void pentecoteTravaillee() {
            CalendrierFrance solidarite = new CalendrierFrance(CalendrierFrance.Zone.METROPOLE, false);
            assertThat(solidarite.estFerie(d("2025-06-09"))).isFalse();
            assertThat(solidarite.joursFeries(2025)).hasSize(10);
        }
    }

    @Test
    @DisplayName("Jours fériés tombant un week-end en 2026 : 15 août et 1er novembre")
    void feriesLeWeekEnd2026() {
        List<LocalDate> weekEnds = metropole.joursFeries(2026).stream().map(JourFerie::date)
                .filter(CalendrierOuvre::estWeekEnd).collect(Collectors.toList());
        assertThat(weekEnds).containsExactly(d("2026-08-15"), d("2026-11-01"));
    }
}
