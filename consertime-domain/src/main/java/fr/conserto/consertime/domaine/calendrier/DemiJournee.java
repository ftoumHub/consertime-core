package fr.conserto.consertime.domaine.calendrier;

/**
 * Demi-journée d'une borne de congé.
 * <p>
 * Legacy : « Matinée » / « Après-midi » dans {@code DemandeCongeFormPanel}. Les bornes étaient stockées
 * comme des heures (début 8 h ou 14 h, fin 12 h ou 18 h) puis relues par
 * {@code DateUtil.congeCommenceAprem} et {@code congeTermineMatin}.
 */
public enum DemiJournee {
    MATIN("Matinée"),
    APRES_MIDI("Après-midi");

    private final String libelle;

    DemiJournee(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
