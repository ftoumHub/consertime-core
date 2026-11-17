package fr.conserto.consertime.domaine.documents;

import fr.conserto.consertime.domaine.affaires.Client;
import fr.conserto.consertime.domaine.calendrier.StatutJour;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;
import fr.conserto.consertime.domaine.temps.ControleCompletude;
import fr.conserto.consertime.domaine.temps.FeuilleDeTemps;
import fr.conserto.consertime.domaine.temps.Jours;
import fr.conserto.consertime.domaine.temps.Saisie;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Compte rendu d'activité (CRA) mensuel remis au client.
 * <p>
 * Règles reprises de {@code ExportCraHeader}, {@code ExportCraBody} et {@code ExportCraFooter} :
 * <ul>
 *   <li>une ligne par jour du mois ; les week-ends et jours fériés sont grisés ;</li>
 *   <li>seuls les projets facturables sont imputés ; les absences et projets internes n'apparaissent pas ;</li>
 *   <li>imputation d'un jour : « (1.0) - Projet », plusieurs projets séparés par « / » ;</li>
 *   <li>total : « Nombre de jours facturés », somme des imputations facturables ;</li>
 *   <li>signatures : consultant et responsable client.</li>
 * </ul>
 * Écart assumé : le client affiché est celui du premier projet facturable du mois. Le legacy prenait le premier
 * projet saisi, quel qu'il soit, et pouvait afficher CONSERTO si le mois commençait par une absence.
 */
public record CompteRenduActivite(
        String nom,
        String prenom,
        Optional<Client> client,
        YearMonth mois,
        LocalDate dateEdition,
        List<Ligne> lignes,
        Jours joursFactures) {

    public static final String TITRE = "COMPTE RENDU D'ACTIVITE";

    /** Imputation facturable d'une journée. */
    public record Imputation(Jours jours, String projet) {
        @Override
        public String toString() {
            return "(" + jours + ") - " + projet;
        }
    }

    public record Ligne(LocalDate date, StatutJour statut, List<Imputation> imputations) {

        public Ligne {
            imputations = List.copyOf(imputations);
        }

        public String libelleImputations() {
            return imputations.stream().map(Imputation::toString).collect(Collectors.joining(" / "));
        }

        public Jours total() {
            return imputations.stream().map(Imputation::jours).reduce(Jours.ZERO, Jours::plus);
        }
    }

    public CompteRenduActivite {
        lignes = List.copyOf(lignes);
    }

    /**
     * Établit le CRA d'une feuille de temps.
     *
     * @param exigerCompletude si vrai, refuse un mois incomplet (EXPORT_SAISIE_INCOMPLETE), comme le legacy
     */
    public static CompteRenduActivite etablir(FeuilleDeTemps feuille, boolean exigerCompletude, Clock clock) {
        if (exigerCompletude) {
            ControleCompletude.Resultat controle = ControleCompletude.verifier(feuille);
            RegleMetierViolee.verifier(controle.estComplet(), CodeRegle.EXPORT_SAISIE_INCOMPLETE,
                    String.join(" ", controle.messages()));
        }
        Collaborateur collaborateur = feuille.collaborateur();
        List<Saisie> facturables = feuille.saisies().stream().filter(s -> s.projet().facturable()).collect(Collectors.toList());

        List<Ligne> lignes = feuille.periode().jours()
                .map(jour -> new Ligne(jour, feuille.calendrier().statut(jour), facturables.stream()
                        .filter(s -> s.date().equals(jour))
                        .map(s -> new Imputation(s.jours(), s.projet().nom()))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());

        Jours total = facturables.stream().map(Saisie::jours).reduce(Jours.ZERO, Jours::plus);
        Optional<Client> client = facturables.stream().findFirst().map(s -> s.projet().client());
        return new CompteRenduActivite(collaborateur.nom(), collaborateur.prenom(), client, feuille.mois(),
                LocalDate.now(clock), lignes, total);
    }
}
