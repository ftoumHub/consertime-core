package fr.conserto.consertime.domaine.calendrier;

/** Nature d'une journée du calendrier. Un jour férié tombant un week-end est {@link #FERIE}. */
public enum StatutJour {
    OUVRE,
    WEEK_END,
    FERIE;

    /** Jour grisé dans le CRA et la FDT (week-end ou férié), comme dans le legacy. */
    public boolean estNonTravaille() {
        return this != OUVRE;
    }
}
