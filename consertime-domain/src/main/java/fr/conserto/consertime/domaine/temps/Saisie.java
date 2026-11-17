package fr.conserto.consertime.domaine.temps;

import fr.conserto.consertime.domaine.affaires.Projet;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/** Temps imputé sur un projet pour une journée (table {@code TIMESHEET_ENTRY}). */
public record Saisie(LocalDate date, Projet projet, Jours jours, String commentaire) {

    public Saisie {
        requireNonNull(date, "date");
        requireNonNull(projet, "projet");
        requireNonNull(jours, "jours");
    }

    public static Saisie de(LocalDate date, Projet projet, Jours jours) {
        return new Saisie(date, projet, jours, null);
    }

    public Optional<String> commentaireEventuel() {
        return Optional.ofNullable(commentaire).filter(c -> !c.isBlank());
    }
}
