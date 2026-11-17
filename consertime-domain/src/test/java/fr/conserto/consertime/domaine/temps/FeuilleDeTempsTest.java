package fr.conserto.consertime.domaine.temps;

import fr.conserto.consertime.domaine.affaires.Affectation;
import fr.conserto.consertime.domaine.affaires.Budget;
import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static fr.conserto.consertime.domaine.Fixtures.ALICE;
import static fr.conserto.consertime.domaine.Fixtures.BRUNO;
import static fr.conserto.consertime.domaine.Fixtures.CALENDRIER;
import static fr.conserto.consertime.domaine.Fixtures.EXTRANET;
import static fr.conserto.consertime.domaine.Fixtures.INTERCONTRAT;
import static fr.conserto.consertime.domaine.Fixtures.affectation;
import static fr.conserto.consertime.domaine.Fixtures.assertRegle;
import static fr.conserto.consertime.domaine.Fixtures.d;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Feuille de temps mensuelle")
class FeuilleDeTempsTest {

    private static final YearMonth MAI = YearMonth.of(2025, 5);

    private final Affectation extranet = affectation(ALICE, EXTRANET);
    private final Affectation inter = affectation(ALICE, INTERCONTRAT);

    private final FeuilleDeTemps feuille = new FeuilleDeTemps(ALICE, MAI, CALENDRIER);

    @Test
    void saisieSimple() {
        feuille.saisir(extranet, d("2025-05-02"), Jours.UN, "mise en prod");
        assertThat(feuille.totalDu(d("2025-05-02"))).isEqualTo(Jours.UN);
        assertThat(feuille.saisies()).hasSize(1);
        assertThat(feuille.saisies().get(0).commentaireEventuel()).contains("mise en prod");
    }

    @Test
    @DisplayName("Deux demi-journées sur deux projets le même jour")
    void deuxDemiJournees() {
        feuille.saisir(extranet, d("2025-05-02"), Jours.DEMI);
        feuille.saisir(inter, d("2025-05-02"), Jours.DEMI);
        assertThat(feuille.totalDu(d("2025-05-02"))).isEqualTo(Jours.UN);
        assertThat(feuille.projets()).containsExactly(EXTRANET, INTERCONTRAT);
    }

    @Test
    @DisplayName("Une nouvelle saisie du même projet le même jour remplace la précédente")
    void remplacement() {
        feuille.saisir(extranet, d("2025-05-05"), Jours.DEMI);
        feuille.saisir(extranet, d("2025-05-05"), Jours.UN);
        assertThat(feuille.saisies()).hasSize(1);
        assertThat(feuille.total()).isEqualTo(Jours.UN);
    }

    @ParameterizedTest(name = "{0} jour(s) refusé(s)")
    @CsvSource({"0", "0.25", "1.5", "-1", "0.7"})
    void quantiteInvalide(String jours) {
        assertRegle(CodeRegle.SAISIE_QUANTITE_INVALIDE, () -> feuille.saisir(extranet, d("2025-05-05"), Jours.of(jours)));
    }

    @Test
    void plusDUnJourParDateInterdit() {
        feuille.saisir(extranet, d("2025-05-05"), Jours.UN);
        assertRegle(CodeRegle.SAISIE_JOUR_DEPASSE, () -> feuille.saisir(inter, d("2025-05-05"), Jours.DEMI));
    }

    @Test
    void dateHorsDuMois() {
        assertRegle(CodeRegle.SAISIE_HORS_MOIS, () -> feuille.saisir(extranet, d("2025-06-02"), Jours.UN));
    }

    @Test
    void affectationDUnAutreCollaborateur() {
        assertRegle(CodeRegle.SAISIE_HORS_AFFECTATION, () -> feuille.saisir(affectation(BRUNO, EXTRANET), d("2025-05-05"), Jours.UN));
    }

    @Test
    void affectationFermee() {
        Affectation jusquAuDix = affectation(ALICE.matricule(), EXTRANET, Periode.du(d("2025-01-01"), d("2025-05-10")), Budget.aucun());
        feuille.saisir(jusquAuDix, d("2025-05-09"), Jours.UN);
        assertRegle(CodeRegle.SAISIE_HORS_AFFECTATION, () -> feuille.saisir(jusquAuDix, d("2025-05-12"), Jours.UN));
    }

    @Test
    void avantLaDateDEntree() {
        Collaborateur nouvelle = Collaborateur.builder().matricule(2001).nom("Nina", "Nouvelle").entreLe(d("2025-05-19")).build();
        FeuilleDeTemps sienne = new FeuilleDeTemps(nouvelle, MAI, CALENDRIER);
        assertRegle(CodeRegle.SAISIE_HORS_PRESENCE, () -> sienne.saisir(affectation(nouvelle, EXTRANET), d("2025-05-16"), Jours.UN));
    }

