package fr.conserto.consertime.domaine.personnel;

/** Matricule d'un collaborateur (colonne {@code USERS.MATRICULE} du legacy). */
public record Matricule(int valeur) {

    public Matricule {
        if (valeur <= 0) {
            throw new IllegalArgumentException("Matricule invalide : " + valeur);
        }
    }

    public static Matricule of(int valeur) {
        return new Matricule(valeur);
    }

    @Override
    public String toString() {
        return String.valueOf(valeur);
    }
}
