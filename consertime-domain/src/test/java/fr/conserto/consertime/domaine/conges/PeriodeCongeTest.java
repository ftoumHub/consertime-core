package fr.conserto.consertime.domaine.conges;

import fr.conserto.consertime.domaine.calendrier.DemiJournee;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.temps.Jours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static fr.conserto.consertime.domaine.Fixtures.CALENDRIER;
import static fr.conserto.consertime.domaine.Fixtures.assertRegle;
import static fr.conserto.consertime.domaine.Fixtures.d;
import static fr.conserto.consertime.domaine.calendrier.DemiJournee.APRES_MIDI;
import static fr.conserto.consertime.domaine.calendrier.DemiJournee.MATIN;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Durée d'un congé à la demi-journée")
class PeriodeCongeTest {

    @ParameterizedTest(name = "{0} {1} -> {2} {3} = {4} jour(s)")
    @CsvSource({
            // une journée, demi-journées
            "2025-06-16, MATIN,      2025-06-16, APRES_MIDI, 1",
            "2025-06-16, APRES_MIDI, 2025-06-16, APRES_MIDI, 0.5",
            "2025-06-16, MATIN,      2025-06-16, MATIN,      0.5",
            // semaine complète : 5 jours (le legacy en comptait 4, fin exclue)
            "2025-06-16, MATIN,      2025-06-20, APRES_MIDI, 5",
            // lundi après-midi -> mardi matin : 1 jour (le legacy comptait 0 et refusait la demande)
            "2025-06-16, APRES_MIDI, 2025-06-17, MATIN,      1",
            // vendredi après-midi -> lundi matin, week-end ignoré
            "2025-06-13, APRES_MIDI, 2025-06-16, MATIN,      1",
            // semaine du lundi de Pentecôte (9 juin) : 4 jours
            "2025-06-09, MATIN,      2025-06-13, APRES_MIDI, 4",
            // pont de l'Ascension : jeudi férié, vendredi posé
            "2025-05-29, MATIN,      2025-05-30, APRES_MIDI, 1",
            // à cheval sur deux mois (28 juillet - 8 août 2025)
            "2025-07-28, MATIN,      2025-08-08, APRES_MIDI, 10",
            // samedi -> dimanche : aucun jour ouvré
            "2025-06-14, MATIN,      2025-06-15, APRES_MIDI, 0"
    })
    void nombreDeJours(LocalDate debut, DemiJournee debutA, LocalDate fin, DemiJournee finA, String attendu) {
        assertThat(new PeriodeConge(debut, debutA, fin, finA).nombreDeJours(CALENDRIER))
                .isEqualTo(Jours.of(attendu));
    }

    @Test
    @DisplayName("Le décompte jour par jour place les demi-journées aux extrémités")
    void decompteParJour() {
        PeriodeConge periode = new PeriodeConge(d("2025-06-11"), APRES_MIDI, d("2025-06-13"), MATIN);
        assertThat(periode.decompte(CALENDRIER))
                .containsEntry(d("2025-06-11"), Jours.DEMI)
                .containsEntry(d("2025-06-12"), Jours.UN)
                .containsEntry(d("2025-06-13"), Jours.DEMI)
                .hasSize(3);
    }

    @Test
    @DisplayName("Un même jour, l'après-midi ne peut pas finir le matin")
    void apresMidiPuisMatinLeMemeJour() {
        assertRegle(CodeRegle.CONGE_DATES_INVALIDES, () -> new PeriodeConge(d("2025-06-16"), APRES_MIDI, d("2025-06-16"), MATIN));
    }

    @Test
    void finAvantDebut() {
        assertRegle(CodeRegle.CONGE_DATES_INVALIDES, () -> PeriodeConge.journeesEntieres(d("2025-06-20"), d("2025-06-16")));
    }

    @ParameterizedTest(name = "{0} {1}-{2} {3} chevauche le 16 juin matin ? {4}")
    @CsvSource({
            "2025-06-16, MATIN,      2025-06-16, MATIN,      true",
            "2025-06-16, APRES_MIDI, 2025-06-16, APRES_MIDI, false",
            "2025-06-13, MATIN,      2025-06-16, MATIN,      true",
            "2025-06-17, MATIN,      2025-06-18, APRES_MIDI, false"
    })
    void chevauchementALaDemiJournee(LocalDate debut, DemiJournee debutA, LocalDate fin, DemiJournee finA, boolean attendu) {
        PeriodeConge matinDu16 = PeriodeConge.demiJournee(d("2025-06-16"), MATIN);
        assertThat(matinDu16.chevauche(new PeriodeConge(debut, debutA, fin, finA))).isEqualTo(attendu);
    }
}
