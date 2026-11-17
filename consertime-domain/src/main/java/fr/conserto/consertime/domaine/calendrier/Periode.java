package fr.conserto.consertime.domaine.calendrier;

import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Période de dates, bornes incluses.
 * <p>
 * Le legacy manipulait des {@code DateRange} dont la fin était tantôt incluse, tantôt exclue
 * ({@code DateUtil.createDateSequence} s'arrête avant la fin). Ici la fin est toujours incluse.
 */
public record Periode(LocalDate debut, LocalDate fin) {

    public Periode {
        Objects.requireNonNull(debut, "debut");
        Objects.requireNonNull(fin, "fin");
        RegleMetierViolee.verifier(!fin.isBefore(debut), CodeRegle.CAL_PERIODE_INVALIDE,
                "du " + debut + " au " + fin);
    }

    public static Periode du(LocalDate debut, LocalDate fin) {
        return new Periode(debut, fin);
    }

    public static Periode jour(LocalDate jour) {
        return new Periode(jour, jour);
    }

    public static Periode mois(YearMonth mois) {
        return new Periode(mois.atDay(1), mois.atEndOfMonth());
    }

    public Stream<LocalDate> jours() {
        return debut.datesUntil(fin.plusDays(1));
    }

    public long nombreDeJours() {
        return ChronoUnit.DAYS.between(debut, fin) + 1;
    }

    public boolean contient(LocalDate date) {
        return !date.isBefore(debut) && !date.isAfter(fin);
    }

    public boolean contient(Periode autre) {
        return contient(autre.debut) && contient(autre.fin);
    }

    public boolean chevauche(Periode autre) {
        return !debut.isAfter(autre.fin) && !autre.debut.isAfter(fin);
    }

    public Optional<Periode> intersection(Periode autre) {
        if (!chevauche(autre)) {
            return Optional.empty();
        }
        LocalDate d = debut.isAfter(autre.debut) ? debut : autre.debut;
        LocalDate f = fin.isBefore(autre.fin) ? fin : autre.fin;
        return Optional.of(new Periode(d, f));
    }

    @Override
    public String toString() {
        return debut.equals(fin) ? "le " + debut : "du " + debut + " au " + fin;
    }
}
