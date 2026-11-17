package fr.conserto.consertime.domaine.conges;

/**
 * États d'une demande de congé. Legacy : {@code DemandeCongeEtat} (EN_ATTENTE, VALIDE, REFUSE).
 * {@link #ANNULEE} est un complément. Dans le legacy, le référent pouvait seulement supprimer une demande
 * (colonne « nok » de {@code CongesAdminPage}), y compris un congé validé en cours ou passé, ses saisies
 * comprises ; le demandeur, lui, ne pouvait pas retirer sa demande.
 */
public enum EtatDemande {
    EN_ATTENTE("En attente"),
    VALIDEE("Validée"),
    REFUSEE("Refusée"),
    ANNULEE("Annulée");

    private final String libelle;

    EtatDemande(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /** Une demande active bloque toute autre demande sur la même période. */
    public boolean estActive() {
        return this == EN_ATTENTE || this == VALIDEE;
    }
}
