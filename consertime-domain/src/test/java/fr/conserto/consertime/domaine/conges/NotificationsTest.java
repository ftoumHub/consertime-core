package fr.conserto.consertime.domaine.conges;

import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static fr.conserto.consertime.domaine.Fixtures.ALICE;
import static fr.conserto.consertime.domaine.Fixtures.BRUNO;
import static fr.conserto.consertime.domaine.Fixtures.CALENDRIER;
import static fr.conserto.consertime.domaine.Fixtures.CHLOE;
import static fr.conserto.consertime.domaine.Fixtures.CLOUD;
import static fr.conserto.consertime.domaine.Fixtures.DIGITAL;
import static fr.conserto.consertime.domaine.Fixtures.RACHEL;
import static fr.conserto.consertime.domaine.Fixtures.d;
import static fr.conserto.consertime.domaine.Fixtures.horloge;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notifications et relances")
class NotificationsTest {

    private static final List<Collaborateur> EFFECTIF = List.of(RACHEL, ALICE, BRUNO, CHLOE);

    @Test
    @DisplayName("La demande part aux référents du département")
    void referentsDuDepartement() {
        DemandeConge demande = DemandeConge.deposer(ALICE, TypeAbsence.RTT, PeriodeConge.journee(d("2025-05-30")), "",
                List.of(), CALENDRIER, horloge("2025-05-05T14:00"));
        Notifications.Destinataires dest = Notifications.nouvelleDemande(demande, DIGITAL, EFFECTIF);

        assertThat(dest.destinataires()).containsExactly(RACHEL);
        assertThat(dest.configurationIncomplete()).isFalse();
    }

    @Test
    @DisplayName("Département sans référent : tous les référents sont prévenus d'un défaut de configuration")
    void departementSansReferent() {
        Collaborateur autreReferent = Collaborateur.builder()
                .matricule(1010)
                .nom("Rémi", "Ref")
                .entreLe(d("2019-01-07"))
                .departement("NA")
                .roles(Role.REFERENT).build();
        DemandeConge deChloe = DemandeConge.deposer(CHLOE, TypeAbsence.CONGES_PAYES,
                PeriodeConge.journeesEntieres(d("2025-06-02"), d("2025-06-06")), "", List.of(), CALENDRIER, horloge("2025-05-02T08:45"));
        Notifications.Destinataires dest = Notifications.nouvelleDemande(deChloe, CLOUD, List.of(RACHEL, ALICE, CHLOE, autreReferent));

        assertThat(dest.configurationIncomplete()).isTrue();
        assertThat(dest.destinataires()).containsExactly(autreReferent, RACHEL);
    }

    @Test
    @DisplayName("La seule référente du département demande : les autres référents sont prévenus, sans défaut de configuration")
    void laReferenteDemandePourElleMeme() {
        Collaborateur autreReferent = Collaborateur.builder().matricule(1010).nom("Rémi", "Ref").entreLe(d("2019-01-07"))
                .departement("NA").roles(Role.REFERENT).build();
        DemandeConge deRachel = DemandeConge.deposer(RACHEL, TypeAbsence.RTT, PeriodeConge.journee(d("2025-05-30")), "",
                List.of(), CALENDRIER, horloge("2025-05-05T14:00"));
        Notifications.Destinataires dest = Notifications.nouvelleDemande(deRachel, DIGITAL, List.of(RACHEL, ALICE, autreReferent));
        assertThat(dest.destinataires()).containsExactly(autreReferent);
        assertThat(dest.configurationIncomplete()).isFalse();
    }

    @Test
    void laReponseVaAuDemandeur() {
        DemandeConge demande = DemandeConge.deposer(BRUNO, TypeAbsence.RTT, PeriodeConge.journee(d("2025-05-30")), "",
                List.of(), CALENDRIER, horloge("2025-05-05T14:00"));
        assertThat(Notifications.reponse(demande)).isEqualTo(BRUNO);
    }

    @Test
    @DisplayName("Relance après 3 jours ouvrés sans réponse, week-end et Ascension exclus")
    void relance() {
        Clock depotMercredi = horloge("2025-05-28T10:00");
        DemandeConge demande = DemandeConge.deposer(ALICE, TypeAbsence.CONGES_PAYES,
                PeriodeConge.journeesEntieres(d("2025-07-07"), d("2025-07-11")), "", List.of(), CALENDRIER, depotMercredi);

        // jeudi 29 férié, vendredi 30 = 1, lundi 2 juin = 2, mardi 3 = 3
        assertThat(Notifications.aRelancer(List.of(demande), 3, CALENDRIER, horloge("2025-06-02T09:00"))).isEmpty();
        assertThat(Notifications.aRelancer(List.of(demande), 3, CALENDRIER, horloge("2025-06-03T09:00"))).containsExactly(demande);

        demande.valider(RACHEL, DIGITAL, CALENDRIER, horloge("2025-06-03T11:00"));
        assertThat(Notifications.aRelancer(List.of(demande), 3, CALENDRIER, horloge("2025-06-10T09:00"))).isEmpty();
    }
}
