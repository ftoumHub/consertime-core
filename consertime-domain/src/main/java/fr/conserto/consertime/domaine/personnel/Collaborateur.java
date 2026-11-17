package fr.conserto.consertime.domaine.personnel;

import fr.conserto.consertime.domaine.calendrier.Periode;

import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Collaborateur Conserto : identité, contrat, département, rôles et période de présence.
 * <p>
 * Legacy : entité {@code User} (matricule, {@code DATE_ENTREE}, {@code DATE_SORTIE}, type de contrat).
 */
public record Collaborateur(
        Matricule matricule,
        String nom,
        String prenom,
        String email,
        LocalDate dateEntree,
        LocalDate dateSortie,
        TypeContrat typeContrat,
        String codeDepartement,
        Set<Role> roles) {

    public Collaborateur {
        Objects.requireNonNull(matricule, "matricule");
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(prenom, "prenom");
        Objects.requireNonNull(dateEntree, "dateEntree");
        Objects.requireNonNull(typeContrat, "typeContrat");
        Objects.requireNonNull(codeDepartement, "codeDepartement");
        if (dateSortie != null && dateSortie.isBefore(dateEntree)) {
            throw new IllegalArgumentException("Date de sortie antérieure à la date d'entrée");
        }
        roles = roles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(roles));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<LocalDate> sortie() {
        return Optional.ofNullable(dateSortie);
    }

    public String nomComplet() {
        return prenom + " " + nom;
    }

    public boolean aLeRole(Role role) {
        return roles.contains(role);
    }

    public boolean estPresentLe(LocalDate date) {
        return !date.isBefore(dateEntree) && (dateSortie == null || !date.isAfter(dateSortie));
    }

    /** Partie de la période pendant laquelle le collaborateur fait partie des effectifs. */
    public Optional<Periode> presenceSur(Periode periode) {
        LocalDate fin = dateSortie == null ? periode.fin() : dateSortie;
        if (fin.isBefore(dateEntree)) {
            return Optional.empty();
        }
        return periode.intersection(new Periode(dateEntree, fin));
    }

    /** Ancienneté à une date donnée, à partir de la date d'entrée. */
    public Period anciennete(LocalDate aDate) {
        return aDate.isBefore(dateEntree) ? Period.ZERO : Period.between(dateEntree, aDate);
    }

    /** Construction pas à pas, pratique dans les tests et les scénarios. */
    public static final class Builder {
        private Matricule matricule;
        private String nom;
        private String prenom;
        private String email;
        private LocalDate dateEntree;
        private LocalDate dateSortie;
        private TypeContrat typeContrat = TypeContrat.ETAM;
        private String codeDepartement = "NA";
        private final Set<Role> roles = EnumSet.noneOf(Role.class);

        public Builder matricule(int valeur) {
            this.matricule = Matricule.of(valeur);
            return this;
        }

        public Builder nom(String prenom, String nom) {
            this.prenom = prenom;
            this.nom = nom;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder entreLe(LocalDate date) {
            this.dateEntree = date;
            return this;
        }

        public Builder sortLe(LocalDate date) {
            this.dateSortie = date;
            return this;
        }

        public Builder contrat(TypeContrat typeContrat) {
            this.typeContrat = typeContrat;
            return this;
        }

        public Builder departement(String code) {
            this.codeDepartement = code;
            return this;
        }

        public Builder roles(Role... roles) {
            this.roles.addAll(Set.of(roles));
            return this;
        }

        public Collaborateur build() {
            if (roles.isEmpty()) {
                roles.add(Role.CONSULTANT);
            }
            return new Collaborateur(matricule, nom, prenom, email, dateEntree, dateSortie,
                    typeContrat, codeDepartement, roles);
        }
    }
}
