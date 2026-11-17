package fr.conserto.consertime.domaine.personnel;

import java.util.Objects;
import java.util.Set;

/**
 * Département (équipe) et ses référents administratifs (table {@code DEPT_TO_USERREFERENT}, script maj3).
 */
public record Departement(String code, String nom, Set<Matricule> referents) {

    public Departement {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(nom, "nom");
        referents = Set.copyOf(referents);
    }

    public boolean aPourReferent(Matricule matricule) {
        return referents.contains(matricule);
    }
}