    @Test
    @DisplayName("Les saisies un jour non ouvré sont acceptées mais signalées")
    void saisieUnJourFerie() {
        feuille.saisir(extranet, d("2025-05-29"), Jours.UN);
        feuille.saisir(extranet, d("2025-05-30"), Jours.UN);
        assertThat(feuille.saisiesHorsJoursOuvres()).extracting(Saisie::date).containsExactly(d("2025-05-29"));
    }

    @Test
    @DisplayName("L'intégration de saisies est tout ou rien")
    void integrationToutOuRien() {
        feuille.saisir(extranet, d("2025-05-06"), Jours.UN);
        List<Saisie> conge = List.of(
                Saisie.de(d("2025-05-05"), TypeAbsence.CONGES_PAYES.projet(), Jours.UN),
                Saisie.de(d("2025-05-06"), TypeAbsence.CONGES_PAYES.projet(), Jours.UN));
        assertRegle(CodeRegle.SAISIE_JOUR_DEPASSE, () -> feuille.integrer(conge));
        assertThat(feuille.saisies()).hasSize(1);
    }

    @Nested
    @DisplayName("Contrôle avant export (jours ouvrés saisis)")
    class Completude {

        @Test
        @DisplayName("Mai 2025 entièrement saisi : 19 jours ouvrés")
        void moisComplet() {
            CALENDRIER.joursOuvres(Periode.mois(MAI)).forEach(jour -> feuille.saisir(extranet, jour, Jours.UN));
            ControleCompletude.Resultat resultat = ControleCompletude.verifier(feuille);
            assertThat(resultat.estComplet()).isTrue();
            assertThat(resultat.joursOuvresAttendus()).isEqualTo(19);
        }

        @Test
        @DisplayName("Une demi-journée manquante rend le mois incomplet (écart : le legacy comptait les lignes)")
        void demiJourneeManquante() {
            CALENDRIER.joursOuvres(Periode.mois(MAI)).forEach(jour -> feuille.saisir(extranet, jour, Jours.UN));
            feuille.saisir(extranet, d("2025-05-02"), Jours.DEMI);
            ControleCompletude.Resultat resultat = ControleCompletude.verifier(feuille);
            assertThat(resultat.estComplet()).isFalse();
            assertThat(resultat.joursIncomplets()).containsExactly(d("2025-05-02"));
            assertThat(resultat.messages()).contains("Nombre de jours saisis : 18");
        }

        @Test
        @DisplayName("Embauche le 19 mai : seuls les jours ouvrés à partir du 19 sont attendus")
        void embaucheEnCoursDeMois() {
            Collaborateur nouvelle = Collaborateur.builder().matricule(2001).nom("Nina", "Nouvelle").entreLe(d("2025-05-19")).build();
            FeuilleDeTemps sienne = new FeuilleDeTemps(nouvelle, MAI, CALENDRIER);
            Affectation a = affectation(nouvelle, EXTRANET);
            CALENDRIER.joursOuvres(Periode.du(d("2025-05-19"), d("2025-05-31"))).forEach(j -> sienne.saisir(a, j, Jours.UN));
            ControleCompletude.Resultat resultat = ControleCompletude.verifier(sienne);
            assertThat(resultat.joursOuvresAttendus()).isEqualTo(9);
            assertThat(resultat.estComplet()).isTrue();
        }

        @Test
        @DisplayName("Embauche puis départ dans le même mois : les deux bornes s'appliquent")
        void entreeEtSortieDansLeMois() {
            Collaborateur passage = Collaborateur.builder().matricule(2002).nom("Paul", "Passage")
                    .entreLe(d("2025-05-05")).sortLe(d("2025-05-16")).build();
            FeuilleDeTemps sienne = new FeuilleDeTemps(passage, MAI, CALENDRIER);
            assertThat(ControleCompletude.verifier(sienne).joursOuvresAttendus()).isEqualTo(9);
        }
    }

    @Test
    void joursCanoniques() {
        assertThat(Jours.of("1")).isEqualTo(Jours.UN);
        assertThat(Jours.of("0,50")).isEqualTo(Jours.DEMI);
        assertThat(Jours.of(2).toString()).isEqualTo("2.0");
        assertThat(Jours.of("1.5").estMultipleDeDemi()).isTrue();
        assertThat(Jours.of("0.3").estMultipleDeDemi()).isFalse();
    }

    @Test
    void parJourEtTotalParProjet() {
        feuille.saisir(extranet, d("2025-05-06"), Jours.UN);
        feuille.saisir(extranet, d("2025-05-05"), Jours.DEMI);
        feuille.saisir(inter, d("2025-05-05"), Jours.DEMI);
        assertThat(feuille.parJour().keySet().stream().toList()).containsExactly(d("2025-05-05"), d("2025-05-06"));
        assertThat(feuille.totalPour(EXTRANET)).isEqualTo(Jours.of("1.5"));
        LocalDate premier = feuille.saisies().get(0).date();
        assertThat(premier).isEqualTo(d("2025-05-05"));
    }
}
