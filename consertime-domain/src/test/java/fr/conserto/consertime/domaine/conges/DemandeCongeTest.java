package fr.conserto.consertime.domaine.conges;

import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.DemiJournee;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.Role;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.temps.FeuilleDeTemps;
import fr.conserto.consertime.domaine.temps.Jours;
import fr.conserto.consertime.domaine.temps.Saisie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static fr.conserto.consertime.domaine.Fixtures.ADMIN;
import static fr.conserto.consertime.domaine.Fixtures.ALICE;
import static fr.conserto.consertime.domaine.Fixtures.BRUNO;
import static fr.conserto.consertime.domaine.Fixtures.CALENDRIER;
import static fr.conserto.consertime.domaine.Fixtures.CHLOE;
import static fr.conserto.consertime.domaine.Fixtures.CLOUD;
import static fr.conserto.consertime.domaine.Fixtures.DIGITAL;
import static fr.conserto.consertime.domaine.Fixtures.EXTRANET;
import static fr.conserto.consertime.domaine.Fixtures.RACHEL;
import static fr.conserto.consertime.domaine.Fixtures.affectation;
import static fr.conserto.consertime.domaine.Fixtures.assertRegle;
import static fr.conserto.consertime.domaine.Fixtures.d;
import static fr.conserto.consertime.domaine.Fixtures.horloge;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Demande de congé")
class DemandeCongeTest {

    private final Clock cinqMai = horloge("2025-05-05T14:00");

    private DemandeConge rttDuPont() {
        return DemandeConge.deposer(ALICE, TypeAbsence.RTT, PeriodeConge.journee(d("2025-05-30")),
                "Pont de l'Ascension", List.of(), CALENDRIER, cinqMai);
    }

    @Nested
    @DisplayName("Dépôt")
    class Depot {

        @Test
        @DisplayName("La date de dépôt vient de l'horloge et le nombre de jours est calculé")
        void depot() {
            DemandeConge demande = rttDuPont();
            assertThat(demande.etat()).isEqualTo(EtatDemande.EN_ATTENTE);
            assertThat(demande.dateDepot()).isEqualTo(LocalDateTime.parse("2025-05-05T14:00"));
            assertThat(demande.nombreDeJours()).isEqualTo(Jours.UN);
            assertThat(demande.type()).contains(TypeAbsence.RTT);
        }

        @Test
        void periodeSansJourOuvre() {
            assertRegle(CodeRegle.CONGE_SANS_JOUR_OUVRE, () -> DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                    PeriodeConge.journee(d("2025-05-29")), "Ascension", List.of(), CALENDRIER, cinqMai));
        }

        @Test
        void projetQuiNEstPasUneAbsence() {
            assertRegle(CodeRegle.CONGE_PROJET_NON_ABSENCE, () -> DemandeConge.deposer(ALICE, EXTRANET,
                    PeriodeConge.journee(d("2025-06-02")), "?", List.of(), CALENDRIER, cinqMai));
        }

        @Test
        void avantLEmbauche() {
            Collaborateur nina = Collaborateur.builder().matricule(2001).nom("Nina", "Nouvelle").entreLe(d("2025-06-02")).build();
            assertRegle(CodeRegle.CONGE_HORS_PRESENCE, () -> DemandeConge.deposer(nina, TypeAbsence.RTT,
                    PeriodeConge.journee(d("2025-05-30")), "", List.of(), CALENDRIER, cinqMai));
        }

