package fr.conserto.consertime.domaine.documents;

import fr.conserto.consertime.domaine.affaires.Affectation;
import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.calendrier.StatutJour;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.temps.FeuilleDeTemps;
import fr.conserto.consertime.domaine.temps.Jours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static fr.conserto.consertime.domaine.Fixtures.ACME;
import static fr.conserto.consertime.domaine.Fixtures.ALICE;
import static fr.conserto.consertime.domaine.Fixtures.CALENDRIER;
import static fr.conserto.consertime.domaine.Fixtures.EXTRANET;
import static fr.conserto.consertime.domaine.Fixtures.INTERCONTRAT;
import static fr.conserto.consertime.domaine.Fixtures.affectation;
import static fr.conserto.consertime.domaine.Fixtures.assertRegle;
import static fr.conserto.consertime.domaine.Fixtures.d;
import static fr.conserto.consertime.domaine.Fixtures.horloge;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Documents mensuels : CRA, FDT, NDF")
class DocumentsTest {

    private static final YearMonth AVRIL = YearMonth.of(2025, 4);
    private final Clock edition = horloge("2025-05-02T09:00");
    private FeuilleDeTemps avril;

    /** Avril 2025 d'Alice, comme dans le golden master : ACME, intercontrat le 15, CP du 22 au 25. */
    @BeforeEach
    void avrilDAlice() {
        avril = new FeuilleDeTemps(ALICE, AVRIL, CALENDRIER);
        Affectation extranet = affectation(ALICE, EXTRANET);
        Affectation inter = affectation(ALICE, INTERCONTRAT);
        Affectation cp = affectation(ALICE, TypeAbsence.CONGES_PAYES.projet());
        for (LocalDate jour : CALENDRIER.joursOuvres(Periode.mois(AVRIL))) {
            if (jour.equals(d("2025-04-15"))) {
                avril.saisir(inter, jour, Jours.UN, "Formation interne");
            } else if (!jour.isBefore(d("2025-04-22")) && !jour.isAfter(d("2025-04-25"))) {
                avril.saisir(cp, jour, Jours.UN);
            } else {
                avril.saisir(extranet, jour, Jours.UN);
            }
        }
    }

    @Nested
    @DisplayName("Compte rendu d'activité")
    class Cra {

        @Test
        @DisplayName("Seuls les jours facturables sont imputés : 21 jours ouvrés - 1 intercontrat - 4 CP = 16")
        void joursFactures() {
            CompteRenduActivite cra = CompteRenduActivite.etablir(avril, true, edition);
            assertThat(cra.joursFactures()).isEqualTo(Jours.of("16"));
            assertThat(cra.client()).contains(ACME);
            assertThat(cra.nom()).isEqualTo("Martin");
            assertThat(cra.dateEdition()).isEqualTo(d("2025-05-02"));
        }

        @Test
        @DisplayName("Une ligne par jour du mois, week-ends et fériés repérés")
        void lignes() {
            CompteRenduActivite cra = CompteRenduActivite.etablir(avril, true, edition);
            assertThat(cra.lignes()).hasSize(30);
            CompteRenduActivite.Ligne lundiDePaques = cra.lignes().get(20);
            assertThat(lundiDePaques.date()).isEqualTo(d("2025-04-21"));
            assertThat(lundiDePaques.statut()).isEqualTo(StatutJour.FERIE);
            assertThat(cra.lignes().get(4).statut()).isEqualTo(StatutJour.WEEK_END);
            assertThat(cra.lignes().get(0).libelleImputations()).isEqualTo("(1.0) - Refonte extranet");
            assertThat(cra.lignes().get(14).imputations()).isEmpty();
        }

        @Test
        @DisplayName("Deux projets facturables le même jour, triés par code projet : « (0.5) - A / (0.5) - B »")
        void deuxProjetsLeMemeJour() {
            FeuilleDeTemps mai = new FeuilleDeTemps(ALICE, YearMonth.of(2025, 5), CALENDRIER);
            mai.saisir(affectation(ALICE, fr.conserto.consertime.domaine.Fixtures.CORE_BANKING), d("2025-05-02"), Jours.DEMI);
            mai.saisir(affectation(ALICE, EXTRANET), d("2025-05-02"), Jours.DEMI);
            CompteRenduActivite cra = CompteRenduActivite.etablir(mai, false, edition);
            assertThat(cra.lignes().get(1).libelleImputations()).isEqualTo("(0.5) - Refonte extranet / (0.5) - Migration core banking");
        }

        @Test
        void moisIncompletRefuse() {
            FeuilleDeTemps mai = new FeuilleDeTemps(ALICE, YearMonth.of(2025, 5), CALENDRIER);
            mai.saisir(affectation(ALICE, EXTRANET), d("2025-05-02"), Jours.UN);
            assertRegle(CodeRegle.EXPORT_SAISIE_INCOMPLETE, () -> CompteRenduActivite.etablir(mai, true, edition));
            assertThat(CompteRenduActivite.etablir(mai, false, edition).joursFactures()).isEqualTo(Jours.UN);
        }

