package fr.conserto.consertime.domaine.affaires;

/**
 * État de consommation d'une affectation.
 * Legacy : {@code ProjectAssignmentStatus.Status} (IN_ALLOTTED, OVER_ALLOTTED, IN_OVERRUN, OVER_OVERRUN).
 */
public enum StatutBudget {
    DANS_LE_BUDGET(true),
    BUDGET_ATTEINT(true),
    DEPASSEMENT_EN_COURS(true),
    DEPASSEMENT_MAXIMAL_ATTEINT(true),
    BUDGET_DEPASSE(false);

    private final boolean acceptable;

    StatutBudget(boolean acceptable) {
        this.acceptable = acceptable;
    }

    /** Faux si la saisie qui mène à cet état doit être refusée. */
    public boolean estAcceptable() {
        return acceptable;
    }
}
