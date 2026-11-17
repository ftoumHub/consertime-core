package fr.conserto.consertime.domaine.droits;

import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.conges.DemandeConge;
import fr.conserto.consertime.domaine.conges.PeriodeConge;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.TypeContrat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

import static fr.conserto.consertime.domaine.Fixtures.ALICE;
import static fr.conserto.consertime.domaine.Fixtures.CALENDRIER;
import static fr.conserto.consertime.domaine.Fixtures.CHLOE;
import static fr.conserto.consertime.domaine.Fixtures.DIGITAL;
import static fr.conserto.consertime.domaine.Fixtures.RACHEL;
import static fr.conserto.consertime.domaine.Fixtures.d;
import static fr.conserto.consertime.domaine.Fixtures.horloge;
import static fr.conserto.consertime.domaine.Fixtures.horlogeLe;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Droits à congés payés selon la date d'embauche")
class CalculDroitsCongesPayesTest {

    private final CalculDroitsCongesPayes ouvrables =
            new CalculDroitsCongesPayes(PolitiqueCongesPayes.codeDuTravail(ModeDecompte.OUVRABLES), CALENDRIER);
    private final CalculDroitsCongesPayes ouvres =
            new CalculDroitsCongesPayes(PolitiqueCongesPayes.codeDuTravail(ModeDecompte.OUVRES), CALENDRIER);
    private final CalculDroitsCongesPayes syntec =
            new CalculDroitsCongesPayes(PolitiqueCongesPayes.syntec(ModeDecompte.OUVRES), CALENDRIER);

    private final PeriodeReference periode2025 = PeriodeReference.contenant(d("2025-01-15"), MonthDay.of(6, 1));

    private static Collaborateur embaucheLe(String date) {
        return Collaborateur.builder()
                .matricule(3000)
                .nom("Nina", "Nouvelle")
                .entreLe(LocalDate.parse(date))
                .build();
    }

    @Test
    void periodeDeReference() {
        assertThat(periode2025.debut()).isEqualTo(d("2024-06-01"));
        assertThat(periode2025.fin()).isEqualTo(d("2025-05-31"));
        assertThat(periode2025.periodeDePrise().debut()).isEqualTo(d("2025-06-01"));
        assertThat(PeriodeReference.contenant(d("2025-06-01"), MonthDay.of(6, 1)).debut()).isEqualTo(d("2025-06-01"));
        assertThat(periode2025.mois()).hasSize(12);
    }

    @Test
    @DisplayName("Année complète : 30 jours ouvrables ou 25 jours ouvrés")
    void anneeComplete() {
        assertThat(ouvrables.calculer(ALICE, periode2025, d("2025-06-01")).acquis()).isEqualByComparingTo("30");
        assertThat(ouvres.calculer(ALICE, periode2025, d("2025-06-01")).acquis()).isEqualByComparingTo("25");
    }

    @ParameterizedTest(name = "embauche le {0} : {1} j ouvrables bruts, {2} retenus ; {3} j ouvrés bruts, {4} retenus")
    @CsvSource({
            // mois d'embauche partiel au prorata (13 j ouvrables / 24 ; 11 j ouvrés / 20), arrondi supérieur à la clôture
            "2024-09-16, 21.35, 22, 17.81, 18",
            // embauche le 2 : 25 j ouvrables en septembre >= 24, le mois compte en entier (art. L3141-4)
            "2024-09-02, 22.50, 23, 18.75, 19",
            // embauche le 1er mars : 3 mois
            "2025-03-01, 7.50,  8,  6.25,  7",
            // embauche après la période : rien
            "2025-06-02, 0.00,  0,  0.00,  0"
    })
    void prorataSelonLaDateDEmbauche(String embauche, String ouvrablesBruts, String ouvrablesRetenus,
                                     String ouvresBruts, String ouvresRetenus) {
        Collaborateur c = embaucheLe(embauche);
        DroitsCongesPayes enOuvrables = ouvrables.calculer(c, periode2025, d("2025-06-02"));
        DroitsCongesPayes enOuvres = ouvres.calculer(c, periode2025, d("2025-06-02"));
        assertThat(enOuvrables.acquisBruts()).isEqualByComparingTo(ouvrablesBruts);
        assertThat(enOuvrables.acquis()).isEqualByComparingTo(ouvrablesRetenus);
        assertThat(enOuvres.acquisBruts()).isEqualByComparingTo(ouvresBruts);
        assertThat(enOuvres.acquis()).isEqualByComparingTo(ouvresRetenus);
        assertThat(enOuvres.periodeClose()).isTrue();
    }

    @Nested
    @DisplayName("Avec une horloge : acquisition en cours")
    class AcquisitionEnCours {

        private final Collaborateur nina = embaucheLe("2024-09-16");

        @Test
        @DisplayName("Au 15 décembre 2024 : septembre partiel, octobre et novembre ; décembre pas encore acquis")
        void miDecembre() {
            Clock miDecembre = horloge("2024-12-15T12:00");
            DroitsCongesPayes droits = ouvrables.enCours(nina, miDecembre);
            assertThat(droits.periodeClose()).isFalse();
            assertThat(droits.acquis()).isEqualByComparingTo("6.35");
            assertThat(ouvres.enCours(nina, miDecembre).acquis()).isEqualByComparingTo("5.31");
        }

