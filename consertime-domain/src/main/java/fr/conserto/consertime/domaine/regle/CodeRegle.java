package fr.conserto.consertime.domaine.regle;

/**
 * Catalogue des règles de gestion vérifiées par le domaine.
 * <p>
 * Chaque code est repris dans {@code REGLES.md} avec son origine : règle extraite du legacy ConserTime
 * (fichier et ligne) ou règle complétée lors de l'extraction.
 */
public enum CodeRegle {

    CAL_PERIODE_INVALIDE("La date de fin d'une période ne peut pas précéder sa date de début"),

    SAISIE_HORS_MOIS("La saisie doit porter sur un jour du mois de la feuille de temps"),
    SAISIE_HORS_PRESENCE("Le collaborateur n'est pas présent dans l'entreprise à cette date"),
    SAISIE_HORS_AFFECTATION("L'affectation n'appartient pas au collaborateur ou n'est pas ouverte à cette date"),
    SAISIE_QUANTITE_INVALIDE("Une saisie vaut une demi-journée ou une journée (0,5 ou 1)"),
    SAISIE_JOUR_DEPASSE("Le total saisi sur une journée ne peut pas dépasser 1 jour"),

    CONGE_DATES_INVALIDES("La fin du congé précède son début"),
    CONGE_SANS_JOUR_OUVRE("La période demandée ne contient aucun jour ouvré"),
    CONGE_HORS_PRESENCE("La période demandée sort de la période de présence du collaborateur"),
    CONGE_PROJET_NON_ABSENCE("Un congé doit être imputé sur un projet d'absence"),
    CONGE_CHEVAUCHEMENT("Une demande en attente ou validée couvre déjà tout ou partie de cette période"),
    CONGE_TRANSITION_INTERDITE("Cette action n'est pas possible dans l'état actuel de la demande"),
    CONGE_DECIDEUR_NON_HABILITE("Seul un référent du département du demandeur ou un administrateur peut décider"),
    CONGE_AUTO_DECISION("Un collaborateur ne peut pas décider de sa propre demande"),
    CONGE_ANNULATION_NON_AUTORISEE("Seul le demandeur ou un administrateur peut annuler une demande"),
    CONGE_ANNULATION_TARDIVE("Un congé validé ne peut plus être annulé une fois commencé"),

    EXPORT_SAISIE_INCOMPLETE("Tous les jours ouvrés de la période doivent être saisis avant l'export"),

    NDF_LIGNE_INVALIDE("Ligne de frais invalide");

    private final String libelle;

    CodeRegle(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