        @Test
        @DisplayName("Chevauchement avec une demande en attente ou validée (legacy : requêtes findDemandesForDates*)")
        void chevauchement() {
            DemandeConge existante = rttDuPont();
            assertRegle(CodeRegle.CONGE_CHEVAUCHEMENT, () -> DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                    PeriodeConge.journeesEntieres(d("2025-05-26"), d("2025-05-30")), "", List.of(existante), CALENDRIER, cinqMai));
        }

        @Test
        @DisplayName("Une demande refusée ne bloque pas la période")
        void demandeRefuseeNeBloquePas() {
            DemandeConge refusee = rttDuPont();
            refusee.refuser(RACHEL, DIGITAL, "Mise en production", cinqMai);
            DemandeConge nouvelle = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES, PeriodeConge.journee(d("2025-05-30")),
                    "", List.of(refusee), CALENDRIER, cinqMai);
            assertThat(nouvelle.etat()).isEqualTo(EtatDemande.EN_ATTENTE);
        }

        @Test
        @DisplayName("Matin et après-midi d'un même jour en deux demandes")
        void deuxDemiJourneesDistinctes() {
            DemandeConge matin = DemandeConge.deposer(ALICE, TypeAbsence.RTT, PeriodeConge.demiJournee(d("2025-06-02"), DemiJournee.MATIN),
                    "", List.of(), CALENDRIER, cinqMai);
            DemandeConge apresMidi = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                    PeriodeConge.demiJournee(d("2025-06-02"), DemiJournee.APRES_MIDI), "", List.of(matin), CALENDRIER, cinqMai);
            assertThat(apresMidi.nombreDeJours()).isEqualTo(Jours.DEMI);
        }

        @Test
        @DisplayName("Les demandes d'un autre collaborateur n'interfèrent pas")
        void autreCollaborateur() {
            DemandeConge deBruno = DemandeConge.deposer(BRUNO, TypeAbsence.RTT, PeriodeConge.journee(d("2025-05-30")), "",
                    List.of(), CALENDRIER, cinqMai);
            assertThat(DemandeConge.deposer(ALICE, TypeAbsence.RTT, PeriodeConge.journee(d("2025-05-30")), "",
                    List.of(deBruno), CALENDRIER, cinqMai).etat()).isEqualTo(EtatDemande.EN_ATTENTE);
        }
    }

    @Nested
    @DisplayName("Décision")
    class Decision {

        @Test
        @DisplayName("La référente du département valide : une saisie d'absence par jour ouvré")
        void validation() {
            DemandeConge demande = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                    new PeriodeConge(d("2025-06-16"), DemiJournee.APRES_MIDI, d("2025-06-18"), DemiJournee.MATIN),
                    "Déménagement", List.of(), CALENDRIER, cinqMai);
            List<Saisie> saisies = demande.valider(RACHEL, DIGITAL, CALENDRIER, horloge("2025-05-06T08:30"));

            assertThat(demande.etat()).isEqualTo(EtatDemande.VALIDEE);
            assertThat(demande.decideur()).contains(RACHEL.matricule());
            assertThat(demande.dateDecision()).contains(LocalDateTime.parse("2025-05-06T08:30"));
            assertThat(saisies).extracting(Saisie::jours).containsExactly(Jours.DEMI, Jours.UN, Jours.DEMI);
            assertThat(saisies).allSatisfy(s -> assertThat(s.projet()).isEqualTo(TypeAbsence.CONGES_PAYES.projet()));
        }

        @Test
        @DisplayName("Les saisies validées s'intègrent à la feuille de temps")
        void integrationDansLaFeuille() {
            DemandeConge demande = rttDuPont();
            FeuilleDeTemps mai = new FeuilleDeTemps(ALICE, YearMonth.of(2025, 5), CALENDRIER);
            mai.integrer(demande.valider(RACHEL, DIGITAL, CALENDRIER, cinqMai));
            assertThat(mai.totalDu(d("2025-05-30"))).isEqualTo(Jours.UN);
        }

        @Test
        @DisplayName("Validation atomique : si l'imputation échoue, la demande reste en attente")
        void validationAtomique() {
            DemandeConge demande = rttDuPont();
            FeuilleDeTemps mai = new FeuilleDeTemps(ALICE, YearMonth.of(2025, 5), CALENDRIER);
            mai.saisir(affectation(ALICE, EXTRANET), d("2025-05-30"), Jours.UN);

            assertRegle(CodeRegle.SAISIE_JOUR_DEPASSE,
                    () -> demande.valider(RACHEL, DIGITAL, CALENDRIER, cinqMai, mai::integrer));
            assertThat(demande.etat()).isEqualTo(EtatDemande.EN_ATTENTE);
            assertThat(demande.decideur()).isEmpty();
            assertThat(mai.totalDu(d("2025-05-30"))).isEqualTo(Jours.UN);
        }

        @Test
        @DisplayName("Un référent d'un autre département n'est pas habilité (écart SEC-015)")
        void referentDUnAutreDepartement() {
            DemandeConge deChloe = DemandeConge.deposer(CHLOE, TypeAbsence.CONGES_PAYES,
                    PeriodeConge.journeesEntieres(d("2025-06-02"), d("2025-06-06")), "", List.of(), CALENDRIER, cinqMai);
            assertRegle(CodeRegle.CONGE_DECIDEUR_NON_HABILITE, () -> deChloe.valider(RACHEL, CLOUD, CALENDRIER, cinqMai));
            assertThat(deChloe.estVisiblePar(RACHEL, CLOUD)).isFalse();
        }

        @Test
        void administrateurToujoursHabilite() {
            DemandeConge deChloe = DemandeConge.deposer(CHLOE, TypeAbsence.CONGES_PAYES,
                    PeriodeConge.journeesEntieres(d("2025-06-02"), d("2025-06-06")), "", List.of(), CALENDRIER, cinqMai);
            deChloe.refuser(ADMIN, CLOUD, "Période de soutenance", cinqMai);
            assertThat(deChloe.etat()).isEqualTo(EtatDemande.REFUSEE);
            assertThat(deChloe.motifRefus()).contains("Période de soutenance");
        }

        @Test
        void unConsultantNePeutPasDecider() {
            assertRegle(CodeRegle.CONGE_DECIDEUR_NON_HABILITE, () -> rttDuPont().valider(BRUNO, DIGITAL, CALENDRIER, cinqMai));
        }

        @Test
        void pasDeDecisionSurSaPropreDemande() {
            Collaborateur rachelDemandeuse = RACHEL;
            DemandeConge sienne = DemandeConge.deposer(rachelDemandeuse, TypeAbsence.RTT, PeriodeConge.journee(d("2025-06-02")),
                    "", List.of(), CALENDRIER, cinqMai);
            assertRegle(CodeRegle.CONGE_AUTO_DECISION, () -> sienne.valider(RACHEL, DIGITAL, CALENDRIER, cinqMai));
        }

        @Test
        void pasDeDoubleDecision() {
            DemandeConge demande = rttDuPont();
            demande.valider(RACHEL, DIGITAL, CALENDRIER, cinqMai);
            assertRegle(CodeRegle.CONGE_TRANSITION_INTERDITE, () -> demande.refuser(RACHEL, DIGITAL, "trop tard", cinqMai));
        }

        @Test
        void departementFourniIncoherent() {
            assertRegle(CodeRegle.CONGE_DECIDEUR_NON_HABILITE, () -> rttDuPont().valider(ADMIN, CLOUD, CALENDRIER, cinqMai));
        }
    }

    @Nested
    @DisplayName("Annulation (règle complétée, testée avec Clock)")
    class Annulation {

        @Test
        void demandeEnAttenteAnnuleeSansSaisieARetirer() {
            DemandeConge demande = rttDuPont();
            assertThat(demande.annuler(ALICE, CALENDRIER, horloge("2025-05-31T10:00"))).isEmpty();
            assertThat(demande.etat()).isEqualTo(EtatDemande.ANNULEE);
        }

        @Test
        @DisplayName("Congé validé annulé la veille : les saisies sont à retirer")
        void congeValideAnnuleLaVeille() {
            DemandeConge demande = rttDuPont();
            demande.valider(RACHEL, DIGITAL, CALENDRIER, cinqMai);
            List<Saisie> aRetirer = demande.annuler(ALICE, CALENDRIER, horloge("2025-05-29T18:00"));
            assertThat(aRetirer).extracting(Saisie::date).containsExactly(d("2025-05-30"));
        }

        @Test
        @DisplayName("Congé validé déjà commencé : annulation refusée")
        void congeCommence() {
            DemandeConge demande = rttDuPont();
            demande.valider(RACHEL, DIGITAL, CALENDRIER, cinqMai);
            assertRegle(CodeRegle.CONGE_ANNULATION_TARDIVE, () -> demande.annuler(ALICE, CALENDRIER, horloge("2025-05-30T08:00")));
        }

        @Test
        void seulLeDemandeurOuUnAdministrateur() {
            DemandeConge demande = rttDuPont();
            assertRegle(CodeRegle.CONGE_ANNULATION_NON_AUTORISEE, () -> demande.annuler(BRUNO, CALENDRIER, cinqMai));
            Collaborateur admin = Collaborateur.builder().matricule(1999).nom("Ad", "Min").entreLe(d("2010-01-01"))
                    .roles(Role.ADMINISTRATEUR).build();
            demande.annuler(admin, CALENDRIER, cinqMai);
            assertThat(demande.etat()).isEqualTo(EtatDemande.ANNULEE);
        }
    }
}
