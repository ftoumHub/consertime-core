package fr.conserto.consertime.domaine.affaires;

import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.personnel.Matricule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Affectation d'un collaborateur à un projet (table {@code PROJECT_ASSIGNMENT}).
 *
 * @param validite période d'ouverture à la saisie ; {@code null} = sans limite
 * @param tauxJournalier taux facturé par jour ; {@code null} pour les projets non facturés
 */
public record Affectation(
        String id,
        Matricule matricule,
        Projet projet,
        Periode validite,
        Budget budget,
        BigDecimal tauxJournalier,
        boolean notifierChefDeProjet) {

    public Affectation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(matricule, "matricule");
        Objects.requireNonNull(projet, "projet");
        Objects.requireNonNull(budget, "budget");
    }

    /** Affectation sans budget ni limite de dates, typique des projets d'absence. */
    public static Affectation simple(Matricule matricule, Projet projet) {
        return new Affectation(projet.code() + "-" + matricule, matricule, projet, null, Budget.aucun(), null, false);
    }

    public Optional<Periode> periodeDeValidite() {
        return Optional.ofNullable(validite);
    }

    public boolean estOuverteLe(LocalDate date) {
        return validite == null || validite.contient(date);
    }

    /**
     * Statut du budget pour une consommation totale donnée (en jours).
     * Règles reprises de {@code ProjectAssignmentStatusServiceImpl.addFixedAssignmentStatus} et
     * {@code addFlexAssignmentStatus}.
     */
    public StatutBudget evaluer(BigDecimal consomme) {
        if (budget instanceof Budget.Fixe fixe) {
            int c = consomme.compareTo(fixe.jours());
            if (c < 0) {
                return StatutBudget.DANS_LE_BUDGET;
            }
            return c == 0 ? StatutBudget.BUDGET_ATTEINT : StatutBudget.BUDGET_DEPASSE;
        }
        if (budget instanceof Budget.Flexible flexible) {
            if (consomme.compareTo(flexible.jours()) < 0) {
                return StatutBudget.DANS_LE_BUDGET;
            }
            int c = consomme.compareTo(flexible.maximum());
            if (c < 0) {
                return StatutBudget.DEPASSEMENT_EN_COURS;
            }
            return c == 0 ? StatutBudget.DEPASSEMENT_MAXIMAL_ATTEINT : StatutBudget.BUDGET_DEPASSE;
        }
        return StatutBudget.DANS_LE_BUDGET;
    }

    /**
     * Le chef de projet est prévenu quand une saisie fait changer le statut du budget
     * et que l'affectation le demande ({@code TimesheetPersisterImpl.validateAndPersist}).
     */
    public boolean doitPrevenirChefDeProjet(StatutBudget avant, StatutBudget apres) {
        return notifierChefDeProjet && avant != apres && apres.estAcceptable();
    }
}
