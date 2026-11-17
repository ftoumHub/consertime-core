package fr.conserto.consertime.domaine.calendrier;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Source des jours fériés d'une année. Implémentation fournie : {@link CalendrierFrance}. */
public interface CalendrierJoursFeries {

    /** Jours fériés de l'année, triés par date. */
    List<JourFerie> joursFeries(int annee);

    default Optional<JourFerie> jourFerie(LocalDate date) {
        return joursFeries(date.getYear()).stream()
                .filter(f -> f.date().equals(date))
                .findFirst();
    }

    default boolean estFerie(LocalDate date) {
        return jourFerie(date).isPresent();
    }
}
