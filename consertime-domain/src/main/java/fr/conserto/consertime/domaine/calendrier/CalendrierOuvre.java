package fr.conserto.consertime.domaine.calendrier;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Jours ouvrés (lundi à vendredi hors fériés) et jours ouvrables (lundi à samedi hors fériés).
 * <p>
 * Legacy : {@code DateUtil.isWeekend}, {@code createDateSequenceJoursOuvres} (jours ouvrés).
 * Les jours ouvrables sont un complément, nécessaire au décompte légal des congés payés.
 */
public final class CalendrierOuvre {

    private final CalendrierJoursFeries feries;

    public CalendrierOuvre(CalendrierJoursFeries feries) {
        this.feries = Objects.requireNonNull(feries);
    }

    /** Calendrier du legacy : jours fériés de métropole. */
    public static CalendrierOuvre france() {
        return new CalendrierOuvre(CalendrierFrance.metropole());
    }

    public CalendrierJoursFeries feries() {
        return feries;
    }

    public static boolean estWeekEnd(LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        return jour == DayOfWeek.SATURDAY || jour == DayOfWeek.SUNDAY;
    }

    public boolean estFerie(LocalDate date) {
        return feries.estFerie(date);
    }

    public boolean estOuvre(LocalDate date) {
        return !estWeekEnd(date) && !estFerie(date);
    }

    public boolean estOuvrable(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SUNDAY && !estFerie(date);
    }

    public StatutJour statut(LocalDate date) {
        if (estFerie(date)) {
            return StatutJour.FERIE;
        }
        return estWeekEnd(date) ? StatutJour.WEEK_END : StatutJour.OUVRE;
    }

    public List<LocalDate> joursOuvres(Periode periode) {
        return periode.jours().filter(this::estOuvre).collect(Collectors.toList());
    }

    public long nombreJoursOuvres(Periode periode) {
        return periode.jours().filter(this::estOuvre).count();
    }

    public long nombreJoursOuvrables(Periode periode) {
        return periode.jours().filter(this::estOuvrable).count();
    }

    /** Numéro de semaine ISO 8601 (semaine commençant le lundi), utilisé en France. */
    public static int numeroSemaine(LocalDate date) {
        return date.get(WeekFields.ISO.weekOfWeekBasedYear());
    }
}