        @Test
        @DisplayName("Le dernier jour du mois, le mois est acquis")
        void finDeMois() {
            assertThat(ouvrables.enCours(nina, horlogeLe(d("2024-12-30"))).acquis()).isEqualByComparingTo("6.35");
            assertThat(ouvrables.enCours(nina, horlogeLe(d("2024-12-31"))).acquis()).isEqualByComparingTo("8.85");
        }

        @Test
        @DisplayName("Le 1er juin, une nouvelle période commence : les droits de l'année passée sont à prendre")
        void bascule1erJuin() {
            Clock premierJuin = horlogeLe(d("2025-06-01"));
            assertThat(ouvrables.enCours(nina, premierJuin).acquis()).isEqualByComparingTo("0");
            DroitsCongesPayes aPrendre = ouvrables.aPrendre(nina, premierJuin);
            assertThat(aPrendre.periodeClose()).isTrue();
            assertThat(aPrendre.acquis()).isEqualByComparingTo("22");
        }
    }

    @Test
    @DisplayName("Départ en cours de mois : le mois du départ est acquis au prorata et la période est close")
    void departEnCoursDeMois() {
        Collaborateur partant = Collaborateur.builder().matricule(3001).nom("Bruno", "Petit").entreLe(d("2021-03-01"))
                .sortLe(d("2025-03-14")).contrat(TypeContrat.MODALITE_2).build();
        DroitsCongesPayes droits = ouvrables.calculer(partant, periode2025, d("2025-03-20"));
        assertThat(droits.acquisBruts()).isEqualByComparingTo("23.75");
        assertThat(droits.periodeClose()).isTrue();
        assertThat(droits.acquis()).isEqualByComparingTo("24");
    }

    @Test
    void stagiaireSansCongesPayes() {
        Collaborateur stagiaire = Collaborateur.builder().matricule(3002).nom("Sam", "Stage").entreLe(d("2024-06-01"))
                .contrat(TypeContrat.STAGIAIRE).build();
        assertThat(ouvrables.calculer(stagiaire, periode2025, d("2025-06-01")).total()).isEqualByComparingTo("0");
    }

    @ParameterizedTest(name = "ancienneté au 1er juin 2025 avec une embauche le {0} : +{1} j")
    @CsvSource({
            "2020-06-02, 0",
            "2020-06-01, 1",
            "2015-01-05, 2",
            "2010-06-01, 3",
            "2000-01-03, 4"
    })
    @DisplayName("Congés d'ancienneté Syntec (paliers à confirmer)")
    void congesAnciennete(String embauche, int supplement) {
        DroitsCongesPayes droits = syntec.calculer(embaucheLe(embauche), periode2025, d("2025-06-01"));
        assertThat(droits.joursAnciennete()).isEqualTo(supplement);
        assertThat(droits.total()).isEqualByComparingTo(String.valueOf(25 + supplement));
    }

    @Test
    @DisplayName("Solde à prendre : droits de l'année passée moins les CP validés de la période de prise")
    void solde() {
        Clock depot = horloge("2025-05-05T10:00");
        List<DemandeConge> demandes = new ArrayList<>();
        DemandeConge juin = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                PeriodeConge.journeesEntieres(d("2025-06-16"), d("2025-06-20")), "", demandes, CALENDRIER, depot);
        demandes.add(juin);
        DemandeConge ete = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                PeriodeConge.journeesEntieres(d("2025-07-28"), d("2025-08-08")), "", demandes, CALENDRIER, depot);
        demandes.add(ete);
        DemandeConge rtt = DemandeConge.deposer(ALICE, TypeAbsence.RTT, PeriodeConge.journee(d("2025-06-23")), "",
                demandes, CALENDRIER, depot);
        demandes.add(rtt);
        DemandeConge mai = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES, PeriodeConge.journee(d("2025-05-30")), "",
                demandes, CALENDRIER, depot);
        demandes.add(mai);
        DemandeConge enAttente = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                PeriodeConge.journeesEntieres(d("2025-10-27"), d("2025-10-31")), "", demandes, CALENDRIER, depot);
        demandes.add(enAttente);
        for (DemandeConge dc : List.of(juin, ete, rtt, mai)) {
            dc.valider(RACHEL, DIGITAL, CALENDRIER, depot);
        }

        Clock miJuillet = horloge("2025-07-15T09:00");
        SoldeCongesPayes enOuvres = ouvres.solde(ALICE, demandes, miJuillet);
        assertThat(enOuvres.droits().acquis()).isEqualByComparingTo("25");
        assertThat(enOuvres.joursPoses()).isEqualByComparingTo("15");
        assertThat(enOuvres.solde()).isEqualByComparingTo("10");

        SoldeCongesPayes enOuvrables = ouvrables.solde(ALICE, demandes, miJuillet);
        // le samedi précédant chaque reprise est décompté : juin 5 + 1, été 11 + 1
        assertThat(enOuvrables.joursPoses()).isEqualByComparingTo("18");
        assertThat(enOuvrables.solde()).isEqualByComparingTo("12");
        assertThat(enOuvrables.estDebiteur()).isFalse();
    }

    @Test
    void chloeAlternanteAcquiertDesConges() {
        assertThat(CHLOE.typeContrat().acquiertCongesPayes()).isTrue();
        assertThat(ouvres.calculer(CHLOE, periode2025, d("2025-06-01")).acquis()).isEqualByComparingTo("19");
    }
}