        @Test
        @DisplayName("Client = premier projet facturable, même si le mois commence par une absence (écart)")
        void clientDuPremierProjetFacturable() {
            FeuilleDeTemps juin = new FeuilleDeTemps(ALICE, YearMonth.of(2025, 6), CALENDRIER);
            juin.saisir(affectation(ALICE, TypeAbsence.RTT.projet()), d("2025-06-02"), Jours.UN);
            juin.saisir(affectation(ALICE, EXTRANET), d("2025-06-03"), Jours.UN);
            assertThat(CompteRenduActivite.etablir(juin, false, edition).client()).contains(ACME);
        }
    }

    @Nested
    @DisplayName("Feuille de temps")
    class Fdt {

        @Test
        @DisplayName("Une ligne par projet, absences comprises, et le total du mois")
        void lignesParProjet() {
            FicheDeTemps fdt = FicheDeTemps.etablir(avril, List.of(), true, edition);
            assertThat(fdt.lignes()).extracting(l -> l.projet().code()).containsExactly("ACME-EXT", "INTER", "CP");
            assertThat(fdt.lignes()).extracting(FicheDeTemps.Ligne::total)
                    .containsExactly(Jours.of("16"), Jours.UN, Jours.of("4"));
            assertThat(fdt.totalMois()).isEqualTo(Jours.of("21"));
            assertThat(fdt.colonnes()).hasSize(30);
            assertThat(fdt.matricule()).isEqualTo(ALICE.matricule());
        }

        @Test
        @DisplayName("Les demi-journées ne sont plus tronquées (écart : intValue() dans le legacy)")
        void demiJournees() {
            FeuilleDeTemps mai = new FeuilleDeTemps(ALICE, YearMonth.of(2025, 5), CALENDRIER);
            mai.saisir(affectation(ALICE, EXTRANET), d("2025-05-02"), Jours.DEMI);
            mai.saisir(affectation(ALICE, EXTRANET), d("2025-05-05"), Jours.UN);
            FicheDeTemps fdt = FicheDeTemps.etablir(mai, List.of(), false, edition);
            assertThat(fdt.lignes().get(0).total()).isEqualTo(Jours.of("1.5"));
            assertThat(fdt.lignes().get(0).valeurLe(d("2025-05-02"))).isEqualTo(Jours.DEMI);
            assertThat(fdt.lignes().get(0).valeurLe(d("2025-05-03"))).isEqualTo(Jours.ZERO);
        }

        @Test
        @DisplayName("Les projets demandés apparaissent même sans saisie, en tête")
        void projetsAffiches() {
            FicheDeTemps fdt = FicheDeTemps.etablir(avril, List.of(TypeAbsence.RTT.projet()), true, edition);
            assertThat(fdt.lignes()).extracting(l -> l.projet().code()).containsExactly("RTT", "ACME-EXT", "INTER", "CP");
            assertThat(fdt.lignes().get(0).total()).isEqualTo(Jours.ZERO);
        }
    }

    @Nested
    @DisplayName("Note de frais")
    class Ndf {

        private NoteDeFrais note() {
            return NoteDeFrais.ouvrir(ALICE, AVRIL, "ACME Assurances", "Digital Nantes",
                    NoteDeFrais.BaremeKilometrique.de("0.603"), edition);
        }

        @Test
        @DisplayName("Indemnités kilométriques, HT = TTC - TVA, total à verser = indemnités + TTC")
        void totaux() {
            NoteDeFrais ndf = note()
                    .ajouter(NoteDeFrais.Ligne.deplacement(d("2025-04-03"), "Nantes - site client", "42"))
                    .ajouter(NoteDeFrais.Ligne.depense(d("2025-04-03"), "Déjeuner client", "36.30", "3.30"))
                    .ajouter(NoteDeFrais.Ligne.depense(d("2025-04-10"), "Train Nantes - Paris", "120.00", "10.91"));
            NoteDeFrais.Totaux t = ndf.totaux();
            assertThat(t.kilometres()).isEqualByComparingTo("42");
            assertThat(t.indemnites()).isEqualByComparingTo("25.33");
            assertThat(t.montantTtc()).isEqualByComparingTo("156.30");
            assertThat(t.montantHt()).isEqualByComparingTo("142.09");
            assertThat(t.totalAVerser()).isEqualByComparingTo("181.63");
            assertThat(ndf.dateSoumission()).isEqualTo(d("2025-05-02"));
        }

        @Test
        void ligneHorsDuMois() {
            assertRegle(CodeRegle.NDF_LIGNE_INVALIDE, () -> note().ajouter(NoteDeFrais.Ligne.deplacement(d("2025-05-02"), "x", "10")));
        }

        @Test
        void tvaSuperieureAuTtc() {
            assertRegle(CodeRegle.NDF_LIGNE_INVALIDE, () -> NoteDeFrais.Ligne.depense(d("2025-04-03"), "x", "10", "12"));
        }

        @Test
        void approbation() {
            assertThat(note().approuver(fr.conserto.consertime.domaine.Fixtures.RACHEL).approuveePar()).contains("Rachel Referent");
        }
    }
}
