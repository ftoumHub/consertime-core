package fr.conserto.consertime.domaine.conges;

import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.Departement;
import fr.conserto.consertime.domaine.personnel.Matricule;
import fr.conserto.consertime.domaine.personnel.Role;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Qui prévenir, et quand relancer, autour d'une demande de congé. */
public final class Notifications {

    private Notifications() {
    }

    /**
     * @param configurationIncomplete vrai si le département n'a pas de référent : le legacy prévient alors
     *                                tous les référents que ConserTime est mal configuré
     */
    public record Destinataires(List<Collaborateur> destinataires, boolean configurationIncomplete) {
        public Destinataires {
            destinataires = List.copyOf(destinataires);
        }
    }

    /**
     * Destinataires du mail de nouvelle demande : les référents du département du demandeur ; si le département
     * n'a aucun référent, tous les collaborateurs ayant le rôle référent, avec un signalement de configuration
     * incomplète ({@code DemandeCongeFormPanel.sendMailForHolidays}).
     * <p>
     * Complément : le demandeur ne pouvant pas décider de sa propre demande, il est retiré des destinataires ;
     * s'il est le seul référent de son département, la demande part aux autres référents, sans signalement.
     */
    public static Destinataires nouvelleDemande(DemandeConge demande, Departement departement,
                                                Collection<Collaborateur> collaborateurs) {
        Matricule demandeur = demande.demandeur().matricule();
        List<Collaborateur> referents = collaborateurs.stream()
                .filter(c -> departement.aPourReferent(c.matricule()))
                .filter(c -> !c.matricule().equals(demandeur))
                .sorted(Comparator.comparing(Collaborateur::nom))
                .collect(Collectors.toList());
        if (!referents.isEmpty()) {
            return new Destinataires(referents, false);
        }
        List<Collaborateur> autresReferents = collaborateurs.stream()
                .filter(c -> c.aLeRole(Role.REFERENT))
                .filter(c -> !c.matricule().equals(demandeur))
                .sorted(Comparator.comparing(Collaborateur::nom))
                .collect(Collectors.toList());
        return new Destinataires(autresReferents, departement.referents().isEmpty());
    }

    /** La réponse (validation, refus) est envoyée au demandeur ({@code MailService.mailDemandeCongeResponse}). */
    public static Collaborateur reponse(DemandeConge demande) {
        return demande.demandeur();
    }

    /**
     * Complément : demandes restées en attente depuis au moins {@code delaiJoursOuvres} jours ouvrés,
     * à rappeler aux référents. Le délai court à partir du lendemain du dépôt.
     */
    public static List<DemandeConge> aRelancer(Collection<DemandeConge> demandes, int delaiJoursOuvres,
                                               CalendrierOuvre calendrier, Clock clock) {
        LocalDate aujourdhui = LocalDate.now(clock);
        return demandes.stream()
                .filter(d -> d.etat() == EtatDemande.EN_ATTENTE)
                .filter(d -> joursOuvresEcoules(d.dateDepot().toLocalDate(), aujourdhui, calendrier) >= delaiJoursOuvres)
                .sorted(Comparator.comparing(DemandeConge::dateDepot))
                .collect(Collectors.toList());
    }

    private static long joursOuvresEcoules(LocalDate depot, LocalDate aujourdhui, CalendrierOuvre calendrier) {
        if (!aujourdhui.isAfter(depot)) {
            return 0;
        }
        return depot.plusDays(1).datesUntil(aujourdhui.plusDays(1)).filter(calendrier::estOuvre).count();
    }
}
