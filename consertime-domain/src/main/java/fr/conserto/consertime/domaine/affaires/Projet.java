package fr.conserto.consertime.domaine.affaires;

import java.util.Objects;
import java.util.Optional;

/**
 * Projet sur lequel on impute du temps (table {@code PROJECT}).
 * Seuls les projets facturables apparaissent dans le CRA remis au client.
 */
public record Projet(String code, String nom, Client client, boolean facturable, NatureProjet nature) {

    public Projet {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(nature, "nature");
        if (nature == NatureProjet.ABSENCE && facturable) {
            throw new IllegalArgumentException("Un projet d'absence n'est pas facturable : " + code);
        }
    }

    public static Projet mission(String code, String nom, Client client) {
        return new Projet(code, nom, client, true, NatureProjet.MISSION);
    }

    public static Projet interne(String code, String nom) {
        return new Projet(code, nom, Client.CONSERTO, false, NatureProjet.INTERNE);
    }

    public static Projet absence(TypeAbsence type) {
        return new Projet(type.codeProjet(), type.libelle(), Client.CONSERTO, false, NatureProjet.ABSENCE);
    }

    public boolean estAbsence() {
        return nature == NatureProjet.ABSENCE;
    }

    public Optional<TypeAbsence> typeAbsence() {
        return estAbsence() ? TypeAbsence.depuisCodeProjet(code) : Optional.empty();
    }
}
