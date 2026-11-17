package fr.conserto.consertime.domaine.affaires;

import java.util.Arrays;
import java.util.Optional;

/** Types d'absence portés par des projets du client CONSERTO. */
public enum TypeAbsence {
    CONGES_PAYES("CP", "Congés payés", true),
    RTT("RTT", "RTT", false),
    ABSENCE_EXCEPTIONNELLE("ABS", "Absence exceptionnelle", false);

    private final String codeProjet;
    private final String libelle;
    private final boolean imputeSurDroitsCongesPayes;

    TypeAbsence(String codeProjet, String libelle, boolean imputeSurDroitsCongesPayes) {
        this.codeProjet = codeProjet;
        this.libelle = libelle;
        this.imputeSurDroitsCongesPayes = imputeSurDroitsCongesPayes;
    }

    public String codeProjet() {
        return codeProjet;
    }

    public String libelle() {
        return libelle;
    }

    /** Vrai si les jours posés viennent en déduction des droits à congés payés. */
    public boolean imputeSurDroitsCongesPayes() {
        return imputeSurDroitsCongesPayes;
    }

    /** Projet d'absence correspondant, rattaché au client CONSERTO. */
    public Projet projet() {
        return Projet.absence(this);
    }

    public static Optional<TypeAbsence> depuisCodeProjet(String code) {
        return Arrays.stream(values()).filter(t -> t.codeProjet.equals(code)).findFirst();
    }
}
