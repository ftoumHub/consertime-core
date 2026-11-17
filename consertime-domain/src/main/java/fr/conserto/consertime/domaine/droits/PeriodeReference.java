package fr.conserto.consertime.domaine.droits;

import fr.conserto.consertime.domaine.calendrier.Periode;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Période d'acquisition des congés payés (du 1er juin N-1 au 31 mai N par défaut).
 * Les droits acquis se prennent pendant la {@link #periodeDePrise()} qui suit.
 */
public record PeriodeReference(Periode periode) {

    public static PeriodeReference contenant(LocalDate date, MonthDay debut) {
        LocalDate debutCetteAnnee = debut.atYear(date.getYear());
        LocalDate d = date.isBefore(debutCetteAnnee) ? debut.atYear(date.getYear() - 1) : debutCetteAnnee;
        return new PeriodeReference(new Periode(d, d.plusYears(1).minusDays(1)));
    }

    public LocalDate debut() {
        return periode.debut();
    }

    public LocalDate fin() {
        return periode.fin();
    }

    public PeriodeReference precedente() {
        LocalDate d = debut().minusYears(1);
        return new PeriodeReference(new Periode(d, debut().minusDays(1)));
    }

    public PeriodeReference suivante() {
        LocalDate d = fin().plusDays(1);
        return new PeriodeReference(new Periode(d, d.plusYears(1).minusDays(1)));
    }

    /** Période pendant laquelle se prennent les droits acquis sur cette période de référence. */
    public Periode periodeDePrise() {
        return suivante().periode();
    }

    public List<YearMonth> mois() {
        YearMonth premier = YearMonth.from(debut());
        YearMonth dernier = YearMonth.from(fin());
        return Stream.iterate(premier, m -> !m.isAfter(dernier), m -> m.plusMonths(1)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "période de référence " + periode;
    }
}
